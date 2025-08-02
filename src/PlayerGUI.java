import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.io.Connector;
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
  private volatile boolean isLoading = false;

  public PlayerGUI(PlayerScreen parent) {
    this.parent = parent;
    this.settingsManager = SettingsManager.getInstance();
    loadSettings();
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

  public void setPlaylist(Tracks tracks, int startIndex) {
    if (tracks == null || tracks.getTracks() == null || startIndex < 0) {
      return;
    }
    cleanup();
    this.trackList = tracks;
    this.currentTrackIndex = Math.min(startIndex, tracks.getTracks().length - 1);
    if (isShuffleEnabled) {
      createShuffle();
    }
    setStatusByKey(Configuration.PLAYER_STATUS_READY);
    parent.updateDisplay();
  }

  public void play() {
    if (getCurrentTrack() == null) {
      return;
    }
    if (isLoading) {
      isLoading = false;
      closePlayer();
      closeResources();
    }
    if (player != null && player.getState() >= Player.PREFETCHED) {
      startPlayback();
    } else {
      loadAndPlay();
    }
  }

  public void pause() {
    if (player != null) {
      try {
        player.stop();
        setStatusByKey(Configuration.PLAYER_STATUS_PAUSED);
        stopTimer();
      } catch (MediaException e) {
        e.printStackTrace();
      }
    }
  }

  public void toggle() {
    if (isPlaying()) {
      pause();
    } else {
      play();
    }
  }

  public void next() {
    changeTrack(true);
  }

  public void previous() {
    changeTrack(false);
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
    if (isShuffleEnabled && trackList != null) {
      createShuffle();
    }
    saveShuffleMode();
  }

  public void seek(long time) {
    if (player != null && time >= 0) {
      try {
        player.setMediaTime(time);
        parent.updateDisplay();
      } catch (MediaException e) {
        e.printStackTrace();
      }
    }
  }

  public synchronized void cleanup() {
    isLoading = false;
    closePlayer();
    closeResources();
    cleanupTimers();
    shuffleOrder = null;
    shufflePosition = 0;
    setStatus("");
  }

  public boolean isPlaying() {
    return player != null && player.getState() >= Player.STARTED;
  }

  public Track getCurrentTrack() {
    if (trackList == null || trackList.getTracks() == null) {
      return null;
    }
    Track[] tracks = trackList.getTracks();
    if (currentTrackIndex >= 0 && currentTrackIndex < tracks.length) {
      return tracks[currentTrackIndex];
    }
    return null;
  }

  public Tracks getCurrentTracks() {
    return trackList;
  }

  public long getCurrentTime() {
    if (player != null) {
      try {
        return player.getMediaTime();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return 0L;
  }

  public long getDuration() {
    Track track = getCurrentTrack();
    if (track == null) {
      return 0L;
    }
    long duration = ((long) track.getDuration()) * 1000000L;
    if (duration <= 0) {
      try {
        long playerDuration = player.getDuration();
        if (playerDuration > 0) {
          duration = playerDuration;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return duration;
  }

  public int getRepeatMode() {
    return repeatMode;
  }

  public boolean isShuffleEnabled() {
    return isShuffleEnabled;
  }

  public int getCurrentIndex() {
    return currentTrackIndex;
  }

  public Tracks getPlaylist() {
    return trackList;
  }

  public void playerUpdate(Player p, String event, Object eventData) {
    if (player != p) {
      return;
    }
    try {
      if (PlayerListener.STARTED.equals(event)) {
        startTimer();
        setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
      } else if (PlayerListener.END_OF_MEDIA.equals(event)) {
        handleTrackEnd();
      } else if (PlayerListener.STOPPED.equals(event)
          || PlayerListener.CLOSED.equals(event)
          || PlayerListener.ERROR.equals(event)) {
        stopTimer();
        parent.updateDisplay();
      } else if (PlayerListener.DURATION_UPDATED.equals(event)) {
        parent.updateDisplay();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadAndPlay() {
    if (isLoading) {
      return;
    }
    isLoading = true;
    setStatusByKey(Configuration.PLAYER_STATUS_LOADING);
    Thread loadThread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  if (!isLoading) {
                    return;
                  }
                  setupPlayer();
                  if (!isLoading) {
                    closePlayer();
                    closeResources();
                    return;
                  }
                  startPlayback();
                } catch (IOException e) {
                  e.printStackTrace();
                } catch (MediaException me) {
                  if (isLoading) {
                    handleError(me);
                  }
                } finally {
                  isLoading = false;
                }
              }
            });
    loadThread.start();
  }

  private void setupPlayer() throws IOException, MediaException {
    Track track = getCurrentTrack();
    if (track == null) {
      throw new IOException("No track selected");
    }
    if (!isLoading) {
      return;
    }
    closePlayer();
    closeResources();
    parent.setAlbumArtUrl(track.getImageUrl());

    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(getPlayerHttpMethod())) {
      createStreamPlayer(track);
    } else {
      createUrlPlayer(track);
    }

    if (!isLoading || player == null) {
      closePlayer();
      closeResources();
      return;
    }

    player.addPlayerListener(this);
    player.realize();

    if (!isLoading) {
      closePlayer();
      closeResources();
      return;
    }

    player.prefetch();
    VolumeControl vc = getVolumeControl();
    if (vc != null) {
      vc.setLevel(volumeLevel);
    }
    parent.setupDisplay();
  }

  private void createStreamPlayer(Track track) throws IOException, MediaException {
    if (!isLoading) {
      return;
    }
    String finalUrl = resolveRedirect(track.getUrl());
    if (!isLoading) {
      return;
    }
    httpConnection = (HttpConnection) Connector.open(finalUrl);
    if (!isLoading) {
      closeResources();
      return;
    }
    inputStream = httpConnection.openInputStream();
    if (!isLoading) {
      closeResources();
      return;
    }
    player = Manager.createPlayer(inputStream, "audio/mpeg");
  }

  private void createUrlPlayer(Track track) throws IOException, MediaException {
    if (!isLoading) {
      return;
    }
    String finalUrl = resolveRedirect(track.getUrl());
    if (!isLoading) {
      return;
    }
    player = Manager.createPlayer(finalUrl);
  }

  private String resolveRedirect(String url) throws IOException {
    if (url.indexOf(".mp3") != -1) {
      return url;
    }
    HttpConnection connection = null;
    try {
      connection = (HttpConnection) Connector.open(url);
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpConnection.HTTP_MOVED_PERM
          || responseCode == HttpConnection.HTTP_MOVED_TEMP) {
        String redirectUrl = connection.getHeaderField("Location");
        if (redirectUrl != null) {
          return redirectUrl;
        }
      }
      return url;
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private void startPlayback() {
    if (player == null) {
      return;
    }
    try {
      long duration = getDuration();
      if (duration > 0 && getCurrentTime() >= duration) {
        player.setMediaTime(0L);
      }
      if (player.getState() < Player.STARTED) {
        player.start();
      }
    } catch (MediaException me) {
      handleError(me);
    }
  }

  private void changeTrack(boolean forward) {
    if (trackList == null || trackList.getTracks() == null) {
      return;
    }
    Track[] tracks = trackList.getTracks();
    if (tracks.length == 0) {
      return;
    }
    isLoading = false;
    closePlayer();
    closeResources();
    boolean trackChanged;
    if (isShuffleEnabled) {
      trackChanged = changeTrackShuffle(forward);
    } else {
      trackChanged = changeTrackNormal(forward);
    }
    if (trackChanged) {
      play();
    }
  }

  private boolean changeTrackNormal(boolean forward) {
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

  private boolean changeTrackShuffle(boolean forward) {
    if (shuffleOrder == null) {
      createShuffle();
      if (shuffleOrder == null) {
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
        shufflePosition = 0;
        createShuffle();
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

  private void createShuffle() {
    if (trackList == null || trackList.getTracks() == null) {
      return;
    }
    Track[] tracks = trackList.getTracks();
    int size = tracks.length;
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
    for (int i = 0; i < size; i++) {
      if (shuffleOrder[i] == currentTrackIndex) {
        shufflePosition = i;
        break;
      }
    }
  }

  private void handleTrackEnd() {
    stopTimer();
    parent.updateDisplay();
    Thread endThread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  Track[] tracks = trackList != null ? trackList.getTracks() : null;
                  if (repeatMode == Configuration.PLAYER_REPEAT_ONE
                      || (repeatMode == Configuration.PLAYER_REPEAT_ALL
                          && tracks != null
                          && tracks.length == 1)) {
                    if (Configuration.PLAYER_METHOD_PASS_URL.equals(getPlayerHttpMethod())) {
                      closePlayer();
                    }
                    play();
                  } else if (hasNextTrack()) {
                    next();
                  } else {
                    setStatusByKey(Configuration.PLAYER_STATUS_FINISHED);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
    endThread.start();
  }

  private boolean hasNextTrack() {
    Track[] tracks = trackList.getTracks();
    if (trackList == null || tracks == null || tracks.length <= 1) {
      return false;
    }

    if (Configuration.PLAYER_REPEAT_ALL == repeatMode) {
      return true;
    }
    if (isShuffleEnabled) {
      if (shuffleOrder == null) {
        return false;
      }
      return shufflePosition < shuffleOrder.length - 1;
    } else {
      return currentTrackIndex < tracks.length - 1;
    }
  }

  private VolumeControl getVolumeControl() {
    if (player != null) {
      try {
        return (VolumeControl) player.getControl("VolumeControl");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private void startTimer() {
    ensureTimer();
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

  private void stopTimer() {
    if (displayTask != null) {
      displayTask.cancel();
      displayTask = null;
    }
    parent.updateDisplay();
  }

  private void ensureTimer() {
    if (mainTimer == null) {
      mainTimer = new Timer();
    }
  }

  private void cleanupTimers() {
    if (displayTask != null) {
      displayTask.cancel();
      displayTask = null;
    }
    if (mainTimer != null) {
      mainTimer.cancel();
      mainTimer = null;
    }
  }

  private void closePlayer() {
    if (player != null) {
      try {
        player.removePlayerListener(this);
        if (player.getState() != Player.CLOSED) {
          player.stop();
          player.close();
        }
      } catch (MediaException e) {
        e.printStackTrace();
      }
      player = null;
    }
  }

  private void closeResources() {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      inputStream = null;
    }
    if (httpConnection != null) {
      try {
        httpConnection.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      httpConnection = null;
    }
  }

  private void handleError(MediaException me) {
    player = null;
    closeResources();
    setStatus(Lang.tr("status.error"));
    parent.showError(me.toString());
  }

  private void setStatus(String status) {
    parent.setStatus(status);
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

  private String getPlayerHttpMethod() {
    if (Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(
        settingsManager.getCurrentPlayerMethod())) {
      return Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    } else {
      return Utils.getPlayerHttpMethod();
    }
  }

  public void pauseApp() {
    if (isPlaying()) {
      try {
        player.stop();
        stopTimer();
      } catch (MediaException e) {
        e.printStackTrace();
      }
    }
  }

  public void resumeApp() {
    if (player != null && player.getState() == Player.PREFETCHED) {
      try {
        player.start();
      } catch (MediaException e) {
        e.printStackTrace();
      }
    }
  }
}
