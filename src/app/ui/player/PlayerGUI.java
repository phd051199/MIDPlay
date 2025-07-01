package app.ui.player;

import app.common.AudioFileConnector;
import app.common.PlayerMethod;
import app.common.RestClient;
import app.common.SettingManager;
import app.constants.PlayerHttpMethod;
import app.model.Song;
import app.utils.I18N;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

  private static String prevStatus;

  private int currentVolumeLevel = -1;

  private int timerInterval = 500;
  private Timer guiTimer = null;
  private TimerTask timeDisplayTask = null;
  private TimerTask volumeStatusTask = null;
  private PlayerCanvas parent;
  private Player player = null;
  Vector listSong = null;
  int index = 0;
  private boolean restartOnResume = false;
  private boolean isTransitioning = false;

  private AudioFileConnector tempFile;
  private int playerHttpMethod;

  HttpConnection httpConn = null;
  InputStream inputStream = null;
  OutputStream outputStream = null;
  String playUrl;

  public PlayerGUI(PlayerCanvas parent) {
    this.parent = parent;
    this.setStatus("");
    this.guiTimer = new Timer();
  }

  public void setListSong(Vector lst, int index) {
    this.index = index;
    this.listSong = lst;
    this.closePlayer();
    this.setStatus("");
    this.parent.updateDisplay();
  }

  public Vector getListSong() {
    return this.listSong;
  }

  public boolean getIsPlaying() {
    if (this.player == null) {
      return false;
    } else {
      return this.player.getState() >= 400;
    }
  }

  public long getDuration() {
    Song s = (Song) this.listSong.elementAt(this.index);
    try {
      if (player != null && s.getDuration() <= 0) {
        long duration = player.getDuration();
        if (duration > 0) {
          return duration;
        }
      }
    } catch (IllegalStateException e) {
    }

    if (this.listSong != null && this.index >= 0 && this.index < this.listSong.size()) {
      if (s != null) {
        return (long) s.getDuration() * 1000000L;
      }
    }
    return 0L;
  }

  public long getCurrentTime() {
    try {
      if (this.player != null) {
        return this.player.getMediaTime();
      }
    } catch (IllegalStateException var2) {
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
    } catch (IllegalStateException var2) {
    }

    return null;
  }

  private void assertPlayer() throws Throwable {
    this.setStatus(I18N.tr("loading"));
    this.playerHttpMethod = PlayerMethod.getPlayerHttpMethod();

    if (SettingManager.getInstance().getCurrentService().equals("soundcloud")) {
      if (this.playerHttpMethod == PlayerHttpMethod.PASS_URL) {
        this.playerHttpMethod = PlayerHttpMethod.PASS_CONNECTION_STREAM;
      }
    }

    Song s = (Song) this.listSong.elementAt(this.index);

    try {
      this.parent.setAlbumArtUrl(s.getImage());

      if (this.playerHttpMethod == PlayerHttpMethod.SAVE_TO_FILE) {
        this.tempFile = AudioFileConnector.getInstance();

        this.httpConn = RestClient.getInstance().getStreamConnection(s.getStreamUrl());
        this.inputStream = this.httpConn.openInputStream();

        this.tempFile.clear();
        this.outputStream = this.tempFile.openOutputStream();

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }

        this.playUrl = this.tempFile.getFilePath();

        this.player = Manager.createPlayer(this.playUrl);
      } else if (this.playerHttpMethod == PlayerHttpMethod.PASS_CONNECTION_STREAM) {
        this.httpConn = RestClient.getInstance().getStreamConnection(s.getStreamUrl());
        this.inputStream = this.httpConn.openInputStream();

        this.player = Manager.createPlayer(this.inputStream, "audio/mpeg");
      } else {
        this.playUrl = s.getStreamUrl();
        this.player = Manager.createPlayer(this.playUrl);
      }
      this.player.addPlayerListener(this);
      this.player.realize();
      this.player.prefetch();
      this.parent.setupDisplay();

      VolumeControl vc = getVolumeControl();
      if (vc != null && this.currentVolumeLevel >= 0) {
        vc.setLevel(this.currentVolumeLevel);
      }
    } catch (Throwable error) {
      this.player = null;
      this.setStatus(error.toString());
    }
  }

  public synchronized void getNextSong() throws Throwable {
    if (isTransitioning) {
      return;
    }

    try {
      isTransitioning = true;

      if (++this.index >= this.listSong.size()) {
        this.index = 0;
      }

      this.closePlayer();
      this.startPlayer();
      this.setStatus(I18N.tr("playing"));
    } finally {
      isTransitioning = false;
    }
  }

  public synchronized void getPrevSong() throws Throwable {
    if (isTransitioning) {
      return;
    }

    try {
      isTransitioning = true;

      if (--this.index < 0) {
        this.index = this.listSong.size() - 1;
      }

      this.closePlayer();
      this.startPlayer();
      this.setStatus(I18N.tr("playing"));
    } finally {
      isTransitioning = false;
    }
  }

  public void startPlayer() {
    (new Thread(
            new Runnable() {
              public void run() {
                try {
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
                }
              }
            }))
        .start();
  }

  public void closePlayer() {
    this.stopDisplayTimer();
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
    if (this.tempFile != null) {
      this.tempFile.close();
      this.tempFile = null;
    }
    if (this.guiTimer != null) {
      this.guiTimer.cancel();
      this.guiTimer = null;
    }

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

  public void pausePlayer() {
    if (this.player != null) {
      try {
        this.player.stop();
      } catch (Exception var2) {
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
      } catch (Exception var4) {
      }
    }
  }

  public void changeVolume(boolean decrease) {
    int diff = 10;
    if (decrease) {
      diff = -diff;
    }

    VolumeControl vc = this.getVolumeControl();

    if (vc != null) {
      if (!this.parent.getStatus().startsWith(I18N.tr("volume"))) {
        prevStatus = this.parent.getStatus();
      }

      int cv = vc.getLevel();
      if (!decrease && cv == 100) {
        this.setVolumeStatus(cv);
        return;
      }
      if (decrease && cv == 0) {
        this.setVolumeStatus(cv);
        return;
      }

      cv += diff;
      vc.setLevel(cv);
      currentVolumeLevel = cv;

      this.setVolumeStatus(cv);
    }
  }

  private void setVolumeStatus(int cv) {
    this.parent.setStatus(I18N.tr("volume") + ": " + cv);

    if (volumeStatusTask != null) {
      volumeStatusTask.cancel();
      volumeStatusTask = null;
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
        if (plyr.getState() == Player.PREFETCHED || plyr.getState() == Player.STARTED) {
          this.stopDisplayTimer();
          this.parent.updateDisplay();
          if (!isTransitioning) {
            isTransitioning = true;
            try {
              new Thread(
                      new Runnable() {
                        public void run() {
                          try {
                            getNextSong();
                          } catch (Throwable t) {
                          }
                        }
                      })
                  .start();
            } finally {
              isTransitioning = false;
            }
          }
        }
      } else if ("started".equals(evt)) {
        this.startDisplayTimer();
      } else if ("durationUpdated".equals(evt)) {
        this.parent.updateDisplay();
      } else if ("volumeChanged".equals(evt)) {
      }
    } catch (Throwable var2) {
    }
  }

  public synchronized void pauseApp() {
    if (this.player != null && this.player.getState() >= 400) {
      try {
        this.player.stop();
      } catch (MediaException var2) {
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
      } catch (MediaException var2) {
      }
    }
    this.restartOnResume = false;
  }

  private class SPTimerTask extends TimerTask {

    private SPTimerTask() {}

    SPTimerTask(Object x1) {
      this();
    }

    public void run() {
      PlayerGUI.this.parent.updateDisplay();
    }
  }
}
