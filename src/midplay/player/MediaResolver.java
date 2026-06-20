package midplay.player;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.HttpConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import midplay.model.Track;
import midplay.net.Network;
import midplay.store.Configuration;
import midplay.util.BufferedInputStream;
import midplay.util.Lang;
import midplay.util.Utils;

// Turns a track URL into a prefetched, listener-attached Player. Owns redirect/
// 202-processing polling, input-stream vs pass-url method selection, transient-
// failure retry, and the content-type probe. Synchronizes on the PlayerGUI
// instance (passed as `gui`).
public class MediaResolver {
  private static final String[] MP3_CONTENT_TYPE_CANDIDATES = {
    "audio/mpeg", "audio/mp3", "audio/x-mp3", "audio/mpeg3"
  };
  private static final long PROCESSING_RETRY_INTERVAL_MS = 2000L;
  private static final long PROCESSING_RETRY_SLICE_MS = 200L;
  private static final int PROCESSING_RESPONSE_CODE = 202;
  // Hard ceiling on how long resolveRedirect keeps polling a 202 / "still
  // processing" response before giving up. New YTMusic tracks are transcoded on
  // first request; without a bound the poll loop can sit on "Loading" forever.
  private static final long MAX_PROCESSING_DURATION_MS = 90000L;
  // Consecutive transient resolution failures tolerated before failing fast:
  // 5xx under transcoding load, or a stale 200 that is not yet audio. Capped well
  // under MAX_PROCESSING_DURATION_MS so a persistently broken URL doesn't spin to
  // the full deadline.
  private static final int MAX_TRANSIENT_RESOLUTION_ERRORS = 6;
  private static final int MAX_MEDIA_REDIRECTS = 5;
  private static final int MAX_REDIRECT_NOT_FOUND_RECOVERY_ATTEMPTS = 3;
  private static final int MAX_INPUTSTREAM_CREATE_RETRIES = 3;
  private static final long INPUTSTREAM_CREATE_RETRY_DELAY_MS = 1200L;
  private static final long INPUTSTREAM_CREATE_RETRY_SLICE_MS = 200L;
  private static final int RESPONSE_BUFFER_SIZE = 512;
  // Cap on how much of a 202 "still processing" body we read. The status JSON
  // is tiny ({Status, Progress}); reading it unbounded re-allocates and re-reads
  // the full body on every poll (up to ~45 iterations over 90s).
  private static final int MAX_PROCESSING_BODY_BYTES = 1024;

  private final PlayerGUI gui;

  private String preferredInputStreamContentType;
  private boolean preferredInputStreamContentTypeResolved = false;

  public MediaResolver(PlayerGUI gui) {
    this.gui = gui;
  }

  // Holds a freshly created (and prefetched) Player plus its underlying connection
  // / stream, until PlayerGUI.attachPendingPlayback swaps them into the live slots.
  static final class PendingPlayback {
    Player pendingPlayer;
    HttpConnection pendingConnection;
    InputStream pendingInputStream;
    boolean usedInputStream;
  }

  private static final class MediaResolutionResult {
    final String resolvedUrl;
    final boolean processing;
    final int progress;
    final String status;
    final int responseCode;
    // True when the response was a transient failure (5xx, or a 200 whose body
    // is not yet audio) that resolveRedirect should keep polling rather than
    // treat as resolved. Defaults to false for every other result kind.
    boolean transientError;

    MediaResolutionResult(
        String resolvedUrl, boolean processing, int progress, String status, int responseCode) {
      this.resolvedUrl = resolvedUrl;
      this.processing = processing;
      this.progress = progress;
      this.status = status;
      this.responseCode = responseCode;
    }

    // A retryable resolution failure — the URL is not usable yet but may become
    // so once the backend finishes processing or recovers from a transient 5xx.
    static MediaResolutionResult transientError(String url, int responseCode) {
      MediaResolutionResult result = new MediaResolutionResult(url, false, -1, null, responseCode);
      result.transientError = true;
      return result;
    }
  }

  PendingPlayback createPendingPlayback(Track track, int sessionId, PlaybackMethod playerMethod)
      throws IOException, MediaException {
    String trackUrl = track.getUrl();
    if (trackUrl == null || trackUrl.length() == 0) {
      throw new IOException("No track URL");
    }

    PendingPlayback pending = new PendingPlayback();
    boolean keepPending = false;
    try {
      String finalUrl = resolveRedirect(trackUrl, sessionId);

      if (!gui.isSessionActive(sessionId)) {
        keepPending = true;
        return pending;
      }

      if (playerMethod.isInputStream()) {
        createInputStreamPlaybackWithRetry(pending, trackUrl, finalUrl, sessionId);
      } else {
        pending.usedInputStream = false;
        pending.pendingPlayer = Manager.createPlayer(finalUrl);
      }

      keepPending = true;
      return pending;
    } finally {
      if (!keepPending) {
        closePendingPlayback(pending);
      }
    }
  }

  // Build a Player for a seek: opens a Range GET at startByte and wraps it in an
  // InputStream player. InputStream-based players can't setMediaTime, so true
  // seeking reloads the media from the seek byte offset (HTTP Range). The new
  // player's media-time 0 == startByte == the requested seek position (the MP3
  // decoder resyncs to the next frame, so the cut is frame-accurate enough).
  PendingPlayback createSeekPlayback(String resolvedUrl, long startByte, int sessionId)
      throws IOException, MediaException {
    PendingPlayback pending = new PendingPlayback();
    boolean keepPending = false;
    try {
      String contentType = getPreferredInputStreamContentType();
      if (contentType == null || contentType.length() == 0) {
        throw new IOException("No content type for seek playback");
      }
      if (!gui.isSessionActive(sessionId)) {
        keepPending = true;
        return pending;
      }

      pending.usedInputStream = true;
      pending.pendingConnection = Network.openConnection(resolvedUrl);
      if (pending.pendingConnection == null) {
        throw new IOException("Failed to open seek connection");
      }
      pending.pendingConnection.setRequestMethod(HttpConnection.GET);
      pending.pendingConnection.setRequestProperty("Range", "bytes=" + startByte + "-");
      int responseCode = pending.pendingConnection.getResponseCode();
      if (responseCode != HttpConnection.HTTP_PARTIAL && responseCode != HttpConnection.HTTP_OK) {
        throw new IOException("Seek HTTP error: " + responseCode);
      }
      if (!gui.isSessionActive(sessionId)) {
        keepPending = true;
        return pending;
      }

      InputStream raw = pending.pendingConnection.openInputStream();
      // Server ignored Range (200, full body): skip to the seek byte manually so
      // the player still starts at the requested position.
      if (responseCode == HttpConnection.HTTP_OK) {
        long remaining = startByte;
        while (remaining > 0) {
          long skipped = raw.skip(remaining);
          if (skipped <= 0) {
            break;
          }
          remaining -= skipped;
        }
      }
      pending.pendingInputStream = new BufferedInputStream(raw);
      pending.pendingPlayer = Manager.createPlayer(pending.pendingInputStream, contentType);
      keepPending = true;
      return pending;
    } finally {
      if (!keepPending) {
        closePendingPlayback(pending);
      }
    }
  }

  private boolean createInputStreamPlayback(PendingPlayback pending, String finalUrl, int sessionId)
      throws IOException, MediaException {
    String contentType = getPreferredInputStreamContentType();
    if (contentType == null || contentType.length() == 0) {
      return false;
    }

    pending.usedInputStream = true;
    pending.pendingConnection = Network.openConnection(finalUrl);
    if (pending.pendingConnection == null) {
      throw new IOException("Failed to open media connection");
    }
    if (!gui.isSessionActive(sessionId)) {
      return true;
    }

    int responseCode = pending.pendingConnection.getResponseCode();
    if (responseCode == PROCESSING_RESPONSE_CODE) {
      throw new IOException("Media still processing");
    }
    if (responseCode == HttpConnection.HTTP_MOVED_PERM
        || responseCode == HttpConnection.HTTP_MOVED_TEMP) {
      throw new IOException("Unexpected media redirect");
    }
    if (responseCode >= HttpConnection.HTTP_BAD_REQUEST) {
      throw new IOException("Media HTTP error: " + responseCode);
    }

    String responseContentType = pending.pendingConnection.getType();
    if (responseContentType != null
        && responseContentType.length() > 0
        && !isLikelyAudioContentType(responseContentType)) {
      throw new IOException("Unexpected media content type: " + responseContentType);
    }

    pending.pendingInputStream =
        new BufferedInputStream(pending.pendingConnection.openInputStream());
    if (!gui.isSessionActive(sessionId)) {
      return true;
    }

    pending.pendingPlayer = Manager.createPlayer(pending.pendingInputStream, contentType);
    return true;
  }

  private void createInputStreamPlaybackWithRetry(
      PendingPlayback pending, String originUrl, String resolvedUrl, int sessionId)
      throws IOException, MediaException {
    String currentUrl = resolvedUrl;
    int retryCount = 0;

    while (true) {
      if (!gui.isSessionActive(sessionId)) {
        return;
      }

      try {
        if (!createInputStreamPlayback(pending, currentUrl, sessionId)) {
          pending.usedInputStream = false;
          pending.pendingPlayer = Manager.createPlayer(currentUrl);
        }
        return;
      } catch (IOException e) {
        if (!shouldRetryInputStreamCreateFailure(e, retryCount, sessionId)) {
          throw e;
        }
      } catch (MediaException e) {
        if (!shouldRetryInputStreamCreateFailure(e, retryCount, sessionId)) {
          throw e;
        }
      }

      closePendingPlayback(pending);
      retryCount++;
      interruptibleSleep(
          INPUTSTREAM_CREATE_RETRY_DELAY_MS, INPUTSTREAM_CREATE_RETRY_SLICE_MS, sessionId);
      if (!gui.isSessionActive(sessionId)) {
        return;
      }
      currentUrl = resolveRedirect(originUrl, sessionId);
    }
  }

  private boolean shouldRetryInputStreamCreateFailure(
      Exception error, int retryCount, int sessionId) {
    return error != null
        && retryCount < MAX_INPUTSTREAM_CREATE_RETRIES
        && gui.isSessionActive(sessionId)
        && isLikelyTransientInputStreamFailure(error.toString());
  }

  private boolean isLikelyTransientInputStreamFailure(String reason) {
    if (reason == null) {
      return true;
    }

    String normalized = reason.toLowerCase();
    return normalized.indexOf("could not create player") != -1
        || normalized.indexOf("not found") != -1
        || normalized.indexOf("404") != -1
        || normalized.indexOf("processing") != -1
        || normalized.indexOf("media http error") != -1
        || normalized.indexOf("unexpected media redirect") != -1
        || normalized.indexOf("unexpected media content type") != -1;
  }

  // Slices a blocking sleep into short checks so a session cancel (track change /
  // cleanup) is noticed promptly. Shared by the inputstream-create retry wait and
  // the 202-processing retry wait.
  private void interruptibleSleep(long totalMs, long sliceMs, int sessionId) {
    long remaining = totalMs;
    while (remaining > 0L && gui.isSessionActive(sessionId)) {
      long sleepTime = remaining > sliceMs ? sliceMs : remaining;
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      }
      remaining -= sleepTime;
    }
  }

  private boolean isLikelyAudioContentType(String contentType) {
    if (contentType == null) {
      return true;
    }

    String normalized = contentType.toLowerCase();
    return normalized.indexOf("audio/") != -1
        || normalized.indexOf("application/octet-stream") != -1
        || normalized.indexOf("application/vnd.apple.mpegurl") != -1
        || normalized.indexOf("application/x-mpegurl") != -1;
  }

  void preparePendingPlayer(Player pendingPlayer, int sessionId) throws MediaException {
    if (pendingPlayer == null || !gui.isSessionActive(sessionId)) {
      return;
    }

    pendingPlayer.addPlayerListener(gui);
    pendingPlayer.realize();

    if (!gui.isSessionActive(sessionId)) {
      return;
    }

    pendingPlayer.prefetch();

    if (!gui.isSessionActive(sessionId)) {
      return;
    }

    VolumeControl vc = gui.getVolumeControl(pendingPlayer);
    if (vc != null) {
      vc.setLevel(gui.volumeLevel);
    }
  }

  void closePendingPlayback(PendingPlayback pending) {
    if (pending == null) {
      return;
    }

    gui.closePlayerQuietly(pending.pendingPlayer);
    gui.closeHttpResources(pending.pendingInputStream, pending.pendingConnection);

    pending.pendingPlayer = null;
    pending.pendingInputStream = null;
    pending.pendingConnection = null;
  }

  private String resolveRedirect(String url, int sessionId) throws IOException {
    if (url == null || url.length() == 0) {
      throw new IOException("Invalid media URL");
    }

    String originUrl = url;
    String currentUrl = url;
    int redirectCount = 0;
    int notFoundRecoveryAttempts = 0;
    int consecutiveTransientErrors = 0;
    int lastTransientResponseCode = -1;
    final long resolveStartMs = System.currentTimeMillis();
    while (gui.isSessionActive(sessionId)) {
      // Bound the whole resolution so a stuck/"still processing" backend can't
      // hold the UI on "Loading" indefinitely.
      if (System.currentTimeMillis() - resolveStartMs > MAX_PROCESSING_DURATION_MS) {
        throw new IOException("Media still processing (timeout)");
      }

      MediaResolutionResult result = resolveMediaUrlOnce(currentUrl, sessionId);
      if (shouldRetryResolutionFromOrigin(originUrl, currentUrl, result)) {
        if (notFoundRecoveryAttempts >= MAX_REDIRECT_NOT_FOUND_RECOVERY_ATTEMPTS) {
          throw new IOException(
              "Media URL returned HTTP 404 after redirect recovery attempts: " + currentUrl);
        }
        notFoundRecoveryAttempts++;
        currentUrl = originUrl;
        redirectCount = 0;
        updateProcessingStatus(-1, null, sessionId);
        interruptibleSleep(PROCESSING_RETRY_INTERVAL_MS, PROCESSING_RETRY_SLICE_MS, sessionId);
        continue;
      }

      // Transient backend failure (5xx under transcoding load, or a 200 whose
      // body is not yet audio): keep polling rather than handing a broken URL to
      // Manager.createPlayer. Fail fast after a run of consecutive failures so a
      // persistently broken URL surfaces a clear error instead of spinning.
      if (result.transientError) {
        consecutiveTransientErrors++;
        lastTransientResponseCode = result.responseCode;
        if (consecutiveTransientErrors >= MAX_TRANSIENT_RESOLUTION_ERRORS) {
          throw new IOException(
              "Media URL resolution failed (HTTP " + lastTransientResponseCode + ")");
        }
        updateProcessingStatus(-1, null, sessionId);
        interruptibleSleep(PROCESSING_RETRY_INTERVAL_MS, PROCESSING_RETRY_SLICE_MS, sessionId);
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

      updateProcessingStatus(result.progress, result.status, sessionId);
      interruptibleSleep(PROCESSING_RETRY_INTERVAL_MS, PROCESSING_RETRY_SLICE_MS, sessionId);
    }

    return currentUrl;
  }

  private MediaResolutionResult resolveMediaUrlOnce(String url, int sessionId) throws IOException {
    HttpConnection connection = null;
    InputStream responseStream = null;
    try {
      if (!gui.isSessionActive(sessionId)) {
        return new MediaResolutionResult(url, false, -1, null, -1);
      }

      connection = Network.openConnection(url);
      if (connection == null) {
        throw new IOException("Failed to resolve media redirect");
      }

      int responseCode = connection.getResponseCode();
      if (responseCode == HttpConnection.HTTP_MOVED_PERM
          || responseCode == HttpConnection.HTTP_MOVED_TEMP) {
        String redirectUrl = connection.getHeaderField("Location");
        if (redirectUrl != null && redirectUrl.length() > 0) {
          return new MediaResolutionResult(redirectUrl, false, -1, null, responseCode);
        }
      }

      if (responseCode == PROCESSING_RESPONSE_CODE) {
        responseStream = connection.openInputStream();
        return parseProcessingResponse(url, readResponseBody(responseStream), responseCode);
      }

      if (responseCode == HttpConnection.HTTP_OK) {
        // Reported done — but only trust a 200 if the response actually looks
        // like audio. A stale/early 200 whose body is still JSON/HTML would be
        // handed straight to Manager.createPlayer and surface as
        // "Could not create player"; poll once more instead.
        String contentType = connection.getType();
        if (contentType != null
            && contentType.length() > 0
            && !isLikelyAudioContentType(contentType)) {
          return MediaResolutionResult.transientError(url, responseCode);
        }
        return new MediaResolutionResult(url, false, -1, null, responseCode);
      }

      // 5xx while resolving: the backend routinely returns transient 5xx under
      // transcoding load. Treat it as retryable instead of returning the URL as
      // "resolved". 4xx keeps the existing path (returned here, then surfaced as
      // "Media HTTP error" by the inputstream layer, which already retries);
      // 404-after-redirect has its own recovery above.
      if (responseCode >= HttpConnection.HTTP_INTERNAL_ERROR) {
        return MediaResolutionResult.transientError(url, responseCode);
      }

      return new MediaResolutionResult(url, false, -1, null, responseCode);
    } finally {
      gui.closeHttpResources(responseStream, connection);
    }
  }

  private MediaResolutionResult parseProcessingResponse(
      String url, String responseBody, int responseCode) {
    String status = null;
    int progress = -1;

    if (responseBody != null && responseBody.trim().length() > 0) {
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
        && result.responseCode == HttpConnection.HTTP_NOT_FOUND
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
    byte[] bytes = output.toByteArray();
    try {
      return new String(bytes, "UTF-8");
    } catch (Exception e) {
      return new String(bytes);
    }
  }

  private void updateProcessingStatus(int progress, String status, int sessionId) {
    if (!gui.isSessionActive(sessionId)) {
      return;
    }

    String loadingText = Lang.tr(Configuration.PLAYER_STATUS_LOADING);
    StringBuffer statusBuffer = new StringBuffer(loadingText);
    if (progress >= 0) {
      statusBuffer.append(' ').append(progress).append('%');
    } else if (status != null && status.length() > 0) {
      statusBuffer.append(' ').append(status);
    }
    gui.setStatus(statusBuffer.toString());
  }

  // synchronized(gui) preserves the original PlayerGUI monitor so callers that
  // already hold the lock (recovery) re-enter it and one-time probes serialize.
  String getPreferredInputStreamContentType() {
    synchronized (gui) {
      if (preferredInputStreamContentTypeResolved) {
        return preferredInputStreamContentType;
      }

      preferredInputStreamContentTypeResolved = true;
      String[] supportedTypes = null;
      try {
        supportedTypes = Manager.getSupportedContentTypes(null);
      } catch (Exception e) {
      }

      preferredInputStreamContentType = findSupportedContentType(supportedTypes);
      if (preferredInputStreamContentType == null && !Utils.isSamsung) {
        preferredInputStreamContentType = MP3_CONTENT_TYPE_CANDIDATES[0];
      }

      return preferredInputStreamContentType;
    }
  }

  private String findSupportedContentType(String[] supportedTypes) {
    if (supportedTypes == null || supportedTypes.length == 0) {
      return null;
    }

    for (int i = 0; i < MP3_CONTENT_TYPE_CANDIDATES.length; i++) {
      String candidate = MP3_CONTENT_TYPE_CANDIDATES[i];
      for (int j = 0; j < supportedTypes.length; j++) {
        String supportedType = supportedTypes[j];
        if (supportedType != null && candidate.equalsIgnoreCase(supportedType)) {
          return supportedType;
        }
      }
    }

    return null;
  }
}
