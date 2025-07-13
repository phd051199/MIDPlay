package app.ui.player;

import app.core.network.RestClient;
import app.core.platform.PlayerMethod;
import app.core.settings.PlayerSettingsManager;
import app.core.threading.ThreadManagerIntegration;
import app.models.Song;
import app.utils.I18N;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class PlayerGUI implements PlayerListener {

  public static final int REPEAT_OFF = 0;
  public static final int REPEAT_ONE = 1;
  public static final int REPEAT_ALL = 2;

  private static String prevStatus = "";
  private int currentVolumeLevel = 100;

  private int repeatMode = REPEAT_ALL;
  private boolean shuffleMode = false;
  private int[] shuffleIndices = null;
  private int currentShufflePosition = 0;
  private final Random random = new Random();
  private boolean[] shufflePlayedIndices = null;
  private int shufflePlayedCount = 0;

  private final int timerInterval = 1000;
  private Timer guiTimer = null;
  private TimerTask timeDisplayTask = null;
  private TimerTask volumeStatusTask = null;
  private final PlayerCanvas parent;
  private Player player = null;
  private Vector listSong = null;
  private int index = 0;
  private boolean restartOnResume = false;
  private boolean isTransitioning = false;

  private final PlayerSettingsManager settingsManager;

  private int playerHttpMethod;

  private HttpConnection httpConn = null;
  private InputStream inputStream = null;
  private OutputStream outputStream = null;
  private String playUrl;

  private volatile boolean playerTaskRunning = false;
  private volatile boolean endOfMediaTaskRunning = false;

  public PlayerGUI(PlayerCanvas parent) {
    this.parent = parent;
    this.settingsManager = PlayerSettingsManager.getInstance();

    this.loadPlayerConfigs();
    this.setStatus("");
  }

  private void loadPlayerConfigs() {
    this.currentVolumeLevel = settingsManager.getVolumeLevel();
    this.repeatMode = settingsManager.getRepeatMode();
    this.shuffleMode = settingsManager.getShuffleMode();
  }

  private void savePlayerConfigs() {
    settingsManager.savePlayerSettings(currentVolumeLevel, repeatMode, shuffleMode);
  }

  public void setListSong(Vector lst, int index) {
    this.index = index;
    this.listSong = lst;
    this.closePlayer();
    this.setStatus("");
    this.parent.updateDisplay();

    if (this.shuffleMode) {
      generateShuffleIndices();
      updateShufflePosition();
    }
  }

  private void generateShuffleIndices() {
    if (listSong == null || listSong.isEmpty()) {
      return;
    }

    int size = listSong.size();
    shuffleIndices = new int[size];
    shufflePlayedIndices = new boolean[size];
    shufflePlayedCount = 0;

    for (int i = 0; i < size; i++) {
      shuffleIndices[i] = i;
      shufflePlayedIndices[i] = false;
    }

    for (int i = size - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);

      int temp = shuffleIndices[i];
      shuffleIndices[i] = shuffleIndices[j];
      shuffleIndices[j] = temp;
    }
  }

  private void updateShufflePosition() {
    if (shuffleIndices == null || listSong == null || listSong.isEmpty()) {
      return;
    }

    for (int i = 0; i < shuffleIndices.length; i++) {
      if (shuffleIndices[i] == index) {
        currentShufflePosition = i;
        return;
      }
    }

    currentShufflePosition = 0;
    index = shuffleIndices[0];
  }

  public int getRepeatMode() {
    return this.repeatMode;
  }

  public void setRepeatMode(int mode) {
    this.repeatMode = mode;
    savePlayerConfigs();
  }

  public void toggleRepeatMode() {
    this.repeatMode = (this.repeatMode + 1) % 3;
    this.parent.updateRepeatCommand();
    savePlayerConfigs();
  }

  public boolean getShuffleMode() {
    return this.shuffleMode;
  }

  public void setShuffleMode(boolean mode) {
    this.shuffleMode = mode;
    savePlayerConfigs();
  }

  public void toggleShuffleMode() {
    this.shuffleMode = !this.shuffleMode;
    this.parent.updateShuffleCommand();

    if (this.shuffleMode && this.listSong != null && this.listSong.size() > 0) {
      generateShuffleIndices();
      updateShufflePosition();

      if (shufflePlayedIndices != null
          && currentShufflePosition >= 0
          && currentShufflePosition < shufflePlayedIndices.length) {
        shufflePlayedIndices[currentShufflePosition] = true;
        shufflePlayedCount++;
      }
    }

    savePlayerConfigs();
  }

  public Vector getListSong() {
    return this.listSong;
  }

  public int getCurrentIndex() {
    return this.index;
  }

  public boolean getIsPlaying() {
    return this.player != null && this.player.getState() >= 400;
  }

  public long getDuration() {
    if (this.listSong == null || this.index < 0 || this.index >= this.listSong.size()) {
      return 0L;
    }

    Song s = (Song) this.listSong.elementAt(this.index);
    if (s == null) {
      return 0L;
    }

    try {
      if (player != null && s.getDuration() <= 0) {
        long duration = player.getDuration();
        if (duration > 0) {
          return duration;
        }
      }
    } catch (IllegalStateException e) {

    }

    return (long) s.getDuration() * 1000000L;
  }

  public long getCurrentTime() {
    try {
      if (this.player != null) {
        return this.player.getMediaTime();
      }
    } catch (IllegalStateException e) {

    }

    return 0L;
  }

  public String getSongName() {
    if (this.listSong != null && this.index >= 0 && this.index < this.listSong.size()) {
      Song s = (Song) this.listSong.elementAt(this.index);
      return s != null ? s.getSongName() : "";
    }
    return "";
  }

  public String getSinger() {
    if (this.listSong != null && this.index >= 0 && this.index < this.listSong.size()) {
      Song s = (Song) this.listSong.elementAt(this.index);
      return s != null ? s.getArtistName() : "";
    }
    return "";
  }

  private void setStatus(String s) {
    this.parent.setStatus(s);
  }

  public Song getCurrentSong() {
    if (listSong != null && index >= 0 && index < listSong.size()) {
      return (Song) listSong.elementAt(index);
    }
    return null;
  }

  private VolumeControl getVolumeControl() {
    try {
      if (this.player != null) {
        return (VolumeControl) this.player.getControl("VolumeControl");
      }
    } catch (IllegalStateException e) {

    }

    return null;
  }

  private void assertPlayer() throws Throwable {
    this.setStatus(I18N.tr("loading"));

    this.playerHttpMethod = PlayerMethod.getPlayerHttpMethod();
    Song s = (Song) this.listSong.elementAt(this.index);

    try {
      this.parent.setAlbumArtUrl(s.getImage());

      switch (this.playerHttpMethod) {
        case PlayerMethod.PASS_CONNECTION_STREAM:
          createPlayerFromStream(s);
          break;
        case PlayerMethod.PASS_URL:
          createPlayerFromUrl(s);
          break;
      }

      this.player.addPlayerListener(this);
      this.player.realize();
      this.player.prefetch();
      this.parent.setupDisplay();

      VolumeControl vc = this.getVolumeControl();
      if (vc != null) {
        vc.setLevel(this.currentVolumeLevel);
      }
    } catch (Throwable error) {
      this.player = null;
      this.setStatus(error.toString());
      closeResources();
    }
  }

  private void createPlayerFromStream(Song s) throws IOException {
    this.httpConn = RestClient.getInstance().getStreamConnection(s.getStreamUrl());
    this.inputStream = this.httpConn.openInputStream();

    try {
      this.player = Manager.createPlayer(this.inputStream, "audio/mpeg");
    } catch (MediaException me) {
      throw new IOException(me.toString());
    }
  }

  private void createPlayerFromUrl(Song s) throws IOException {
    this.playUrl = s.getStreamUrl();
    try {
      this.player = Manager.createPlayer(this.playUrl);
    } catch (MediaException me) {
      throw new IOException(me.toString());
    }
  }

  public synchronized void changeSong(boolean next) throws Throwable {
    if (isTransitioning) {
      return;
    }

    try {
      isTransitioning = true;

      playerTaskRunning = false;
      endOfMediaTaskRunning = false;

      if (shuffleMode) {
        if (next) {
          if (shufflePlayedCount >= shuffleIndices.length && repeatMode == REPEAT_OFF) {
            currentShufflePosition = shuffleIndices.length - 1;
            return;
          }
          getNextShuffleSong();
        } else {
          currentShufflePosition--;
          if (currentShufflePosition < 0) {
            currentShufflePosition = shuffleIndices.length - 1;
          }
          index = shuffleIndices[currentShufflePosition];
        }
      } else {
        if (next) {
          if (++this.index >= this.listSong.size()) {
            if (repeatMode == REPEAT_OFF && this.listSong.size() > 0) {
              this.index = this.listSong.size() - 1;
              return;
            }
            this.index = 0;
          }
        } else {
          if (--this.index < 0) {
            this.index = this.listSong.size() - 1;
          }
        }
      }

      this.closePlayer();
      this.startPlayer();
      this.setStatus(I18N.tr("playing"));
    } finally {
      isTransitioning = false;
    }
  }

  public void getNextSong() throws Throwable {
    changeSong(true);
  }

  public void getPrevSong() throws Throwable {
    changeSong(false);
  }

  public void startPlayer() {
    if (playerTaskRunning) {
      return;
    }

    playerTaskRunning = true;
    ThreadManagerIntegration.executePlayerTask(
        new Runnable() {
          public void run() {
            try {
              if (!playerTaskRunning) {
                return;
              }
              if (PlayerGUI.this.player == null || PlayerGUI.this.player.getState() < 300) {
                PlayerGUI.this.assertPlayer();
              }
              if (PlayerGUI.this.player == null || PlayerGUI.this.player.getState() >= 400) {
                return;
              }
              try {
                long duration = PlayerGUI.this.getDuration();
                if (duration != -1L && PlayerGUI.this.player.getMediaTime() >= duration) {
                  PlayerGUI.this.player.setMediaTime(0L);
                }
              } catch (MediaException var3) {

              }
              PlayerGUI.this.player.start();
              if (PlayerGUI.this.player.getState() >= 400) {
                PlayerGUI.this.setStatus(I18N.tr("playing"));
              }
            } catch (Throwable var4) {

            } finally {
              playerTaskRunning = false;
            }
          }
        },
        "PlayerStart");
  }

  private void closeResources() {
    if (this.inputStream != null) {
      try {
        this.inputStream.close();
      } catch (IOException e) {

      }
      this.inputStream = null;
    }

    if (this.outputStream != null) {
      try {
        this.outputStream.close();
      } catch (IOException e) {

      }
      this.outputStream = null;
    }

    if (this.httpConn != null) {
      try {
        this.httpConn.close();
      } catch (IOException e) {

      }
      this.httpConn = null;
    }
  }

  public void closePlayer() {
    this.stopDisplayTimer();

    playerTaskRunning = false;
    endOfMediaTaskRunning = false;

    ThreadManagerIntegration.clearPlayerQueues();

    if (this.player != null) {
      try {
        if (this.player.getState() != Player.CLOSED) {
          this.player.removePlayerListener(this);

          if (this.player.getState() != Player.CLOSED) {
            try {
              this.player.stop();
            } catch (MediaException e) {

            }
          }

          this.player.close();
        }
      } catch (Exception e) {

      } finally {
        this.player = null;
      }
      this.setStatus("");
    }

    closeResources();

    if (this.guiTimer != null) {
      this.guiTimer.cancel();
      this.guiTimer = null;
    }
  }

  public void pausePlayer() {
    if (this.player != null) {
      try {
        this.player.stop();
      } catch (Exception e) {

      }
      this.setStatus(I18N.tr("paused"));
    }
  }

  public void togglePlayer() {
    if (this.player != null) {
      if (this.player.getState() >= 400) {
        this.pausePlayer();
      } else {
        this.startPlayer();
      }
    }
  }

  public void setMediaTime(long time) {
    if (this.player != null) {
      try {
        this.player.setMediaTime(time);
        this.parent.updateDisplay();
      } catch (Exception e) {

      }
    }
  }

  public void changeVolume(boolean decrease) {
    VolumeControl vc = this.getVolumeControl();
    if (vc == null) {
      return;
    }

    if (!this.parent.getStatus().startsWith(I18N.tr("volume"))) {
      prevStatus = this.parent.getStatus();
    }

    if (decrease) {
      currentVolumeLevel = Math.max(0, currentVolumeLevel - 10);
    } else {
      currentVolumeLevel = Math.min(100, currentVolumeLevel + 10);
    }

    vc.setLevel(currentVolumeLevel);
    this.setVolumeStatus(currentVolumeLevel);

    savePlayerConfigs();
  }

  private void setVolumeStatus(int cv) {
    this.parent.setStatus(I18N.tr("volume") + ": " + cv);

    if (volumeStatusTask != null) {
      volumeStatusTask.cancel();
    }

    if (this.guiTimer == null) {
      this.guiTimer = new Timer();
    }

    volumeStatusTask =
        new TimerTask() {
          public void run() {
            parent.setStatus(prevStatus);
          }
        };

    this.guiTimer.schedule(volumeStatusTask, 1000);
  }

  private synchronized void startDisplayTimer() {
    if (this.guiTimer == null) {
      this.guiTimer = new Timer();
    }
    if (this.timeDisplayTask == null) {
      this.timeDisplayTask = new SPTimerTask();
      this.guiTimer.scheduleAtFixedRate(this.timeDisplayTask, 0L, (long) this.timerInterval);
    }
  }

  private synchronized void stopDisplayTimer() {
    if (this.timeDisplayTask != null) {
      this.timeDisplayTask.cancel();
      this.timeDisplayTask = null;
      this.parent.updateDisplay();
    }
  }

  public void playerUpdate(Player plyr, String evt, Object evtData) {
    try {
      if (this.player != plyr) {
        return;
      }

      if ("closed".equals(evt) || "error".equals(evt) || "stopped".equals(evt)) {
        this.stopDisplayTimer();
        this.parent.updateDisplay();
      } else if ("endOfMedia".equals(evt)) {
        handleEndOfMedia(plyr);
      } else if ("started".equals(evt)) {
        this.startDisplayTimer();
      } else if ("durationUpdated".equals(evt)) {
        this.parent.updateDisplay();
      }
    } catch (Throwable t) {

    }
  }

  private void handleEndOfMedia(Player plyr) {
    if (plyr.getState() != Player.PREFETCHED && plyr.getState() != Player.STARTED) {
      return;
    }

    this.stopDisplayTimer();
    this.parent.updateDisplay();

    if (isTransitioning) {
      return;
    }

    if (endOfMediaTaskRunning) {
      return;
    }

    isTransitioning = true;
    endOfMediaTaskRunning = true;

    try {
      ThreadManagerIntegration.executeEndOfMediaHandling(
          new Runnable() {
            public void run() {
              try {
                if (!endOfMediaTaskRunning) {
                  return;
                }
                switch (repeatMode) {
                  case REPEAT_ONE:
                    closePlayer();
                    startPlayer();
                    break;
                  case REPEAT_ALL:
                    getNextSong();
                    break;
                  case REPEAT_OFF:
                  default:
                    if (shuffleMode) {
                      if (shufflePlayedCount >= shuffleIndices.length) {
                        closePlayer();
                        setStatus("");
                      } else {
                        getNextShuffleSong();
                        closePlayer();
                        startPlayer();
                      }
                    } else if (index < listSong.size() - 1) {
                      getNextSong();
                    } else {
                      closePlayer();
                      setStatus("");
                    }
                    break;
                }
              } catch (Throwable t) {
              } finally {
                endOfMediaTaskRunning = false;
              }
            }
          });
    } finally {
      isTransitioning = false;
    }
  }

  private void getNextShuffleSong() {
    if (shuffleIndices == null || shufflePlayedIndices == null) {
      return;
    }

    if (currentShufflePosition >= 0 && currentShufflePosition < shufflePlayedIndices.length) {
      shufflePlayedIndices[currentShufflePosition] = true;
      shufflePlayedCount++;
    }

    if (shufflePlayedCount < shuffleIndices.length) {
      int nextPosition = (currentShufflePosition + 1) % shuffleIndices.length;
      while (shufflePlayedIndices[nextPosition]) {
        nextPosition = (nextPosition + 1) % shuffleIndices.length;
      }
      currentShufflePosition = nextPosition;
      index = shuffleIndices[currentShufflePosition];
    }

    if (shufflePlayedCount >= shuffleIndices.length && repeatMode != REPEAT_OFF) {
      for (int i = 0; i < shufflePlayedIndices.length; i++) {
        shufflePlayedIndices[i] = false;
      }
      shufflePlayedCount = 0;

      shufflePlayedIndices[currentShufflePosition] = true;
      shufflePlayedCount = 1;
    }
  }

  public synchronized void pauseApp() {
    if (this.player != null && this.player.getState() >= 400) {
      try {
        this.player.stop();
      } catch (MediaException e) {

      }

      this.stopDisplayTimer();
      this.restartOnResume = true;
    } else {
      this.restartOnResume = false;
    }
  }

  public synchronized void resumeApp() {
    if (this.player != null && this.restartOnResume) {
      try {
        this.player.start();
      } catch (MediaException e) {

      }
    }
    this.restartOnResume = false;
  }

  public void shutdown() {
    playerTaskRunning = false;
    endOfMediaTaskRunning = false;

    if (settingsManager != null) {
      settingsManager.shutdown();
    }
  }

  private class SPTimerTask extends TimerTask {
    private SPTimerTask() {}

    public void run() {
      PlayerGUI.this.parent.updateDisplay();
    }
  }
}
