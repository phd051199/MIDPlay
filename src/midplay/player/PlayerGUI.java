package midplay.player;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.io.HttpConnection;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.net.MediaHttpClient;
import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.util.Lang;
import midplay.util.Utils;

public class PlayerGUI implements PlayerListener {
  private static final int TIMER_INTERVAL = 1000;
  private static final int VOLUME_STEP = 10;
  private static final int MAX_PENDING_TRACK_STEPS = 20;
  private static final long MEDIA_SAMPLE_WINDOW_MS = 1200L;

  private final PlayerScreen parent;
  private final SettingsManager settingsManager;
  private final SettingsPersistence settingsPersistence;
  private final MediaResolver mediaResolver = new MediaResolver(this);
  private final PlaybackRecovery recovery = new PlaybackRecovery(this);
  private final CommandWorker commandWorker = new CommandWorker(this);
  private Player player;
  private Tracks trackList;
  private int currentTrackIndex;
  int volumeLevel = Configuration.PLAYER_MAX_VOLUME;
  int repeatMode = Configuration.PLAYER_REPEAT_ALL;
  boolean isShuffleEnabled = false;
  private final ShuffleController shuffle = new ShuffleController();
  private HttpConnection httpConnection;
  private InputStream inputStream;
  private String currentResolvedUrl;
  private long currentContentLength = -1;
  private long pendingResumeSeekMicros = -1;
  private long deferredResumeSeekMicros = -1;
  private Timer mainTimer;
  private TimerTask displayTask;
  private boolean repaintSuppressed = false;

  private volatile Thread loadThread;
  private TimerTask startWatchdogTask;

  private volatile boolean loading = false;
  private boolean destroyed = false;
  private int playbackSessionId = 0;
  private int playlistGeneration = 0;
  private boolean handlingTrackEnd = false;
  private int pendingTrackDelta = 0;
  private PlaybackMethod pendingPlayerMethodOverride;
  private boolean sessionUsedInputStream = false;
  private boolean sessionStarted = false;
  private int sessionMethodSessionId = -1;
  private int startWatchdogSessionId = -1;
  private int lastFallbackSessionId = -1;
  private int consecutivePlaybackErrors = 0;
  private static final int MAX_CONSECUTIVE_PLAYBACK_ERRORS = 3;

  private long cachedMediaTimeMicros = 0L;
  private long cachedDurationMicros = 0L;
  private long cachedSampleTimeMs = 0L;
  private int cachedSessionId = -1;

  public PlayerGUI(PlayerScreen parent) {
    this.parent = parent;
    this.settingsManager = SettingsManager.getInstance();
    this.settingsPersistence = new SettingsPersistence(settingsManager);
    settingsPersistence.loadInto(this);
    commandWorker.start();
    setStatus("");
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
      commandWorker.start();
      resetCorePlaybackStateLocked();
      playlistGeneration++;
      this.trackList = tracks;
      this.currentTrackIndex = normalizedIndex;
      if (hasTracks && isShuffleEnabled) {
        createShuffle();
      } else {
        shuffle.clear();
      }
      destroyed = false;
    }

    resetDisplay();
    setStatusByKey(
        hasTracks ? Configuration.PLAYER_STATUS_READY : Configuration.PLAYER_STATUS_STOPPED);
    parent.updateDisplay();
    return hasTracks;
  }

  public synchronized void addToQueue(Track[] toAdd) {
    if (toAdd == null || toAdd.length == 0) {
      return;
    }
    Track[] current = (trackList != null) ? trackList.getTracks() : null;
    int currentLen = (current != null) ? current.length : 0;
    int max = Configuration.QUEUE_MAX_SIZE;
    int kept = 0;
    Track[] filtered = new Track[toAdd.length];
    for (int i = 0; i < toAdd.length; i++) {
      Track candidate = toAdd[i];
      if (candidate == null
          || isTrackPresent(current, candidate)
          || isTrackPresent(filtered, candidate)) {
        continue;
      }
      if (currentLen + kept >= max) {
        break;
      }
      filtered[kept++] = candidate;
    }
    if (kept == 0) {
      return;
    }
    Track[] merged = new Track[currentLen + kept];
    if (current != null) {
      System.arraycopy(current, 0, merged, 0, currentLen);
    }
    System.arraycopy(filtered, 0, merged, currentLen, kept);
    if (trackList == null) {
      trackList = new Tracks();
    }
    trackList.setTracks(merged);
    if (isShuffleEnabled) {
      shuffle.append(merged.length);
    } else {
      shuffle.clear();
    }
  }

  private boolean isTrackPresent(Track[] tracks, Track candidate) {
    if (tracks == null || candidate == null) {
      return false;
    }
    for (int i = 0; i < tracks.length; i++) {
      if (tracks[i] != null && tracks[i].isSame(candidate)) {
        return true;
      }
    }
    return false;
  }

  public synchronized void reorderQueue(Track[] newOrder) {
    if (trackList == null || newOrder == null || newOrder.length == 0) {
      return;
    }
    Track[] unique = dedupFirstOccurrence(newOrder);
    Track current = getCurrentTrackLocked();
    trackList.setTracks(unique);
    currentTrackIndex = 0;
    if (current != null) {
      for (int i = 0; i < unique.length; i++) {
        if (unique[i] != null && current.isSame(unique[i])) {
          currentTrackIndex = i;
          break;
        }
      }
    }
    if (isShuffleEnabled) {
      createShuffle();
    } else {
      shuffle.clear();
    }
  }

  private Track[] dedupFirstOccurrence(Track[] in) {
    int kept = 0;
    Track[] out = new Track[in.length];
    for (int i = 0; i < in.length; i++) {
      Track candidate = in[i];
      if (candidate == null || isTrackPresent(out, candidate)) {
        continue;
      }
      out[kept++] = candidate;
    }
    if (kept == out.length) {
      return out;
    }
    Track[] trimmed = new Track[kept];
    System.arraycopy(out, 0, trimmed, 0, kept);
    return trimmed;
  }

  void play() {
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
      } catch (Exception e) {
      }
    }
    setStatusByKey(Configuration.PLAYER_STATUS_PAUSED);
    stopTimer();
  }

  public synchronized void deallocate() {
    if (player != null) {
      try {
        player.deallocate();
      } catch (Exception e) {
      }
    }
  }

  public void stop() {
    synchronized (this) {
      destroyed = false;
      resetCorePlaybackStateLocked();
      loading = false;
    }
    resetDisplay();
    setStatusByKey(Configuration.PLAYER_STATUS_STOPPED);
    parent.updateDisplayAsync();
  }

  public void toggle() {
    if (isLoading()) {
      return;
    }
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
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }
    VolumeControl vc = getVolumeControl(currentPlayer);
    if (vc == null) {
      return;
    }
    if (increase) {
      volumeLevel = Math.min(Configuration.PLAYER_MAX_VOLUME, volumeLevel + VOLUME_STEP);
    } else {
      volumeLevel = Math.max(0, volumeLevel - VOLUME_STEP);
    }
    vc.setLevel(volumeLevel);
    parent.showVolumeAlert();
  }

  public void setVolumeLevel(int level) {
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }
    VolumeControl vc = getVolumeControl(currentPlayer);
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
    repeatMode = nextRepeatMode(repeatMode);
    saveRepeatMode();
    parent.updateDisplayAsync();
  }

  public void toggleShuffle() {
    isShuffleEnabled = !isShuffleEnabled;
    synchronized (this) {
      if (isShuffleEnabled) {
        createShuffle();
      } else {
        shuffle.clear();
      }
    }
    saveShuffleMode();
    parent.updateDisplayAsync();
  }

  void setPendingResumeSeek(long micros) {
    synchronized (this) {
      pendingResumeSeekMicros = micros;
    }
  }

  public void seek(long time) {
    if (time < 0) {
      return;
    }
    long durationMicros = getDuration();
    if (durationMicros > 0 && time > durationMicros) {
      time = durationMicros;
    }
    Player currentPlayer;
    synchronized (this) {
      if (destroyed) {
        return;
      }
      if (loading) {
        return;
      }
      currentPlayer = player;
    }
    parent.updateDisplayAsync();
    if (currentPlayer == null) {
      return;
    }

    if (currentResolvedUrl != null && currentContentLength > 0) {
      startSeekReload(time);
      return;
    }
    try {
      currentPlayer.setMediaTime(time);
      synchronized (this) {
        cachedMediaTimeMicros = time;
        cachedSampleTimeMs = System.currentTimeMillis();
        cachedSessionId = playbackSessionId;
      }
      parent.updateDisplayAsync();
    } catch (Exception e) {
    }
  }

  public void seekPreview(long time) {
    if (time < 0) {
      time = 0;
    }
    synchronized (this) {
      if (destroyed) {
        return;
      }
      cachedMediaTimeMicros = time;
      cachedSampleTimeMs = System.currentTimeMillis();
      cachedSessionId = playbackSessionId;
    }
    parent.updateDisplayAsync();
  }

  private void startSeekReload(final long seekTimeMicros) {
    final int sessionId;
    final String url;
    final long startByte;
    synchronized (this) {
      if (destroyed || loading) {
        return;
      }
      long length = currentContentLength;
      long duration = getDuration();
      if (currentResolvedUrl == null || length <= 0 || duration <= 0) {
        return;
      }
      long computed = (length * seekTimeMicros) / duration;
      if (computed < 0 || computed >= length) {
        return;
      }
      sessionId = playbackSessionId;
      url = currentResolvedUrl;
      startByte = computed;
      loading = true;
      if (player != null) {
        try {
          int state = player.getState();
          if (state != Player.CLOSED && state >= Player.STARTED) {
            player.stop();
          }
        } catch (Exception e) {
        }
      }
    }
    setStatusByKey(Configuration.PLAYER_STATUS_LOADING);
    Thread seekThread =
        new Thread(
            new Runnable() {
              public void run() {
                loadSeekSession(sessionId, url, startByte, seekTimeMicros);
              }
            });
    this.loadThread = seekThread;
    seekThread.start();
  }

  private void loadSeekSession(
      int sessionId, String resolvedUrl, long startByte, long seekTimeMicros) {
    PendingPlayback pending = null;
    boolean attached = false;
    try {
      if (!isSessionActive(sessionId)) {
        return;
      }
      pending = mediaResolver.createSeekPlayback(resolvedUrl, startByte, sessionId);
      if (pending == null || pending.pendingPlayer == null || !isSessionActive(sessionId)) {
        return;
      }
      mediaResolver.preparePendingPlayer(pending.pendingPlayer, sessionId);
      if (!isSessionActive(sessionId)) {
        return;
      }
      attachPendingPlayback(pending, sessionId);
      pending = null;
      attached = true;
      synchronized (this) {
        cachedMediaTimeMicros = seekTimeMicros;
        cachedSampleTimeMs = System.currentTimeMillis();
        cachedSessionId = playbackSessionId;
      }
      parent.updateDisplayAsync();
      startPlayback();
    } catch (Exception e) {
      if (attached && isSessionActive(sessionId)) {
        handleError(e);
      }
    } finally {
      if (!attached) {
        synchronized (this) {
          closePlayerLocked();
        }
      }
      mediaResolver.closePendingPlayback(pending);
      finishLoadSession(sessionId);
    }
  }

  public void cleanup() {
    synchronized (this) {
      destroyed = true;
      resetCorePlaybackStateLocked();
      shuffle.clear();
    }
    commandWorker.stop();
    shutdownTimer();

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

  public synchronized boolean hasPlayer() {
    return player != null;
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
    boolean started = isPlayerStarted(currentPlayer);
    if (mayEstimateFromCache
        && cachedAt > 0L
        && (!started || now - cachedAt < MEDIA_SAMPLE_WINDOW_MS)) {
      long estimated = extrapolate(cachedTime, cachedAt, now, cachedDuration, started);
      if (estimated < 0L) {
        estimated = 0L;
      }
      return estimated;
    }

    long sampledTime = getMediaTimeSafe(currentPlayer);
    long sampledDuration = getDurationSafe(currentPlayer);

    if (started && mayEstimateFromCache && cachedAt > 0L) {
      long extrapolated = extrapolate(cachedTime, cachedAt, now, cachedDuration, started);
      if (sampledTime < extrapolated) {
        sampledTime = extrapolated;
      }
    }

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

  private long extrapolate(
      long cachedTime, long cachedAt, long now, long cachedDuration, boolean started) {
    long estimated = cachedTime;
    if (started) {
      estimated += (now - cachedAt) * 1000L;
    }
    return estimated;
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

  synchronized boolean isDestroyed() {
    return destroyed;
  }

  synchronized boolean isSessionStarted() {
    return sessionStarted;
  }

  synchronized boolean sessionUsedInputStream() {
    return sessionUsedInputStream;
  }

  synchronized int sessionMethodSessionId() {
    return sessionMethodSessionId;
  }

  synchronized int currentSessionId() {
    return playbackSessionId;
  }

  public synchronized int playlistGeneration() {
    return playlistGeneration;
  }

  synchronized Player currentPlayerLocked() {
    return player;
  }

  synchronized boolean claimStartWatchdog(int sessionId) {
    if (sessionId != playbackSessionId || startWatchdogSessionId == sessionId) {
      return false;
    }
    startWatchdogSessionId = sessionId;
    return true;
  }

  synchronized void releaseStartWatchdog(int sessionId) {
    if (startWatchdogSessionId == sessionId) {
      startWatchdogSessionId = -1;
    }
  }

  synchronized boolean isLastFallbackFor(int sessionId) {
    return lastFallbackSessionId == sessionId;
  }

  synchronized void markLastFallback(int sessionId) {
    lastFallbackSessionId = sessionId;
  }

  synchronized void setPendingPlayerMethodOverride(PlaybackMethod method) {
    pendingPlayerMethodOverride = method;
  }

  boolean hasPreferredInputStreamContentType() {
    return mediaResolver.getPreferredInputStreamContentType() != null;
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
        if (recovery.handlePlayerError(eventData)) {
          return;
        }
        terminalPlaybackError(
            "player error: " + (eventData == null ? "unknown" : String.valueOf(eventData)));
      } else if (PlayerListener.STOPPED.equals(event) || PlayerListener.CLOSED.equals(event)) {
        stopTimer();
        parent.updateDisplayAsync();
      } else if (PlayerListener.DURATION_UPDATED.equals(event)) {
        parent.updateDisplayAsync();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void onPlaybackStarted() {
    int sessionId;
    long resumeSeek;
    synchronized (this) {
      loading = false;
      handlingTrackEnd = false;
      sessionStarted = true;
      sessionMethodSessionId = playbackSessionId;
      startWatchdogSessionId = -1;
      consecutivePlaybackErrors = 0;
      cachedSampleTimeMs = System.currentTimeMillis();
      cachedSessionId = playbackSessionId;
      sessionId = playbackSessionId;
      resumeSeek = pendingResumeSeekMicros;
      pendingResumeSeekMicros = -1;
      if (resumeSeek > 0) {
        deferredResumeSeekMicros = resumeSeek;
      }
    }
    startTimer();
    setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
    commandWorker.enqueue(CommandWorker.APPLY_PENDING, sessionId);

    if (resumeSeek > 0) {
      commandWorker.enqueue(CommandWorker.RESUME_SEEK, sessionId);
    }
  }

  void applyResumeSeek(int sessionId) {
    long target;
    synchronized (this) {
      if (!isSessionActiveLocked(sessionId)) {
        deferredResumeSeekMicros = -1;
        return;
      }
      target = deferredResumeSeekMicros;
      deferredResumeSeekMicros = -1;
    }
    if (target > 0) {
      seek(target);
    }
  }

  private void startLoadThread() {
    final int sessionId;
    final PlaybackMethod playerMethod;

    synchronized (this) {
      if (destroyed || loading) {
        return;
      }
      loading = true;
      playbackSessionId++;
      sessionId = playbackSessionId;
      playerMethod = getNextPlayerHttpMethodLocked();
      sessionStarted = false;
      sessionUsedInputStream = playerMethod.isInputStream();
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
    this.loadThread = loadThread;
    loadThread.start();
  }

  private void loadAndPlaySession(int sessionId, PlaybackMethod playerMethod) {
    PendingPlayback pending = null;

    try {
      Track track = getTrackForSession(sessionId);
      if (track == null || !isSessionActive(sessionId)) {
        return;
      }

      parent.albumArtLoader.setAlbumArtUrl(track.getImageUrl());

      if (playerMethod.isInputStream()) {
        freeHeapForInputStreamPlayback();
      }

      pending = mediaResolver.createPendingPlayback(track, sessionId, playerMethod);

      if (pending == null || pending.pendingPlayer == null || !isSessionActive(sessionId)) {
        return;
      }

      mediaResolver.preparePendingPlayer(pending.pendingPlayer, sessionId);

      if (!isSessionActive(sessionId)) {
        return;
      }

      attachPendingPlayback(pending, sessionId);
      pending = null;

      parent.setupDisplay();
      startPlayback();
    } catch (IOException e) {
      handleLoadFailure(e, "load io error", sessionId);
    } catch (MediaException me) {
      handleLoadFailure(me, "load media error", sessionId);
    } catch (Exception e) {
      handleLoadFailure(e, "load error", sessionId);
    } finally {
      mediaResolver.closePendingPlayback(pending);
      finishLoadSession(sessionId);
    }
  }

  private void handleLoadFailure(Exception e, String label, int sessionId) {
    if (!isSessionActive(sessionId)) {
      return;
    }
    if (recovery.recoverFromFailure(label + ": " + e.toString())) {
      return;
    }
    handleError(e);
  }

  private void freeHeapForInputStreamPlayback() {
    synchronized (this) {
      closePlayerLocked();
      closeResourcesLocked();
    }
    parent.albumArtLoader.freeImageHeap();
    System.gc();
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

    currentResolvedUrl = pending.pendingResolvedUrl;
    if (currentResolvedUrl == null && httpConnection != null) {
      try {
        currentResolvedUrl = httpConnection.getURL();
      } catch (Exception e) {
      }
    }
    currentContentLength =
        httpConnection != null
            ? MediaHttpClient.totalContentLength(httpConnection)
            : pending.pendingContentLength;

    if (pending.connectionWatchdog != null) {
      pending.connectionWatchdog.cancel();
      pending.connectionWatchdog = null;
    }
    pending.pendingPlayer = null;
    pending.pendingConnection = null;
    pending.pendingInputStream = null;
  }

  private synchronized void finishLoadSession(int sessionId) {
    boolean shouldApplyPending = false;
    if (sessionId == playbackSessionId) {
      loading = false;
      shouldApplyPending = pendingTrackDelta != 0;
    }
    if (shouldApplyPending) {
      commandWorker.enqueue(CommandWorker.APPLY_PENDING, sessionId);
    }
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
        recovery.scheduleStartWatchdog(sessionId);
      }
    } catch (MediaException me) {
      if (isCurrentPlayer(currentPlayer)) {
        handleError(me);
      }
    } catch (Exception e) {
      if (isCurrentPlayer(currentPlayer)) {
        handleError(e);
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
        int next =
            shuffle.advance(
                forward,
                repeatMode == Configuration.PLAYER_REPEAT_OFF,
                tracks.length,
                currentTrackIndex);
        if (next >= 0) {
          currentTrackIndex = next;
          trackChanged = true;
        }
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
        if (repeatMode == Configuration.PLAYER_REPEAT_OFF) {
          currentTrackIndex = tracks.length - 1;
          return false;
        }
        currentTrackIndex = 0;
      }
    } else {
      currentTrackIndex--;
      if (currentTrackIndex < 0) {
        if (repeatMode == Configuration.PLAYER_REPEAT_OFF) {
          currentTrackIndex = 0;
          return false;
        }
        currentTrackIndex = tracks.length - 1;
      }
    }

    return true;
  }

  private void createShuffle() {
    int size =
        trackList != null && trackList.getTracks() != null ? trackList.getTracks().length : 0;
    shuffle.rebuild(size, currentTrackIndex);
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
    parent.updateDisplayAsync();
    commandWorker.enqueue(CommandWorker.TRACK_END, sessionId);
  }

  void clearHandlingTrackEndFlag() {
    synchronized (this) {
      handlingTrackEnd = false;
    }
  }

  void processTrackEnd() {
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
    boolean reused = false;
    synchronized (this) {
      if (loading || destroyed) {
        return;
      }
      if (player != null) {
        try {
          player.setMediaTime(0L);
          cachedMediaTimeMicros = 0L;
          cachedSampleTimeMs = System.currentTimeMillis();
          cachedSessionId = playbackSessionId;
          if (player.getState() < Player.STARTED) {
            player.start();
          }
          reused = true;
        } catch (Exception e) {
          reused = false;
        }
      }
      if (!reused) {
        resetPlaybackStateLocked();
      }
    }

    if (reused) {
      startTimer();
      setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
      parent.updateDisplayAsync();
    } else {
      resetDisplay();
      play();
    }
  }

  public synchronized boolean canSkipForward() {
    if (trackList == null) {
      return false;
    }
    Track[] tracks = trackList.getTracks();
    if (tracks == null || tracks.length <= 1) {
      return false;
    }
    if (isShuffleEnabled) {
      return repeatMode != Configuration.PLAYER_REPEAT_OFF || shuffle.hasNext();
    }
    if (repeatMode != Configuration.PLAYER_REPEAT_OFF) {
      return true;
    }
    return currentTrackIndex < tracks.length - 1;
  }

  public synchronized boolean canSkipBackward() {
    if (trackList == null) {
      return false;
    }
    Track[] tracks = trackList.getTracks();
    if (tracks == null || tracks.length <= 1) {
      return false;
    }
    if (isShuffleEnabled) {
      return repeatMode != Configuration.PLAYER_REPEAT_OFF || shuffle.hasPrev();
    }
    if (repeatMode != Configuration.PLAYER_REPEAT_OFF) {
      return true;
    }
    return currentTrackIndex > 0;
  }

  private synchronized boolean hasNextTrackLocked() {
    if (trackList == null || trackList.getTracks() == null) {
      return false;
    }

    Track[] tracks = trackList.getTracks();
    if (tracks.length <= 1) {
      return false;
    }

    if (repeatMode == Configuration.PLAYER_REPEAT_ALL) {
      return true;
    }

    if (isShuffleEnabled) {
      return shuffle.hasNext();
    }

    return currentTrackIndex < tracks.length - 1;
  }

  VolumeControl getVolumeControl(Player targetPlayer) {
    if (targetPlayer == null) {
      return null;
    }
    try {
      return (VolumeControl) targetPlayer.getControl("VolumeControl");
    } catch (Exception e) {
      return null;
    }
  }

  private void startTimer() {
    synchronized (this) {
      if (destroyed || repaintSuppressed) {
        return;
      }
      ensureTimerLocked();
      if (displayTask == null) {
        displayTask =
            new TimerTask() {
              public void run() {
                try {
                  parent.onRepaintTick();
                } catch (Throwable t) {
                }
              }
            };
        mainTimer.scheduleAtFixedRate(displayTask, 0L, TIMER_INTERVAL);
      }
    }
  }

  void stopTimer() {
    synchronized (this) {
      stopTimerLocked();
    }
    parent.updateDisplayAsync();
  }

  public void pauseRepaintTimer() {
    synchronized (this) {
      repaintSuppressed = true;
      stopTimerLocked();
    }
  }

  public void resumeRepaintTimer() {
    synchronized (this) {
      repaintSuppressed = false;
      if (isPlaying()) {
        startTimer();
      }
    }
    parent.updateDisplayAsync();
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

  private void stopScheduledTasksLocked() {
    stopTimerLocked();
    cancelStartWatchdogLocked();
  }

  private void cancelStartWatchdogLocked() {
    if (startWatchdogTask != null) {
      startWatchdogTask.cancel();
      startWatchdogTask = null;
    }
  }

  void scheduleStartWatchdogTask(TimerTask task, long delayMs) {
    synchronized (this) {
      cancelStartWatchdogLocked();
      ensureTimerLocked();
      startWatchdogTask = task;
      mainTimer.schedule(task, delayMs);
    }
  }

  private void shutdownTimer() {
    synchronized (this) {
      stopScheduledTasksLocked();
      if (mainTimer != null) {
        mainTimer.cancel();
        mainTimer = null;
      }
    }
  }

  synchronized void resetPlaybackStateLocked() {
    invalidateSessionLocked();
    Thread pendingLoad = loadThread;
    if (pendingLoad != null) {
      pendingLoad.interrupt();
      loadThread = null;
    }
    loading = false;
    handlingTrackEnd = false;
    sessionUsedInputStream = false;
    sessionStarted = false;
    sessionMethodSessionId = -1;
    startWatchdogSessionId = -1;
    pendingResumeSeekMicros = -1;
    deferredResumeSeekMicros = -1;
    resetMediaCacheLocked();
    closePlayerLocked();
    closeResourcesLocked();
    stopScheduledTasksLocked();
  }

  private synchronized void resetCorePlaybackStateLocked() {
    resetPlaybackStateLocked();
    pendingTrackDelta = 0;
    commandWorker.clearQueue();
    pendingPlayerMethodOverride = null;
  }

  private void resetMediaCacheLocked() {
    cachedMediaTimeMicros = 0L;
    cachedDurationMicros = 0L;
    cachedSampleTimeMs = 0L;
    cachedSessionId = playbackSessionId;
  }

  private static int nextRepeatMode(int mode) {
    if (mode == Configuration.PLAYER_REPEAT_OFF) {
      return Configuration.PLAYER_REPEAT_ONE;
    }
    if (mode == Configuration.PLAYER_REPEAT_ONE) {
      return Configuration.PLAYER_REPEAT_ALL;
    }
    return Configuration.PLAYER_REPEAT_OFF;
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

  void applyPendingTrackChange() {
    int direction = pollPendingTrackDirectionLocked();
    if (direction == 0) {
      return;
    }

    changeTrack(direction > 0, false);
  }

  private void closePlayerLocked() {
    if (player != null) {
      Utils.closePlayerQuietly(player, this);
      player = null;
    }
  }

  private void closeResourcesLocked() {
    Utils.closeQuietly(inputStream);
    inputStream = null;
    Utils.closeQuietly(httpConnection);
    httpConnection = null;
  }

  private synchronized void invalidateSessionLocked() {
    playbackSessionId++;
  }

  private synchronized boolean isSessionActiveLocked(int sessionId) {
    return !destroyed && sessionId == playbackSessionId;
  }

  synchronized boolean isSessionActive(int sessionId) {
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

  Track getCurrentTrackLocked() {
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

  boolean isPlayerStarted(Player targetPlayer) {
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
    parent.painter.resetDurationText();
    parent.albumArtLoader.resetImage();
  }

  private void handleError(Exception e) {
    terminalPlaybackError(e == null ? "error" : e.toString());
  }

  private void terminalPlaybackError(String reason) {
    boolean advanced = false;
    synchronized (this) {
      if (!destroyed) {
        consecutivePlaybackErrors++;
        advanced =
            consecutivePlaybackErrors <= MAX_CONSECUTIVE_PLAYBACK_ERRORS && hasNextTrackLocked();
      }
    }

    if (advanced) {
      setStatusByKey(Configuration.PLAYER_STATUS_LOADING);
      changeTrack(true, true);
      return;
    }

    synchronized (this) {
      consecutivePlaybackErrors = 0;
      resetPlaybackStateLocked();
    }
    stopTimer();
    setStatus(Lang.tr("status.error"));
    parent.showError(reason);
  }

  void setStatus(String status) {
    parent.setStatus(status == null ? "" : status);
  }

  void setStatusByKey(String statusKey) {
    parent.setStatusByKey(statusKey);
  }

  public void saveVolumeLevel() {
    settingsPersistence.saveVolume(volumeLevel);
  }

  private void saveRepeatMode() {
    settingsPersistence.saveRepeat(repeatMode);
  }

  private void saveShuffleMode() {
    settingsPersistence.saveShuffle(isShuffleEnabled);
  }

  private PlaybackMethod getConfiguredPlayerHttpMethod() {
    String configuredMethod = settingsManager.getCurrentPlayerMethod();
    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(configuredMethod)
        || Configuration.PLAYER_METHOD_PASS_URL.equals(configuredMethod)) {
      return PlaybackMethod.fromCode(configuredMethod);
    }

    return PlaybackMethod.fromCode(settingsManager.getDefaultPlayerMethod());
  }

  private synchronized PlaybackMethod getNextPlayerHttpMethodLocked() {
    PlaybackMethod playerMethod = pendingPlayerMethodOverride;
    pendingPlayerMethodOverride = null;
    if (playerMethod == null) {
      playerMethod = getConfiguredPlayerHttpMethod();
    }
    return playerMethod;
  }
}
