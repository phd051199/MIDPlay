import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.io.HttpConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.rms.RecordStoreException;
import model.Track;
import model.Tracks;

public class PlayerGUI implements PlayerListener {
  private static final int TIMER_INTERVAL = 1000;
  private static final int VOLUME_STEP = 10;
  private static final int MAX_PENDING_TRACK_STEPS = 20;
  private static final long MEDIA_SAMPLE_WINDOW_MS = 250L;
  private static final String[] MP3_CONTENT_TYPE_CANDIDATES = {
    "audio/mpeg", "audio/mp3", "audio/x-mp3", "audio/mpeg3"
  };
  private static final long PROCESSING_RETRY_INTERVAL_MS = 2000L;
  private static final long PROCESSING_RETRY_SLICE_MS = 200L;
  private static final int PROCESSING_RESPONSE_CODE = 202;
  private static final int MAX_MEDIA_REDIRECTS = 5;
  private static final int RESPONSE_BUFFER_SIZE = 1024;
  private static final int COMMAND_TRACK_END = 1;
  private static final int COMMAND_APPLY_PENDING = 2;

  private final PlayerScreen parent;
  private final SettingsManager settingsManager;
  private Player player;
  private Tracks trackList;
  private int currentTrackIndex;
  private int volumeLevel = Configuration.PLAYER_MAX_VOLUME;
  private int repeatMode = Configuration.PLAYER_REPEAT_ALL;
  private boolean isShuffleEnabled = false;
  private int[] shuffleOrder;
  private int shufflePosition;
  private final Random random = new Random();
  private HttpConnection httpConnection;
  private InputStream inputStream;
  private Timer mainTimer;
  private TimerTask displayTask;

  private volatile boolean loading = false;
  private boolean destroyed = false;
  private int playbackSessionId = 0;
  private boolean handlingTrackEnd = false;
  private int pendingTrackDelta = 0;
  private final Vector commandQueue = new Vector();
  private Thread commandThread;
  private boolean commandWorkerStopRequested = false;
  private String pendingPlayerMethodOverride;
  private boolean sessionUsedInputStream = false;
  private boolean sessionStarted = false;
  private int sessionMethodSessionId = -1;
  private int startWatchdogSessionId = -1;
  private int lastFallbackSessionId = -1;
  private String preferredInputStreamContentType;
  private boolean preferredInputStreamContentTypeResolved = false;

  private long cachedMediaTimeMicros = 0L;
  private long cachedDurationMicros = 0L;
  private long cachedSampleTimeMs = 0L;
  private int cachedSessionId = -1;

  private static final class PendingPlayback {
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

    MediaResolutionResult(String resolvedUrl, boolean processing, int progress, String status) {
      this.resolvedUrl = resolvedUrl;
      this.processing = processing;
      this.progress = progress;
      this.status = status;
    }
  }

  private static final class CommandTask {
    final int type;
    final int sessionId;

    CommandTask(int type, int sessionId) {
      this.type = type;
      this.sessionId = sessionId;
    }
  }

  public PlayerGUI(PlayerScreen parent) {
    this.parent = parent;
    this.settingsManager = SettingsManager.getInstance();
    loadSettings();
    startCommandWorker();
    setStatus("");
  }

  private void loadSettings() {
    try {
      this.volumeLevel = settingsManager.getCurrentVolumeLevel();
      this.repeatMode = settingsManager.getCurrentRepeatMode();
      this.isShuffleEnabled =
          Configuration.PLAYER_SHUFFLE_ON == settingsManager.getCurrentShuffleMode();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized boolean isLoading() {
    return loading;
  }

  public boolean setPlaylist(Tracks tracks, int startIndex) {
    Track[] playlistTracks = tracks != null ? tracks.getTracks() : null;
    boolean hasTracks = playlistTracks != null && playlistTracks.length > 0;
    int normalizedIndex = -1;
    if (hasTracks) {
      normalizedIndex = startIndex;
      if (normalizedIndex < 0) {
        normalizedIndex = 0;
      }
      if (normalizedIndex >= playlistTracks.length) {
        normalizedIndex = playlistTracks.length - 1;
      }
    }

    synchronized (this) {
      startCommandWorker();
      resetPlaybackStateLocked();
      clearPendingTrackChangesLocked();
      clearCommandQueueLocked();
      clearPendingPlayerMethodOverrideLocked();
      this.trackList = tracks;
      this.currentTrackIndex = normalizedIndex;
      if (hasTracks && isShuffleEnabled) {
        createShuffleLocked();
      } else {
        shuffleOrder = null;
        shufflePosition = 0;
      }
      destroyed = false;
    }

    resetDisplay();
    setStatusByKey(
        hasTracks ? Configuration.PLAYER_STATUS_READY : Configuration.PLAYER_STATUS_STOPPED);
    parent.updateDisplay();
    return hasTracks;
  }

  public void play() {
    Track currentTrack = getCurrentTrack();
    if (currentTrack == null) {
      return;
    }

    Player currentPlayer;
    synchronized (this) {
      destroyed = false;
      currentPlayer = player;
      if (loading) {
        return;
      }
    }

    if (isPlayerReady(currentPlayer)) {
      startPlayback();
      return;
    }

    startLoadThread();
  }

  public void pause() {
    Player currentPlayer;
    synchronized (this) {
      if (loading) {
        return;
      }
      currentPlayer = player;
    }
    if (currentPlayer != null) {
      try {
        currentPlayer.stop();
      } catch (MediaException e) {
      }
    }
    setStatusByKey(Configuration.PLAYER_STATUS_PAUSED);
    stopTimer();
  }

  public void stop() {
    synchronized (this) {
      destroyed = false;
      resetPlaybackStateLocked();
      clearPendingTrackChangesLocked();
      clearCommandQueueLocked();
      clearPendingPlayerMethodOverrideLocked();
      loading = false;
    }
    resetDisplay();
    setStatusByKey(Configuration.PLAYER_STATUS_STOPPED);
    parent.updateDisplay();
  }

  public void toggle() {
    if (isPlaying()) {
      pause();
    } else {
      play();
    }
  }

  public void next() {
    changeTrack(true, true);
  }

  public void previous() {
    changeTrack(false, true);
  }

  public void adjustVolume(boolean increase) {
    VolumeControl vc = getVolumeControl();
    if (vc == null) {
      return;
    }
    if (increase) {
      volumeLevel = Math.min(Configuration.PLAYER_MAX_VOLUME, volumeLevel + VOLUME_STEP);
    } else {
      volumeLevel = Math.max(0, volumeLevel - VOLUME_STEP);
    }
    vc.setLevel(volumeLevel);
    parent.showVolumeAlert(volumeLevel);
  }

  public void setVolumeLevel(int level) {
    VolumeControl vc = getVolumeControl();
    if (vc == null) {
      return;
    }
    volumeLevel = Math.max(0, Math.min(Configuration.PLAYER_MAX_VOLUME, level));
    vc.setLevel(volumeLevel);
  }

  public int getVolumeLevel() {
    return volumeLevel;
  }

  public void toggleRepeat() {
    if (Configuration.PLAYER_REPEAT_OFF == repeatMode) {
      repeatMode = Configuration.PLAYER_REPEAT_ONE;
    } else if (Configuration.PLAYER_REPEAT_ONE == repeatMode) {
      repeatMode = Configuration.PLAYER_REPEAT_ALL;
    } else {
      repeatMode = Configuration.PLAYER_REPEAT_OFF;
    }
    saveRepeatMode();
  }

  public void toggleShuffle() {
    isShuffleEnabled = !isShuffleEnabled;
    synchronized (this) {
      if (isShuffleEnabled) {
        createShuffleLocked();
      } else {
        shuffleOrder = null;
        shufflePosition = 0;
      }
    }
    saveShuffleMode();
  }

  public void seek(long time) {
    if (time < 0) {
      return;
    }
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }
    if (currentPlayer == null) {
      return;
    }
    try {
      currentPlayer.setMediaTime(time);
      synchronized (this) {
        cachedMediaTimeMicros = time;
        cachedSampleTimeMs = System.currentTimeMillis();
        cachedSessionId = playbackSessionId;
      }
      parent.updateDisplay();
    } catch (MediaException e) {
    }
  }

  public void cleanup() {
    synchronized (this) {
      destroyed = true;
      resetPlaybackStateLocked();
      clearPendingTrackChangesLocked();
      clearCommandQueueLocked();
      clearPendingPlayerMethodOverrideLocked();
      shuffleOrder = null;
      shufflePosition = 0;
    }
    stopCommandWorker();

    resetDisplay();
    setStatus("");
  }

  public boolean isPlaying() {
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }
    if (currentPlayer == null) {
      return false;
    }
    try {
      return currentPlayer.getState() >= Player.STARTED;
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized Track getCurrentTrack() {
    return getCurrentTrackLocked();
  }

  public synchronized Tracks getCurrentTracks() {
    return trackList;
  }

  public long getCurrentTime() {
    Player currentPlayer;
    boolean mayEstimateFromCache = false;
    long cachedTime = 0L;
    long cachedDuration = 0L;
    long cachedAt = 0L;
    int sessionId;
    synchronized (this) {
      currentPlayer = player;
      sessionId = playbackSessionId;
      if (currentPlayer != null && cachedSessionId == playbackSessionId) {
        mayEstimateFromCache = true;
        cachedTime = cachedMediaTimeMicros;
        cachedDuration = cachedDurationMicros;
        cachedAt = cachedSampleTimeMs;
      }
    }
    if (currentPlayer == null) {
      return 0L;
    }

    long now = System.currentTimeMillis();
    if (mayEstimateFromCache && cachedAt > 0L && now - cachedAt < MEDIA_SAMPLE_WINDOW_MS) {
      long estimated = cachedTime;
      if (isPlayerStarted(currentPlayer)) {
        long deltaMicros = (now - cachedAt) * 1000L;
        estimated += deltaMicros;
      }
      if (cachedDuration > 0L && estimated > cachedDuration) {
        estimated = cachedDuration;
      }
      if (estimated < 0L) {
        estimated = 0L;
      }
      return estimated;
    }

    long sampledTime = getMediaTimeSafe(currentPlayer);
    long sampledDuration = getDurationSafe(currentPlayer);
    synchronized (this) {
      if (sessionId == playbackSessionId) {
        cachedMediaTimeMicros = sampledTime;
        cachedDurationMicros = sampledDuration;
        cachedSampleTimeMs = now;
        cachedSessionId = playbackSessionId;
      }
    }
    return sampledTime;
  }

  public long getDuration() {
    Track track = getCurrentTrack();
    if (track == null) {
      return 0L;
    }

    long duration = ((long) track.getDuration()) * 1000000L;
    if (duration > 0) {
      return duration;
    }

    Player currentPlayer;
    int sessionId;
    synchronized (this) {
      currentPlayer = player;
      sessionId = playbackSessionId;
      if (cachedSessionId == playbackSessionId && cachedDurationMicros > 0L) {
        return cachedDurationMicros;
      }
    }
    if (currentPlayer == null) {
      return 0L;
    }

    long sampledDuration = getDurationSafe(currentPlayer);
    synchronized (this) {
      if (sampledDuration > 0L && sessionId == playbackSessionId) {
        cachedDurationMicros = sampledDuration;
        cachedSessionId = playbackSessionId;
      }
    }
    return sampledDuration;
  }

  public int getRepeatMode() {
    return repeatMode;
  }

  public boolean isShuffleEnabled() {
    return isShuffleEnabled;
  }

  public synchronized int getCurrentIndex() {
    return currentTrackIndex;
  }

  public synchronized Tracks getPlaylist() {
    return trackList;
  }

  public void playerUpdate(Player p, String event, Object eventData) {
    if (!isCurrentPlayer(p)) {
      return;
    }

    try {
      if (PlayerListener.STARTED.equals(event)) {
        onPlaybackStarted();
      } else if (PlayerListener.END_OF_MEDIA.equals(event)) {
        handleTrackEnd();
      } else if (PlayerListener.ERROR.equals(event)) {
        if (handlePlayerError(eventData)) {
          return;
        }
        stopTimer();
        parent.updateDisplay();
      } else if (PlayerListener.STOPPED.equals(event) || PlayerListener.CLOSED.equals(event)) {
        stopTimer();
        parent.updateDisplay();
      } else if (PlayerListener.DURATION_UPDATED.equals(event)) {
        parent.updateDisplay();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void onPlaybackStarted() {
    int sessionId;
    synchronized (this) {
      loading = false;
      handlingTrackEnd = false;
      sessionStarted = true;
      sessionMethodSessionId = playbackSessionId;
      startWatchdogSessionId = -1;
      cachedSampleTimeMs = System.currentTimeMillis();
      cachedSessionId = playbackSessionId;
      sessionId = playbackSessionId;
    }
    startTimer();
    setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
    enqueueCommand(COMMAND_APPLY_PENDING, sessionId);
  }

  private void startLoadThread() {
    final int sessionId;
    final String playerMethod;

    synchronized (this) {
      if (destroyed || loading) {
        return;
      }
      loading = true;
      playbackSessionId++;
      sessionId = playbackSessionId;
      playerMethod = getNextPlayerHttpMethodLocked();
      sessionStarted = false;
      sessionUsedInputStream = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(playerMethod);
      sessionMethodSessionId = sessionId;
      startWatchdogSessionId = -1;
    }

    setStatusByKey(Configuration.PLAYER_STATUS_LOADING);

    Thread loadThread =
        new Thread(
            new Runnable() {
              public void run() {
                loadAndPlaySession(sessionId, playerMethod);
              }
            });
    loadThread.start();
  }

  private void loadAndPlaySession(int sessionId, String playerMethod) {
    PendingPlayback pending = null;

    try {
      Track track = getTrackForSession(sessionId);
      if (track == null || !isSessionActive(sessionId)) {
        return;
      }

      parent.setAlbumArtUrl(track.getImageUrl());
      pending = createPendingPlayback(track, sessionId, playerMethod);

      if (pending == null || pending.pendingPlayer == null || !isSessionActive(sessionId)) {
        return;
      }

      preparePendingPlayer(pending.pendingPlayer, sessionId);

      if (!isSessionActive(sessionId)) {
        return;
      }

      attachPendingPlayback(pending, sessionId);
      pending = null;

      parent.setupDisplay();
      startPlayback();
    } catch (IOException e) {
      if (isSessionActive(sessionId)) {
        if (recoverFromFailure("load io error: " + e.toString())) {
          return;
        }
        handleLoadError(e);
      }
    } catch (MediaException me) {
      if (isSessionActive(sessionId)) {
        if (recoverFromFailure("load media error: " + me.toString())) {
          return;
        }
        handleError(me);
      }
    } catch (Exception e) {
      if (isSessionActive(sessionId)) {
        if (recoverFromFailure("load error: " + e.toString())) {
          return;
        }
        handleLoadError(e);
      }
    } finally {
      closePendingPlayback(pending);
      finishLoadSession(sessionId);
    }
  }

  private PendingPlayback createPendingPlayback(Track track, int sessionId, String playerMethod)
      throws IOException, MediaException {
    String trackUrl = track.getUrl();
    if (trackUrl == null || trackUrl.length() == 0) {
      throw new IOException("No track URL");
    }

    PendingPlayback pending = new PendingPlayback();
    String finalUrl = resolveRedirect(trackUrl, sessionId);

    if (!isSessionActive(sessionId)) {
      return pending;
    }

    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(playerMethod)) {
      if (!createInputStreamPlayback(pending, finalUrl, sessionId)) {
        pending.usedInputStream = false;
        pending.pendingPlayer = Manager.createPlayer(finalUrl);
      }
    } else {
      pending.usedInputStream = false;
      pending.pendingPlayer = Manager.createPlayer(finalUrl);
    }

    return pending;
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
    if (!isSessionActive(sessionId)) {
      return true;
    }

    pending.pendingInputStream = pending.pendingConnection.openInputStream();
    if (!isSessionActive(sessionId)) {
      return true;
    }

    pending.pendingPlayer = Manager.createPlayer(pending.pendingInputStream, contentType);
    return true;
  }

  private void preparePendingPlayer(Player pendingPlayer, int sessionId) throws MediaException {
    if (pendingPlayer == null || !isSessionActive(sessionId)) {
      return;
    }

    pendingPlayer.addPlayerListener(this);
    pendingPlayer.realize();

    if (!isSessionActive(sessionId)) {
      return;
    }

    pendingPlayer.prefetch();

    if (!isSessionActive(sessionId)) {
      return;
    }

    VolumeControl vc = getVolumeControl(pendingPlayer);
    if (vc != null) {
      vc.setLevel(volumeLevel);
    }
  }

  private synchronized void attachPendingPlayback(PendingPlayback pending, int sessionId) {
    if (pending == null || pending.pendingPlayer == null || !isSessionActiveLocked(sessionId)) {
      return;
    }

    closePlayerLocked();
    closeResourcesLocked();

    player = pending.pendingPlayer;
    httpConnection = pending.pendingConnection;
    inputStream = pending.pendingInputStream;
    sessionUsedInputStream = pending.usedInputStream;
    sessionStarted = false;
    sessionMethodSessionId = sessionId;
    startWatchdogSessionId = -1;

    pending.pendingPlayer = null;
    pending.pendingConnection = null;
    pending.pendingInputStream = null;
  }

  private void closePendingPlayback(PendingPlayback pending) {
    if (pending == null) {
      return;
    }

    closePlayerQuietly(pending.pendingPlayer);
    closeInputStreamQuietly(pending.pendingInputStream);
    closeHttpConnectionQuietly(pending.pendingConnection);

    pending.pendingPlayer = null;
    pending.pendingInputStream = null;
    pending.pendingConnection = null;
  }

  private synchronized void finishLoadSession(int sessionId) {
    boolean shouldApplyPending = false;
    if (sessionId == playbackSessionId) {
      loading = false;
      shouldApplyPending = pendingTrackDelta != 0;
    }
    if (shouldApplyPending) {
      enqueueCommand(COMMAND_APPLY_PENDING, sessionId);
    }
  }

  private String resolveRedirect(String url, int sessionId) throws IOException {
    if (url == null || url.length() == 0) {
      throw new IOException("Invalid media URL");
    }

    String currentUrl = url;
    int redirectCount = 0;
    while (isSessionActive(sessionId)) {
      MediaResolutionResult result = resolveMediaUrlOnce(currentUrl, sessionId);
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
      waitForProcessingRetry(sessionId);
    }

    return currentUrl;
  }

  private MediaResolutionResult resolveMediaUrlOnce(String url, int sessionId) throws IOException {
    HttpConnection connection = null;
    InputStream responseStream = null;
    try {
      if (!isSessionActive(sessionId)) {
        return new MediaResolutionResult(url, false, -1, null);
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
          return new MediaResolutionResult(redirectUrl, false, -1, null);
        }
      }

      if (responseCode == PROCESSING_RESPONSE_CODE) {
        responseStream = connection.openInputStream();
        return parseProcessingResponse(url, readResponseBody(responseStream));
      }

      return new MediaResolutionResult(url, false, -1, null);
    } finally {
      closeInputStreamQuietly(responseStream);
      closeHttpConnectionQuietly(connection);
    }
  }

  private MediaResolutionResult parseProcessingResponse(String url, String responseBody) {
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

    return new MediaResolutionResult(url, true, progress, status);
  }

  private String readResponseBody(InputStream stream) throws IOException {
    if (stream == null) {
      return "";
    }

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[RESPONSE_BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = stream.read(buffer)) != -1) {
      output.write(buffer, 0, bytesRead);
    }
    try {
      return new String(output.toByteArray(), "UTF-8");
    } catch (Exception e) {
      return new String(output.toByteArray());
    }
  }

  private void updateProcessingStatus(int progress, String status, int sessionId) {
    if (!isSessionActive(sessionId)) {
      return;
    }

    String loadingText = Lang.tr(Configuration.PLAYER_STATUS_LOADING);
    StringBuffer statusBuffer = new StringBuffer(loadingText);
    if (progress >= 0) {
      statusBuffer.append(' ').append(progress).append('%');
    } else if (status != null && status.length() > 0) {
      statusBuffer.append(' ').append(status);
    }
    setStatus(statusBuffer.toString());
  }

  private void waitForProcessingRetry(int sessionId) {
    long remaining = PROCESSING_RETRY_INTERVAL_MS;
    while (remaining > 0L && isSessionActive(sessionId)) {
      long sleepTime =
          remaining > PROCESSING_RETRY_SLICE_MS ? PROCESSING_RETRY_SLICE_MS : remaining;
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      }
      remaining -= sleepTime;
    }
  }

  private synchronized String getPreferredInputStreamContentType() {
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

  private void startPlayback() {
    Player currentPlayer;
    int sessionId;
    synchronized (this) {
      if (destroyed) {
        return;
      }
      currentPlayer = player;
      sessionId = playbackSessionId;
    }

    if (currentPlayer == null) {
      return;
    }

    try {
      long duration = getDurationSafe(currentPlayer);
      long currentTime = getMediaTimeSafe(currentPlayer);
      boolean started = false;

      if (duration > 0 && currentTime >= duration) {
        currentPlayer.setMediaTime(0L);
      }

      if (currentPlayer.getState() < Player.STARTED) {
        currentPlayer.start();
        started = true;
      }

      if (started) {
        scheduleStartWatchdog(sessionId);
      }
    } catch (MediaException me) {
      if (isCurrentPlayer(currentPlayer)) {
        handleError(me);
      }
    } catch (Exception e) {
      if (isCurrentPlayer(currentPlayer)) {
        handleLoadError(e);
      }
    }
  }

  private void changeTrack(boolean forward, boolean queueIfLoading) {
    boolean trackChanged = false;

    synchronized (this) {
      if (loading || trackList == null || trackList.getTracks() == null) {
        if (loading && queueIfLoading) {
          enqueuePendingTrackChangeLocked(forward ? 1 : -1);
        }
        return;
      }

      Track[] tracks = trackList.getTracks();
      if (tracks.length == 0) {
        return;
      }

      if (isShuffleEnabled) {
        trackChanged = changeTrackShuffleLocked(forward);
      } else {
        trackChanged = changeTrackNormalLocked(forward);
      }

      if (trackChanged) {
        resetPlaybackStateLocked();
      }
    }

    if (trackChanged) {
      resetDisplay();
      play();
    }
  }

  private boolean changeTrackNormalLocked(boolean forward) {
    Track[] tracks = trackList.getTracks();
    if (tracks.length == 1) {
      return false;
    }

    if (forward) {
      currentTrackIndex++;
      if (currentTrackIndex >= tracks.length) {
        if (Configuration.PLAYER_REPEAT_OFF == repeatMode) {
          currentTrackIndex = tracks.length - 1;
          return false;
        }
        currentTrackIndex = 0;
      }
    } else {
      currentTrackIndex--;
      if (currentTrackIndex < 0) {
        if (Configuration.PLAYER_REPEAT_OFF == repeatMode) {
          currentTrackIndex = 0;
          return false;
        }
        currentTrackIndex = tracks.length - 1;
      }
    }

    return true;
  }

  private boolean changeTrackShuffleLocked(boolean forward) {
    if (shuffleOrder == null || shuffleOrder.length == 0) {
      createShuffleLocked();
      if (shuffleOrder == null || shuffleOrder.length == 0) {
        return false;
      }
    }

    if (shuffleOrder.length == 1) {
      return false;
    }

    if (forward) {
      shufflePosition++;
      if (shufflePosition >= shuffleOrder.length) {
        if (Configuration.PLAYER_REPEAT_OFF == repeatMode) {
          shufflePosition = shuffleOrder.length - 1;
          return false;
        }
        createShuffleLocked();
        shufflePosition = 0;
      }
    } else {
      shufflePosition--;
      if (shufflePosition < 0) {
        if (Configuration.PLAYER_REPEAT_OFF == repeatMode) {
          shufflePosition = 0;
          return false;
        }
        shufflePosition = shuffleOrder.length - 1;
      }
    }

    currentTrackIndex = shuffleOrder[shufflePosition];
    return true;
  }

  private synchronized void createShuffleLocked() {
    if (trackList == null || trackList.getTracks() == null) {
      shuffleOrder = null;
      shufflePosition = 0;
      return;
    }

    Track[] tracks = trackList.getTracks();
    int size = tracks.length;
    if (size <= 0) {
      shuffleOrder = null;
      shufflePosition = 0;
      return;
    }

    shuffleOrder = new int[size];
    for (int i = 0; i < size; i++) {
      shuffleOrder[i] = i;
    }

    for (int i = size - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int temp = shuffleOrder[i];
      shuffleOrder[i] = shuffleOrder[j];
      shuffleOrder[j] = temp;
    }

    shufflePosition = 0;
    for (int i = 0; i < size; i++) {
      if (shuffleOrder[i] == currentTrackIndex) {
        shufflePosition = i;
        break;
      }
    }
  }

  private void handleTrackEnd() {
    int sessionId;
    synchronized (this) {
      if (destroyed || handlingTrackEnd) {
        return;
      }
      handlingTrackEnd = true;
      sessionId = playbackSessionId;
    }

    stopTimer();
    parent.updateDisplay();
    enqueueCommand(COMMAND_TRACK_END, sessionId);
  }

  private void processTrackEnd() {
    try {
      boolean shouldRepeatCurrent;
      boolean shouldGoNext;

      synchronized (this) {
        if (destroyed) {
          return;
        }

        Track[] tracks = trackList != null ? trackList.getTracks() : null;
        shouldRepeatCurrent =
            repeatMode == Configuration.PLAYER_REPEAT_ONE
                || (repeatMode == Configuration.PLAYER_REPEAT_ALL
                    && tracks != null
                    && tracks.length == 1);

        shouldGoNext = !shouldRepeatCurrent && hasNextTrackLocked();
      }

      if (shouldRepeatCurrent) {
        restartCurrentTrack();
      } else if (shouldGoNext) {
        changeTrack(true, false);
      } else {
        setStatusByKey(Configuration.PLAYER_STATUS_FINISHED);
      }
    } finally {
      synchronized (this) {
        handlingTrackEnd = false;
      }
    }
  }

  private void restartCurrentTrack() {
    synchronized (this) {
      if (loading || destroyed) {
        return;
      }
      resetPlaybackStateLocked();
    }

    resetDisplay();
    play();
  }

  private synchronized boolean hasNextTrackLocked() {
    if (trackList == null || trackList.getTracks() == null) {
      return false;
    }

    Track[] tracks = trackList.getTracks();
    if (tracks.length <= 1) {
      return false;
    }

    if (Configuration.PLAYER_REPEAT_ALL == repeatMode) {
      return true;
    }

    if (isShuffleEnabled) {
      if (shuffleOrder == null || shuffleOrder.length == 0) {
        return false;
      }
      return shufflePosition < shuffleOrder.length - 1;
    }

    return currentTrackIndex < tracks.length - 1;
  }

  private VolumeControl getVolumeControl(Player targetPlayer) {
    if (targetPlayer == null) {
      return null;
    }
    try {
      return (VolumeControl) targetPlayer.getControl("VolumeControl");
    } catch (Exception e) {
      return null;
    }
  }

  private VolumeControl getVolumeControl() {
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }
    return getVolumeControl(currentPlayer);
  }

  private void startTimer() {
    synchronized (this) {
      ensureTimerLocked();
      if (displayTask == null) {
        displayTask =
            new TimerTask() {
              public void run() {
                parent.updateDisplay();
              }
            };
        mainTimer.scheduleAtFixedRate(displayTask, 0L, TIMER_INTERVAL);
      }
    }
  }

  private void stopTimer() {
    synchronized (this) {
      stopTimerLocked();
    }
    parent.updateDisplay();
  }

  private void ensureTimerLocked() {
    if (mainTimer == null) {
      mainTimer = new Timer();
    }
  }

  private void stopTimerLocked() {
    if (displayTask != null) {
      displayTask.cancel();
      displayTask = null;
    }
  }

  private void cleanupTimersLocked() {
    stopTimerLocked();
    if (mainTimer != null) {
      mainTimer.cancel();
      mainTimer = null;
    }
  }

  private synchronized void resetPlaybackStateLocked() {
    invalidateSessionLocked();
    loading = false;
    handlingTrackEnd = false;
    sessionUsedInputStream = false;
    sessionStarted = false;
    sessionMethodSessionId = -1;
    startWatchdogSessionId = -1;
    resetMediaCacheLocked();
    closePlayerLocked();
    closeResourcesLocked();
    cleanupTimersLocked();
  }

  private synchronized void clearPendingTrackChangesLocked() {
    pendingTrackDelta = 0;
  }

  private synchronized void clearPendingPlayerMethodOverrideLocked() {
    pendingPlayerMethodOverride = null;
  }

  private void resetMediaCacheLocked() {
    cachedMediaTimeMicros = 0L;
    cachedDurationMicros = 0L;
    cachedSampleTimeMs = 0L;
    cachedSessionId = playbackSessionId;
  }

  private void enqueuePendingTrackChangeLocked(int delta) {
    pendingTrackDelta += delta;
    if (pendingTrackDelta > MAX_PENDING_TRACK_STEPS) {
      pendingTrackDelta = MAX_PENDING_TRACK_STEPS;
    } else if (pendingTrackDelta < -MAX_PENDING_TRACK_STEPS) {
      pendingTrackDelta = -MAX_PENDING_TRACK_STEPS;
    }
  }

  private synchronized int pollPendingTrackDirectionLocked() {
    if (loading || destroyed) {
      return 0;
    }

    if (pendingTrackDelta > 0) {
      pendingTrackDelta--;
      return 1;
    }

    if (pendingTrackDelta < 0) {
      pendingTrackDelta++;
      return -1;
    }

    return 0;
  }

  private void applyPendingTrackChange() {
    int direction = pollPendingTrackDirectionLocked();
    if (direction == 0) {
      return;
    }

    changeTrack(direction > 0, false);
  }

  private void startCommandWorker() {
    synchronized (commandQueue) {
      if (commandThread != null && commandThread.isAlive()) {
        return;
      }

      commandWorkerStopRequested = false;
      commandThread =
          new Thread(
              new Runnable() {
                public void run() {
                  commandLoop();
                }
              });
      commandThread.start();
    }
  }

  private void stopCommandWorker() {
    synchronized (commandQueue) {
      commandWorkerStopRequested = true;
      commandQueue.removeAllElements();
      commandQueue.notifyAll();
    }
  }

  private void clearCommandQueueLocked() {
    synchronized (commandQueue) {
      commandQueue.removeAllElements();
    }
  }

  private void enqueueCommand(int commandType, int sessionId) {
    if (sessionId <= 0) {
      return;
    }
    startCommandWorker();
    synchronized (commandQueue) {
      commandQueue.addElement(new CommandTask(commandType, sessionId));
      commandQueue.notifyAll();
    }
  }

  private CommandTask pollCommand() {
    synchronized (commandQueue) {
      while (commandQueue.size() == 0 && !commandWorkerStopRequested) {
        try {
          commandQueue.wait();
        } catch (InterruptedException e) {
        }
      }
      if (commandWorkerStopRequested) {
        return null;
      }

      CommandTask command = (CommandTask) commandQueue.elementAt(0);
      commandQueue.removeElementAt(0);
      return command;
    }
  }

  private synchronized boolean isCurrentSession(int sessionId) {
    return sessionId == playbackSessionId;
  }

  private void commandLoop() {
    while (true) {
      CommandTask command = pollCommand();
      if (command == null) {
        synchronized (commandQueue) {
          if (commandWorkerStopRequested) {
            commandWorkerStopRequested = false;
            commandThread = null;
            return;
          }
        }
        continue;
      }

      if (!isCurrentSession(command.sessionId)) {
        if (command.type == COMMAND_TRACK_END) {
          synchronized (this) {
            handlingTrackEnd = false;
          }
        }
        continue;
      }

      if (command.type == COMMAND_TRACK_END) {
        processTrackEnd();
      } else if (command.type == COMMAND_APPLY_PENDING) {
        applyPendingTrackChange();
      }
    }
  }

  private void closePlayerLocked() {
    if (player != null) {
      closePlayerQuietly(player);
      player = null;
    }
  }

  private void closeResourcesLocked() {
    if (inputStream != null) {
      closeInputStreamQuietly(inputStream);
      inputStream = null;
    }
    if (httpConnection != null) {
      closeHttpConnectionQuietly(httpConnection);
      httpConnection = null;
    }
  }

  private void closePlayerQuietly(Player targetPlayer) {
    if (targetPlayer == null) {
      return;
    }

    try {
      targetPlayer.removePlayerListener(this);
    } catch (Exception e) {
    }

    try {
      int state = targetPlayer.getState();
      if (state != Player.CLOSED && state >= Player.REALIZED) {
        targetPlayer.stop();
      }
    } catch (Exception e) {
    }

    try {
      targetPlayer.close();
    } catch (Exception e) {
    }
  }

  private void closeInputStreamQuietly(InputStream stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }
  }

  private void closeHttpConnectionQuietly(HttpConnection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (IOException e) {
      }
    }
  }

  private synchronized void invalidateSessionLocked() {
    playbackSessionId++;
  }

  private synchronized boolean isSessionActiveLocked(int sessionId) {
    return !destroyed && sessionId == playbackSessionId;
  }

  private synchronized boolean isSessionActive(int sessionId) {
    return isSessionActiveLocked(sessionId);
  }

  private synchronized boolean isCurrentPlayer(Player candidate) {
    return !destroyed && candidate != null && candidate == player;
  }

  private synchronized Track getTrackForSession(int sessionId) {
    if (!isSessionActiveLocked(sessionId)) {
      return null;
    }
    return getCurrentTrackLocked();
  }

  private Track getCurrentTrackLocked() {
    if (trackList == null || trackList.getTracks() == null) {
      return null;
    }

    Track[] tracks = trackList.getTracks();
    if (currentTrackIndex >= 0 && currentTrackIndex < tracks.length) {
      return tracks[currentTrackIndex];
    }

    return null;
  }

  private boolean isPlayerReady(Player targetPlayer) {
    if (targetPlayer == null) {
      return false;
    }

    try {
      int state = targetPlayer.getState();
      return state >= Player.PREFETCHED && state != Player.CLOSED;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isPlayerStarted(Player targetPlayer) {
    if (targetPlayer == null) {
      return false;
    }

    try {
      return targetPlayer.getState() >= Player.STARTED;
    } catch (Exception e) {
      return false;
    }
  }

  private long getMediaTimeSafe(Player targetPlayer) {
    try {
      long value = targetPlayer.getMediaTime();
      return value > 0 ? value : 0L;
    } catch (Exception e) {
      return 0L;
    }
  }

  private long getDurationSafe(Player targetPlayer) {
    try {
      long value = targetPlayer.getDuration();
      return value > 0 ? value : 0L;
    } catch (Exception e) {
      return 0L;
    }
  }

  private void resetDisplay() {
    parent.resetTruncatedText();
    parent.resetDurationText();
    parent.resetImage();
  }

  private void handleError(MediaException me) {
    setStatus(Lang.tr("status.error"));
    parent.showError(me.toString());
  }

  private void handleLoadError(Exception e) {
    setStatus(Lang.tr("status.error"));
    parent.showError(e.toString());
  }

  private void setStatus(String status) {
    parent.setStatus(status == null ? "" : status);
  }

  private void setStatusByKey(String statusKey) {
    parent.setStatusByKey(statusKey);
  }

  public void saveVolumeLevel() {
    try {
      settingsManager.saveVolumeLevel(volumeLevel);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  private void saveRepeatMode() {
    try {
      settingsManager.saveRepeatMode(repeatMode);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  private void saveShuffleMode() {
    try {
      int mode =
          isShuffleEnabled ? Configuration.PLAYER_SHUFFLE_ON : Configuration.PLAYER_SHUFFLE_OFF;
      settingsManager.saveShuffleMode(mode);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  private String getConfiguredPlayerHttpMethod() {
    String configuredMethod = settingsManager.getCurrentPlayerMethod();
    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(configuredMethod)
        || Configuration.PLAYER_METHOD_PASS_URL.equals(configuredMethod)) {
      return configuredMethod;
    }

    String detectedMethod = Utils.getPlayerHttpMethod();
    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(detectedMethod)) {
      return Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    }

    return Configuration.PLAYER_METHOD_PASS_URL;
  }

  private synchronized String getNextPlayerHttpMethodLocked() {
    String playerMethod = pendingPlayerMethodOverride;
    pendingPlayerMethodOverride = null;
    if (playerMethod == null || playerMethod.length() == 0) {
      playerMethod = getConfiguredPlayerHttpMethod();
    }
    return playerMethod;
  }

  public void pauseApp() {
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }

    if (currentPlayer != null) {
      try {
        if (currentPlayer.getState() >= Player.STARTED) {
          currentPlayer.stop();
        }
      } catch (Exception e) {
      }
    }

    stopTimer();
  }

  public void resumeApp() {
    Player currentPlayer;
    synchronized (this) {
      if (loading) {
        return;
      }
      currentPlayer = player;
    }

    if (currentPlayer != null) {
      try {
        if (currentPlayer.getState() == Player.PREFETCHED) {
          currentPlayer.start();
        }
      } catch (Exception e) {
      }
    }
  }

  private boolean handlePlayerError(Object eventData) {
    String errorText = eventData == null ? "unknown" : String.valueOf(eventData);
    return recoverFromFailure("player error: " + errorText);
  }

  private void scheduleStartWatchdog(final int sessionId) {
    synchronized (this) {
      if (sessionId != playbackSessionId || startWatchdogSessionId == sessionId) {
        return;
      }
      startWatchdogSessionId = sessionId;
    }

    Thread watchdog =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  Thread.sleep(2500L);
                } catch (InterruptedException e) {
                }

                boolean shouldPromoteStarted = false;
                boolean recovered = false;
                synchronized (PlayerGUI.this) {
                  if (sessionId == playbackSessionId) {
                    if (!sessionStarted && isPlayerStarted(player)) {
                      shouldPromoteStarted = true;
                    } else if (!sessionStarted) {
                      recovered = tryRecoverInputStreamFailureLocked("start watchdog timeout");
                    }
                  }
                  if (startWatchdogSessionId == sessionId) {
                    startWatchdogSessionId = -1;
                  }
                }

                if (shouldPromoteStarted) {
                  onPlaybackStarted();
                  return;
                }

                if (recovered) {
                  restartAfterRecovery();
                }
              }
            });
    watchdog.start();
  }

  private boolean recoverFromFailure(String reason) {
    boolean recovered;
    synchronized (this) {
      recovered = tryRecoverPlayerMethodFailureLocked(reason);
    }

    if (recovered) {
      restartAfterRecovery();
      return true;
    }

    return false;
  }

  private void restartAfterRecovery() {
    stopTimer();
    setStatusByKey(Configuration.PLAYER_STATUS_LOADING);
    play();
  }

  private boolean tryRecoverPlayerMethodFailureLocked(String reason) {
    if (tryRecoverInputStreamFailureLocked(reason)) {
      return true;
    }
    return tryRecoverUrlContentTypeFailureLocked(reason);
  }

  private boolean tryRecoverInputStreamFailureLocked(String reason) {
    int sessionId = playbackSessionId;
    if (destroyed || sessionId <= 0) {
      return false;
    }

    if (sessionMethodSessionId != sessionId || !sessionUsedInputStream || sessionStarted) {
      return false;
    }

    if (lastFallbackSessionId == sessionId) {
      return false;
    }

    lastFallbackSessionId = sessionId;
    resetPlaybackStateLocked();
    pendingPlayerMethodOverride = Configuration.PLAYER_METHOD_PASS_URL;
    return true;
  }

  private boolean tryRecoverUrlContentTypeFailureLocked(String reason) {
    int sessionId = playbackSessionId;
    if (destroyed || sessionId <= 0) {
      return false;
    }

    if (sessionMethodSessionId != sessionId || sessionUsedInputStream || sessionStarted) {
      return false;
    }

    if (getPreferredInputStreamContentType() == null) {
      return false;
    }

    if (!isUnknownContentTypeFailure(reason) || lastFallbackSessionId == sessionId) {
      return false;
    }

    lastFallbackSessionId = sessionId;
    resetPlaybackStateLocked();
    pendingPlayerMethodOverride = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    return true;
  }

  private boolean isUnknownContentTypeFailure(String reason) {
    if (reason == null) {
      return false;
    }

    String normalized = reason.toLowerCase();
    return normalized.indexOf("unknown content type") != -1
        || normalized.indexOf("unsupported content type") != -1
        || (normalized.indexOf("unknown") != -1
            && (normalized.indexOf("content type") != -1
                || normalized.indexOf("content-type") != -1));
  }
}
