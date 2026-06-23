package midplay.player;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.HttpConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import midplay.model.Track;
import midplay.net.MediaHttpClient;
import midplay.net.Network;
import midplay.store.Configuration;
import midplay.util.BufferedInputStream;
import midplay.util.HttpCodes;
import midplay.util.Lang;
import midplay.util.Utils;

public class MediaResolver implements MediaHttpClient.ResolveContext {
  private static final String[] MP3_CONTENT_TYPE_CANDIDATES = {
    "audio/mpeg", "audio/mp3", "audio/x-mp3", "audio/mpeg3"
  };
  private static final int MAX_INPUTSTREAM_CREATE_RETRIES = 3;
  private static final long INPUTSTREAM_CREATE_RETRY_DELAY_MS = 1200L;
  private static final long INPUTSTREAM_CREATE_RETRY_SLICE_MS = 200L;

  private static final String[] TRANSIENT_INPUTSTREAM_FAILURE_MARKERS = {
    "could not create player",
    "not found",
    "404",
    "processing",
    "media http error",
    "unexpected media redirect",
    "unexpected media content type"
  };

  private final PlayerGUI gui;
  private final MediaHttpClient httpClient = new MediaHttpClient();

  private String preferredInputStreamContentType;
  private boolean preferredInputStreamContentTypeResolved = false;

  public MediaResolver(PlayerGUI gui) {
    this.gui = gui;
  }

  public boolean isSessionActive(int sessionId) {
    return gui.isSessionActive(sessionId);
  }

  public void onResolveProgress(int progress, String status, int sessionId) {
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

  PendingPlayback createPendingPlayback(Track track, int sessionId, PlaybackMethod playerMethod)
      throws IOException, MediaException {
    String trackUrl = track.getUrl();
    if (trackUrl == null || trackUrl.length() == 0) {
      throw new IOException("No track URL");
    }

    PendingPlayback pending = new PendingPlayback();
    boolean keepPending = false;
    try {
      String finalUrl = httpClient.resolveRedirect(trackUrl, sessionId, this);

      if (!gui.isSessionActive(sessionId)) {
        keepPending = true;
        return pending;
      }

      if (playerMethod.isInputStream()) {
        createInputStreamPlaybackWithRetry(pending, trackUrl, finalUrl, sessionId);
      } else {
        pending.usedInputStream = false;
        pending.pendingResolvedUrl = finalUrl;
        pending.pendingContentLength = httpClient.getLastResolvedContentLength();
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
      pending.connectionWatchdog =
          Network.armPlaybackWatchdog(pending.pendingConnection, Network.PLAYBACK_SETUP_TIMEOUT_MS);
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
      vc.setLevel(gui.getVolumeLevel());
    }
    EqualizerEngine.applyFromSettings();
  }

  void closePendingPlayback(PendingPlayback pending) {
    if (pending == null) {
      return;
    }

    if (pending.connectionWatchdog != null) {
      pending.connectionWatchdog.cancel();
      pending.connectionWatchdog = null;
    }

    Utils.closePlayerQuietly(pending.pendingPlayer, gui);
    Utils.closeQuietly(pending.pendingInputStream);
    Utils.closeQuietly(pending.pendingConnection);

    pending.pendingPlayer = null;
    pending.pendingInputStream = null;
    pending.pendingConnection = null;
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
    pending.connectionWatchdog =
        Network.armPlaybackWatchdog(pending.pendingConnection, Network.PLAYBACK_SETUP_TIMEOUT_MS);
    if (!gui.isSessionActive(sessionId)) {
      return true;
    }

    int responseCode = pending.pendingConnection.getResponseCode();
    if (HttpCodes.isProcessing(responseCode)) {
      throw new IOException("Media still processing");
    }
    if (HttpCodes.isRedirect(responseCode)) {
      throw new IOException("Unexpected media redirect");
    }
    if (HttpCodes.isError(responseCode)) {
      throw new IOException("Media HTTP error: " + responseCode);
    }

    String responseContentType = pending.pendingConnection.getType();
    if (responseContentType != null
        && responseContentType.length() > 0
        && !httpClient.isLikelyAudioContentType(responseContentType)) {
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
      MediaHttpClient.interruptibleSleep(
          INPUTSTREAM_CREATE_RETRY_DELAY_MS, INPUTSTREAM_CREATE_RETRY_SLICE_MS, sessionId, this);
      if (!gui.isSessionActive(sessionId)) {
        return;
      }
      currentUrl = httpClient.resolveRedirect(originUrl, sessionId, this);
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
    return reason == null
        || Utils.containsAnyIgnoreCase(reason, TRANSIENT_INPUTSTREAM_FAILURE_MARKERS);
  }

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
