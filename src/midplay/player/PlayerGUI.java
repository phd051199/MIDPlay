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
import javax.microedition.rms.RecordStoreException;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.util.Lang;
import midplay.util.Utils;

public class PlayerGUI implements PlayerListener {
  private static final int TIMER_INTERVAL = 1000;
  private static final int VOLUME_STEP = 10;
  private static final int MAX_PENDING_TRACK_STEPS = 20;
  // ponytail: widened from 250ms to 1200ms so the 1s repaint tick usually
  // extrapolates from the cached sample instead of native-sampling
  // getMediaTime/getDuration every second. Ceiling: extrapolation drifts <1s
  // between re-samples (invisible at tick resolution); a re-sample every ~2s
  // still corrects it and the monotonic guard prevents backward jumps.
  private static final long MEDIA_SAMPLE_WINDOW_MS = 1200L;

  private final PlayerScreen parent;
  private final SettingsManager settingsManager;
  final MediaResolver mediaResolver = new MediaResolver(this);
  private final PlaybackRecovery recovery = new PlaybackRecovery(this);
  private final CommandWorker commandWorker = new CommandWorker(this);
  Player player;
  private Tracks trackList;
  private int currentTrackIndex;
  int volumeLevel = Configuration.PLAYER_MAX_VOLUME;
  private int repeatMode = Configuration.PLAYER_REPEAT_ALL;
  private boolean isShuffleEnabled = false;
  private final ShuffleController shuffle = new ShuffleController();
  private HttpConnection httpConnection;
  private InputStream inputStream;
  // Resolved media URL + total content length of the currently attached playback,
  // captured at attach time so seek() can reload an InputStream player from a byte
  // offset (InputStream players can't setMediaTime). Null/-1 when not applicable
  // (the pass_url method has no live HttpConnection, so it seeks natively instead).
  private String currentResolvedUrl;
  private long currentContentLength = -1;
  // Resume position (micros) carried from a restored session: set before play()
  // and applied as a seek once playback actually starts (onPlaybackStarted). -1
  // when not resuming, so a normal play() doesn't seek. Seeking must wait until
  // the player is loaded/started — calling seek() before that is a no-op.
  private long pendingResumeSeekMicros = -1;
  private Timer mainTimer;
  private TimerTask displayTask;
  // True while the player canvas is hidden (hideNotify). startTimer() refuses
  // to schedule while suppressed so playback that starts in the background
  // (onPlaybackStarted while hidden) doesn't repaint a non-visible canvas.
  private boolean repaintSuppressed = false;

  // Background threads tracked so resetPlaybackStateLocked() can interrupt
  // them promptly on track change / cleanup (CLDC interrupt unblocks the
  // Thread.sleep waits used by the start watchdog and interruptibleSleep).
  private volatile Thread loadThread;
  // One-shot task on the shared player timer that promotes a silently-started
  // player or falls back to the alternate method if STARTED never fires. Held so
  // resetPlaybackStateLocked can cancel a pending watchdog on track change
  // instead of leaving it armed. Replaces a former per-start Thread+sleep.
  TimerTask startWatchdogTask;

  private volatile boolean loading = false;
  boolean destroyed = false;
  int playbackSessionId = 0;
  private boolean handlingTrackEnd = false;
  private int pendingTrackDelta = 0;
  PlaybackMethod pendingPlayerMethodOverride;
  boolean sessionUsedInputStream = false;
  boolean sessionStarted = false;
  int sessionMethodSessionId = -1;
  int startWatchdogSessionId = -1;
  int lastFallbackSessionId = -1;
  // Consecutive playback failures auto-advanced past. Once this hits the cap (or
  // there is no next track) a terminal error surfaces instead of spinning the
  // whole list. Reset to 0 on a successful start.
  private int consecutivePlaybackErrors = 0;
  private static final int MAX_CONSECUTIVE_PLAYBACK_ERRORS = 3;

  private final MediaTimeCache mediaTimeCache = new MediaTimeCache();

  public PlayerGUI(PlayerScreen parent) {
    this.parent = parent;
    this.settingsManager = SettingsManager.getInstance();
    loadSettings();
    commandWorker.start();
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
      commandWorker.start();
      resetCorePlaybackStateLocked();
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

  // Append tracks to the current queue without disturbing playback. The
  // currently-playing track and index are untouched; shuffle order is rebuilt so
  // the appended tracks participate (ponytail: rebuild re-randomizes the whole
  // order — per-item append that preserves already-heard continuity isn't worth
  // it until someone notices).
  public synchronized void addToQueue(Track[] toAdd) {
    if (toAdd == null || toAdd.length == 0) {
      return;
    }
    Track[] current = (trackList != null) ? trackList.getTracks() : null;
    int currentLen = (current != null) ? current.length : 0;
    Track[] merged = new Track[currentLen + toAdd.length];
    if (current != null) {
      System.arraycopy(current, 0, merged, 0, currentLen);
    }
    System.arraycopy(toAdd, 0, merged, currentLen, toAdd.length);
    if (trackList == null) {
      trackList = new Tracks();
    }
    trackList.setTracks(merged);
    if (isShuffleEnabled) {
      createShuffle();
    } else {
      shuffle.clear();
    }
  }

  // Adopt a reordered queue (manual sort). Playback keeps going on the same
  // physical track: we re-resolve currentTrackIndex to wherever that track now
  // lives in newOrder (by isSame), and rebuild shuffle if on.
  public synchronized void reorderQueue(Track[] newOrder) {
    if (trackList == null || newOrder == null || newOrder.length == 0) {
      return;
    }
    Track current = getCurrentTrackLocked();
    trackList.setTracks(newOrder);
    currentTrackIndex = 0;
    if (current != null) {
      for (int i = 0; i < newOrder.length; i++) {
        if (newOrder[i] != null && current.isSame(newOrder[i])) {
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
        // stop() on a non-realized player throws IllegalStateException; ignore.
      }
    }
    setStatusByKey(Configuration.PLAYER_STATUS_PAUSED);
    stopTimer();
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
    parent.showVolumeAlert();
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
    repeatMode = RepeatMode.next(repeatMode);
    saveRepeatMode();
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
  }

  // Stash a position to seek to once playback starts (resume). Harmless to call
  // with 0/-1: a non-positive value just clears any pending resume seek.
  void setPendingResumeSeek(long micros) {
    synchronized (this) {
      pendingResumeSeekMicros = micros;
    }
  }

  public void seek(long time) {
    if (time < 0) {
      return;
    }
    Player currentPlayer;
    synchronized (this) {
      if (destroyed) {
        return;
      }
      // An InputStream seek reloads the media (no native setMediaTime). If a reload
      // is already in flight, drop this seek instead of reseeding the display to a
      // target that won't load — otherwise holding/dragging to seek desyncs the
      // slider from the audio until the in-flight reload lands.
      if (sessionUsedInputStream && loading) {
        return;
      }
      currentPlayer = player;
      // Jump the display to the target immediately; the audio catches up below.
      mediaTimeCache.cachedMediaTimeMicros = time;
      mediaTimeCache.cachedSampleTimeMs = System.currentTimeMillis();
      mediaTimeCache.cachedSessionId = playbackSessionId;
    }
    parent.updateDisplayAsync();
    if (currentPlayer == null) {
      return;
    }

    // InputStream-based players can't setMediaTime (it's a silent no-op): reload
    // the media from the seek byte offset (HTTP Range) on a worker thread so it
    // actually plays from the seek point. The pass_url method seeks natively.
    if (sessionUsedInputStream && currentResolvedUrl != null && currentContentLength > 0) {
      startSeekReload(time);
      return;
    }
    try {
      currentPlayer.setMediaTime(time);
      parent.updateDisplayAsync();
    } catch (Exception e) {
      // setMediaTime() on a non-prefetched/unsupported player may throw; ignore.
    }
  }

  // Lightweight scrub preview: jumps the displayed playhead to `time` WITHOUT
  // touching the player (no setMediaTime, no InputStream reload), so dragging the
  // slider or holding a seek key tracks smoothly instead of lagging on a reload
  // per move. The real seek runs once via seek() when the interaction ends.
  public void seekPreview(long time) {
    if (time < 0) {
      time = 0;
    }
    synchronized (this) {
      if (destroyed) {
        return;
      }
      mediaTimeCache.cachedMediaTimeMicros = time;
      mediaTimeCache.cachedSampleTimeMs = System.currentTimeMillis();
      mediaTimeCache.cachedSessionId = playbackSessionId;
    }
    parent.updateDisplayAsync();
  }

  // Reload the current InputStream player from the byte offset for seekTimeMicros
  // so it actually plays from the seek point (setMediaTime is a silent no-op on
  // those players). Runs on a worker thread; createPlayer + prefetch block on the
  // network. byte = length * time / duration assumes CBR (off by ~1 MP3 frame;
  // the decoder resyncs to the next frame).
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
      // The rebuilt player only starts once the network reload lands, so stop the
      // old audio now — otherwise the pre-seek position keeps playing (and overlaps
      // the seek point) for the whole reload window on release. Not closed here:
      // attachPendingPlayback closes it on swap, and on reload failure it just stays
      // paused. A stopped player also freezes getCurrentTime() at the seek target, so
      // the slider holds instead of drifting during the reload.
      if (player != null) {
        try {
          int state = player.getState();
          if (state != Player.CLOSED && state >= Player.STARTED) {
            player.stop();
          }
        } catch (Exception e) {
          // stop() on an unsupported state throws IllegalStateException; ignore.
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
    MediaResolver.PendingPlayback pending = null;
    boolean attached = false;
    try {
      if (!isSessionActive(sessionId)) {
        return;
      }
      // Build the new partial-stream player FIRST while the old one keeps playing
      // (and the album art stays loaded), so the slider/art don't blank during the
      // reload. attachPendingPlayback closes the old player atomically at the swap.
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
      // Anchor the display at the seek target; the rebuilt player starts there.
      synchronized (this) {
        mediaTimeCache.cachedMediaTimeMicros = seekTimeMicros;
        mediaTimeCache.cachedSampleTimeMs = System.currentTimeMillis();
        mediaTimeCache.cachedSessionId = playbackSessionId;
      }
      // Repaint only — no full relayout (setupDisplay would reset layout + reload
      // album art, causing the seek glitch).
      parent.updateDisplayAsync();
      startPlayback();
    } catch (Exception e) {
      // If we already swapped to the new player, surface the error; otherwise the
      // old player is untouched, so abort the seek quietly and keep playing.
      if (attached && isSessionActive(sessionId)) {
        handleError(e);
      }
    } finally {
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

  // True when a Player instance is loaded (realized/prefetched/started). Used by
  // PlayerScreen to distinguish paused (player present, not playing) from
  // stopped (no player). player is nulled only by stop()/reset/cleanup.
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
      if (currentPlayer != null && mediaTimeCache.cachedSessionId == playbackSessionId) {
        mayEstimateFromCache = true;
        cachedTime = mediaTimeCache.cachedMediaTimeMicros;
        cachedDuration = mediaTimeCache.cachedDurationMicros;
        cachedAt = mediaTimeCache.cachedSampleTimeMs;
      }
    }
    if (currentPlayer == null) {
      return 0L;
    }

    long now = System.currentTimeMillis();
    boolean started = isPlayerStarted(currentPlayer);
    // A paused player (stopped but not nulled) has a frozen position, so the
    // cached sample stays exact indefinitely — trust it and never re-sample.
    // Re-sampling a stopped player is unsafe: some devices (KEmulator) return
    // getMediaTime()==0 once stopped, which collapses the progress fill to the
    // start. While playing, trust the cache only briefly and extrapolate.
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

    // Monotonic guard for the displayed time. getMediaTime() on HTTP/streaming
    // players can return coarse, stale, or momentarily-regressing values, which
    // would yank the slider and current-time label backward each 1s tick (the
    // visible "glitch"). While playing, never report less than the smooth
    // wall-clock extrapolation of the last trusted sample — the running time may
    // only advance. A deliberate seek reseeds the cache to its target, so this
    // floor follows seeks (forward and backward) instead of fighting them.
    if (started && mayEstimateFromCache && cachedAt > 0L) {
      long extrapolated = extrapolate(cachedTime, cachedAt, now, cachedDuration, started);
      if (sampledTime < extrapolated) {
        sampledTime = extrapolated;
      }
    }

    synchronized (this) {
      if (sessionId == playbackSessionId) {
        mediaTimeCache.cachedMediaTimeMicros = sampledTime;
        mediaTimeCache.cachedDurationMicros = sampledDuration;
        mediaTimeCache.cachedSampleTimeMs = now;
        mediaTimeCache.cachedSessionId = playbackSessionId;
      }
    }
    return sampledTime;
  }

  // Wall-clock extrapolation of a cached sample: cached value plus elapsed
  // time while playing, clamped to the known duration. Shared by the
  // cache-hit fast path and the streaming monotonic floor in getCurrentTime.
  private long extrapolate(
      long cachedTime, long cachedAt, long now, long cachedDuration, boolean started) {
    long estimated = cachedTime;
    if (started) {
      estimated += (now - cachedAt) * 1000L;
    }
    if (cachedDuration > 0L && estimated > cachedDuration) {
      estimated = cachedDuration;
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
      if (mediaTimeCache.cachedSessionId == playbackSessionId
          && mediaTimeCache.cachedDurationMicros > 0L) {
        return mediaTimeCache.cachedDurationMicros;
      }
    }
    if (currentPlayer == null) {
      return 0L;
    }

    long sampledDuration = getDurationSafe(currentPlayer);
    synchronized (this) {
      if (sampledDuration > 0L && sessionId == playbackSessionId) {
        mediaTimeCache.cachedDurationMicros = sampledDuration;
        mediaTimeCache.cachedSessionId = playbackSessionId;
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
        // ponytail: recovery exhausted — keep the queue flowing by advancing
        // past the dead track instead of stalling silently.
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
      mediaTimeCache.cachedSampleTimeMs = System.currentTimeMillis();
      mediaTimeCache.cachedSessionId = playbackSessionId;
      sessionId = playbackSessionId;
      resumeSeek = pendingResumeSeekMicros;
      pendingResumeSeekMicros = -1;
    }
    startTimer();
    setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
    commandWorker.enqueue(CommandWorker.APPLY_PENDING, sessionId);

    // Resume: seek to the saved in-track position once playback is live (the
    // player, resolved URL, content length, and duration are all populated by
    // now, so seek() works for both the pass_url and InputStream methods).
    if (resumeSeek > 0) {
      seek(resumeSeek);
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
    MediaResolver.PendingPlayback pending = null;

    try {
      Track track = getTrackForSession(sessionId);
      if (track == null || !isSessionActive(sessionId)) {
        return;
      }

      parent.setAlbumArtUrl(track.getImageUrl());

      // The inputstream method buffers the whole media file into the Java heap
      // during realize()/prefetch(). On low-heap devices the previous Player's
      // media plus decoded album art can exhaust memory before the new file is
      // fully read. Release them first; the pass_url path streams natively and
      // keeps the old Player alive, so it is left untouched.
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

  // Common recovery path for the load thread's catch arms: if the session is
  // still current, try one auto-recovery; otherwise surface the error. Each arm
  // only differs in the diagnostic label passed to the recovery logger.
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
    parent.freeImageHeap();
    System.gc();
  }

  private synchronized void attachPendingPlayback(
      MediaResolver.PendingPlayback pending, int sessionId) {
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

    // Capture the resolved URL + total size for seek-by-reload (InputStream method).
    currentResolvedUrl = null;
    currentContentLength = -1;
    if (httpConnection != null) {
      try {
        currentResolvedUrl = httpConnection.getURL();
      } catch (Exception e) {
      }
      try {
        currentContentLength = httpConnection.getLength();
      } catch (Exception e) {
      }
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
                forward, RepeatMode.isOff(repeatMode), tracks.length, currentTrackIndex);
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
        if (RepeatMode.isOff(repeatMode)) {
          currentTrackIndex = tracks.length - 1;
          return false;
        }
        currentTrackIndex = 0;
      }
    } else {
      currentTrackIndex--;
      if (currentTrackIndex < 0) {
        if (RepeatMode.isOff(repeatMode)) {
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
            RepeatMode.isOne(repeatMode)
                || (RepeatMode.isAll(repeatMode) && tracks != null && tracks.length == 1);

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
      // Repeat-one: rewind the already-loaded player instead of tearing it
      // down and re-downloading — a big win on slow links.
      if (player != null) {
        try {
          player.setMediaTime(0L);
          mediaTimeCache.cachedMediaTimeMicros = 0L;
          mediaTimeCache.cachedSampleTimeMs = System.currentTimeMillis();
          mediaTimeCache.cachedSessionId = playbackSessionId;
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

  private synchronized boolean hasNextTrackLocked() {
    if (trackList == null || trackList.getTracks() == null) {
      return false;
    }

    Track[] tracks = trackList.getTracks();
    if (tracks.length <= 1) {
      return false;
    }

    if (RepeatMode.isAll(repeatMode)) {
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

  private VolumeControl getVolumeControl() {
    Player currentPlayer;
    synchronized (this) {
      currentPlayer = player;
    }
    return getVolumeControl(currentPlayer);
  }

  private void startTimer() {
    synchronized (this) {
      if (repaintSuppressed) {
        return;
      }
      ensureTimerLocked();
      if (displayTask == null) {
        displayTask =
            new TimerTask() {
              public void run() {
                // Guard the shared timer thread: an uncaught throw would kill it
                // and silently stop the per-second repaint (and any watchdog).
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

  // Pause/resume ONLY the 1s repaint timer, independent of audio playback.
  // Used by PlayerScreen.hideNotify/showNotify so a hidden canvas stops
  // repainting (saving CPU/battery) while audio keeps playing (Decision A).
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
      // MIDP's Timer only exposes the no-arg ctor (no daemon threads in CLDC),
      // so it's finally cancelled by shutdownTimer() on app destroy. Kept alive
      // across track changes so we don't spawn a fresh thread on every next/prev.
      mainTimer = new Timer();
    }
  }

  private void stopTimerLocked() {
    if (displayTask != null) {
      displayTask.cancel();
      displayTask = null;
    }
  }

  // Cancel scheduled tasks (repaint + start watchdog) but keep the shared daemon
  // timer alive so the next track doesn't pay for a new thread. The timer itself
  // is finally cancelled only by shutdownTimer() on app destroy.
  private void stopScheduledTasksLocked() {
    stopTimerLocked();
    cancelStartWatchdogLocked();
  }

  // Cancel a pending start-watchdog task (track change / cleanup). Replaces the
  // former thread.interrupt() on a dedicated watchdog thread.
  private void cancelStartWatchdogLocked() {
    if (startWatchdogTask != null) {
      startWatchdogTask.cancel();
      startWatchdogTask = null;
    }
  }

  // Schedule a one-shot start-watchdog on the shared player timer, superseding
  // any pending one. Replaces a former dedicated Thread per playback start.
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
    resetMediaCacheLocked();
    closePlayerLocked();
    closeResourcesLocked();
    stopScheduledTasksLocked();
  }

  // Shared teardown of in-flight playback state for setPlaylist/stop/cleanup:
  // session + player/resources, the pending track-change queue, and the pending
  // method override. Each caller sets its own flags (destroyed/loading/shuffle)
  // around it.
  private synchronized void resetCorePlaybackStateLocked() {
    resetPlaybackStateLocked();
    clearPendingTrackChangesLocked();
    commandWorker.clearQueue();
    clearPendingPlayerMethodOverrideLocked();
  }

  private synchronized void clearPendingTrackChangesLocked() {
    pendingTrackDelta = 0;
  }

  private synchronized void clearPendingPlayerMethodOverrideLocked() {
    pendingPlayerMethodOverride = null;
  }

  private void resetMediaCacheLocked() {
    mediaTimeCache.reset(playbackSessionId);
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

  synchronized boolean isCurrentSession(int sessionId) {
    return sessionId == playbackSessionId;
  }

  private void closePlayerLocked() {
    if (player != null) {
      closePlayerQuietly(player);
      player = null;
    }
  }

  private void closeResourcesLocked() {
    Utils.closeQuietly(inputStream);
    inputStream = null;
    Utils.closeQuietly(httpConnection);
    httpConnection = null;
  }

  void closePlayerQuietly(Player targetPlayer) {
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

  void closeHttpResources(InputStream stream, HttpConnection connection) {
    Utils.closeQuietly(stream);
    Utils.closeQuietly(connection);
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
    parent.resetDurationText();
    parent.resetImage();
  }

  private void handleError(Exception e) {
    terminalPlaybackError(e == null ? "error" : e.toString());
  }

  // Last-resort fallback for a track that exhausted every in-track recovery
  // (resolution/inputstream retries + inputstream<->url method swap): keep
  // playback flowing by advancing to the next track instead of dead-stopping the
  // queue. After MAX_CONSECUTIVE_PLAYBACK_ERRORS in a row, or when there is no
  // next track, give up and surface the error. changeTrack(true, true) advances
  // immediately when idle, or queues the skip onto the pending-delta path while a
  // load is still unwinding, so this is safe from the load thread, the start
  // path, and the PlayerListener callback thread alike.
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

    consecutivePlaybackErrors = 0;
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

  private PlaybackMethod getConfiguredPlayerHttpMethod() {
    String configuredMethod = settingsManager.getCurrentPlayerMethod();
    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(configuredMethod)
        || Configuration.PLAYER_METHOD_PASS_URL.equals(configuredMethod)) {
      return PlaybackMethod.fromCode(configuredMethod);
    }

    return PlaybackMethod.fromCode(Utils.getPlayerHttpMethod());
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
