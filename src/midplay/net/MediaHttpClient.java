package midplay.net;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.HttpConnection;
import midplay.util.HttpCodes;
import midplay.util.Utils;

public final class MediaHttpClient {
  private static final String[] AUDIO_CONTENT_TYPE_MARKERS = {
    "audio/", "application/octet-stream", "application/vnd.apple.mpegurl", "application/x-mpegurl"
  };
  private static final long PROCESSING_RETRY_INTERVAL_MS = 2000L;
  private static final long PROCESSING_RETRY_SLICE_MS = 200L;
  private static final long MAX_PROCESSING_DURATION_MS = 90000L;
  private static final int MAX_TRANSIENT_RESOLUTION_ERRORS = 6;
  private static final int MAX_MEDIA_REDIRECTS = 5;
  private static final int MAX_REDIRECT_NOT_FOUND_RECOVERY_ATTEMPTS = 3;
  private static final int RESPONSE_BUFFER_SIZE = 512;
  private static final int MAX_PROCESSING_BODY_BYTES = 1024;

  private long lastResolvedContentLength = -1L;

  public interface ResolveContext {
    boolean isSessionActive(int sessionId);

    void onResolveProgress(int progress, String status, int sessionId);
  }

  public long getLastResolvedContentLength() {
    return lastResolvedContentLength;
  }

  public String resolveRedirect(String url, int sessionId, ResolveContext ctx) throws IOException {
    if (url == null || url.length() == 0) {
      throw new IOException("Invalid media URL");
    }

    String originUrl = url;
    String currentUrl = url;
    lastResolvedContentLength = -1L;
    int redirectCount = 0;
    int notFoundRecoveryAttempts = 0;
    int consecutiveTransientErrors = 0;
    int lastTransientResponseCode = -1;
    final long resolveStartMs = System.currentTimeMillis();
    while (ctx.isSessionActive(sessionId)) {
      if (System.currentTimeMillis() - resolveStartMs > MAX_PROCESSING_DURATION_MS) {
        throw new IOException("Media still processing (timeout)");
      }

      MediaResolutionResult result = resolveMediaUrlOnce(currentUrl, sessionId, ctx);
      if (shouldRetryResolutionFromOrigin(originUrl, currentUrl, result)) {
        if (notFoundRecoveryAttempts >= MAX_REDIRECT_NOT_FOUND_RECOVERY_ATTEMPTS) {
          throw new IOException(
              "Media URL returned HTTP 404 after redirect recovery attempts: " + currentUrl);
        }
        notFoundRecoveryAttempts++;
        currentUrl = originUrl;
        redirectCount = 0;
        ctx.onResolveProgress(-1, null, sessionId);
        interruptibleSleep(PROCESSING_RETRY_INTERVAL_MS, PROCESSING_RETRY_SLICE_MS, sessionId, ctx);
        continue;
      }

      if (result.transientError) {
        consecutiveTransientErrors++;
        lastTransientResponseCode = result.responseCode;
        if (consecutiveTransientErrors >= MAX_TRANSIENT_RESOLUTION_ERRORS) {
          throw new IOException(
              "Media URL resolution failed (HTTP " + lastTransientResponseCode + ")");
        }
        ctx.onResolveProgress(-1, null, sessionId);
        interruptibleSleep(PROCESSING_RETRY_INTERVAL_MS, PROCESSING_RETRY_SLICE_MS, sessionId, ctx);
        continue;
      }
      consecutiveTransientErrors = 0;

      if (!result.processing
          && result.resolvedUrl != null
          && !result.resolvedUrl.equals(currentUrl)
          && redirectCount < MAX_MEDIA_REDIRECTS) {
        currentUrl = result.resolvedUrl;
        redirectCount++;
        continue;
      }

      if (!result.processing) {
        return result.resolvedUrl;
      }

      ctx.onResolveProgress(result.progress, result.status, sessionId);
      interruptibleSleep(PROCESSING_RETRY_INTERVAL_MS, PROCESSING_RETRY_SLICE_MS, sessionId, ctx);
    }

    return currentUrl;
  }

  private MediaResolutionResult resolveMediaUrlOnce(String url, int sessionId, ResolveContext ctx)
      throws IOException {
    HttpConnection connection = null;
    InputStream responseStream = null;
    try {
      if (!ctx.isSessionActive(sessionId)) {
        return new MediaResolutionResult(url, false, -1, null, -1);
      }

      connection = Network.openConnection(url);
      if (connection == null) {
        throw new IOException("Failed to resolve media redirect");
      }

      int responseCode = connection.getResponseCode();
      if (HttpCodes.isRedirect(responseCode)) {
        String redirectUrl = connection.getHeaderField("Location");
        if (redirectUrl != null && redirectUrl.length() > 0) {
          return new MediaResolutionResult(redirectUrl, false, -1, null, responseCode);
        }
      }

      if (HttpCodes.isProcessing(responseCode)) {
        responseStream = connection.openInputStream();
        return parseProcessingResponse(url, readResponseBody(responseStream), responseCode);
      }

      if (responseCode == HttpConnection.HTTP_OK) {
        String contentType = connection.getType();
        if (contentType != null
            && contentType.length() > 0
            && !isLikelyAudioContentType(contentType)) {
          return MediaResolutionResult.transientError(url, responseCode);
        }
        try {
          lastResolvedContentLength = connection.getLength();
        } catch (Exception e) {
        }
        return new MediaResolutionResult(url, false, -1, null, responseCode);
      }

      if (HttpCodes.isServerError(responseCode)) {
        return MediaResolutionResult.transientError(url, responseCode);
      }

      return new MediaResolutionResult(url, false, -1, null, responseCode);
    } finally {
      Utils.closeQuietly(responseStream);
      Utils.closeQuietly(connection);
    }
  }

  private MediaResolutionResult parseProcessingResponse(
      String url, String responseBody, int responseCode) {
    String status = null;
    int progress = -1;

    if (hasNonWhitespace(responseBody)) {
      try {
        JSONObject json = JSON.getObject(responseBody);
        if (json != null) {
          status = json.getString("Status", json.getString("status", ""));
          if (status != null && status.length() == 0) {
            status = null;
          }
          if (json.has("Progress")) {
            progress = json.getInt("Progress", -1);
          } else if (json.has("progress")) {
            progress = json.getInt("progress", -1);
          }
        }
      } catch (Exception e) {
      }
    }

    if (progress < 0) {
      progress = -1;
    } else if (progress > 100) {
      progress = 100;
    }

    return new MediaResolutionResult(url, true, progress, status, responseCode);
  }

  private boolean shouldRetryResolutionFromOrigin(
      String originUrl, String currentUrl, MediaResolutionResult result) {
    return result != null
        && HttpCodes.isNotFound(result.responseCode)
        && originUrl != null
        && currentUrl != null
        && !originUrl.equals(currentUrl);
  }

  private String readResponseBody(InputStream stream) throws IOException {
    if (stream == null) {
      return "";
    }

    ByteArrayOutputStream output = new ByteArrayOutputStream(256);
    byte[] buffer = new byte[RESPONSE_BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = stream.read(buffer)) != -1) {
      output.write(buffer, 0, bytesRead);
      if (output.size() >= MAX_PROCESSING_BODY_BYTES) {
        break;
      }
    }
    return Utils.bytesToUtf8(output.toByteArray());
  }

  private static boolean hasNonWhitespace(String s) {
    if (s == null) {
      return false;
    }
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
        return true;
      }
    }
    return false;
  }

  public static void interruptibleSleep(
      long totalMs, long sliceMs, int sessionId, ResolveContext ctx) {
    long remaining = totalMs;
    while (remaining > 0L && ctx.isSessionActive(sessionId)) {
      long sleepTime = remaining > sliceMs ? sliceMs : remaining;
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      }
      remaining -= sleepTime;
    }
  }

  public boolean isLikelyAudioContentType(String contentType) {
    return contentType == null
        || Utils.containsAnyIgnoreCase(contentType, AUDIO_CONTENT_TYPE_MARKERS);
  }

  public static long totalContentLength(HttpConnection connection) {
    if (connection == null) {
      return -1L;
    }
    try {
      String contentRange = connection.getHeaderField("Content-Range");
      if (contentRange != null) {
        int slash = contentRange.lastIndexOf('/');
        if (slash >= 0 && slash < contentRange.length() - 1) {
          String total = contentRange.substring(slash + 1).trim();
          if (total.length() > 0) {
            return Long.parseLong(total);
          }
        }
      }
    } catch (Exception e) {
    }
    try {
      return connection.getLength();
    } catch (Exception e) {
      return -1L;
    }
  }
}
