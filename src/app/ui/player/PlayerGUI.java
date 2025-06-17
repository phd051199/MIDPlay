package app.ui.player;

import app.common.RestClient;
import app.model.Song;
import app.utils.I18N;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

public class PlayerGUI implements PlayerListener {

  private int timerInterval = 500;
  private Timer guiTimer = null;
  private TimerTask timeDisplayTask = null;
  private PlayerCanvas parent;
  private Player player = null;
  Vector listSong = null;
  int index = 0;
  private boolean restartOnResume = false;
  private boolean isTransitioning = false;

  private static int playerHttpMethod = 1; // 0 - pass url, 1 - pass connection stream
  // platform
  private static boolean symbianJrt;
  private static boolean symbian;

  private String tempFilePath = null;

  public PlayerGUI(PlayerCanvas parent) {
    this.parent = parent;
    this.setStatus("");
    this.guiTimer = new Timer();
    setPlayerHttpMethod();
  }

  // reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  public static void setPlayerHttpMethod() {
    String p, v;
    if ((p = System.getProperty("microedition.platform")) != null) {
      if ((symbianJrt = p.indexOf("platform=S60") != -1)) {
        int i;
        v = p.substring(i = p.indexOf("platform_version=") + 17, i = p.indexOf(';', i));
      }

      try {
        Class.forName("emulator.custom.CustomMethod");
        p = "KEmulator";
        if ((v = System.getProperty("kemulator.mod.version")) != null) {
          p = p.concat(" ".concat(v));
        }
      } catch (Exception e) {
        int i;

        if ((i = p.indexOf('/')) != -1 || (i = p.indexOf(' ')) != -1) {
          p = p.substring(0, i);
        }
      }
    }
    symbian =
        symbianJrt
            || System.getProperty("com.symbian.midp.serversocket.support") != null
            || System.getProperty("com.symbian.default.to.suite.icon") != null
            || checkClass("com.symbian.midp.io.protocol.http.Protocol")
            || checkClass("com.symbian.lcdjava.io.File");

    // check media capabilities
    try {
      // s40 check
      Class.forName("com.nokia.mid.impl.isa.jam.Jam");
      try {
        Class.forName("com.sun.mmedia.protocol.CommonDS");
        // s40v1 uses sun impl for media and i/o so it should work fine
        playerHttpMethod = 0;
      } catch (Exception e) {
        // s40v2+ breaks http locator parsing
        playerHttpMethod = 1;
      }
    } catch (Exception e) {
      playerHttpMethod = 0;
      if (symbian) {
        if (symbianJrt
            && (p.indexOf("java_build_version=2.") != -1
                || p.indexOf("java_build_version=1.4") != -1)) {
          // emc (s60v5+), supports mp3 streaming
        } else if (checkClass("com.symbian.mmapi.PlayerImpl")) {
          // uiq
        } else {
          // mmf (s60v3.2-)
          playerHttpMethod = 1;
        }
      }
    }
  }

  private static boolean checkClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (Exception e) {
      return false;
    }
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
    try {
      int totalLength = 0;
      int curLength = 0;
      String playUrl;

      this.setStatus(I18N.tr("loading"));
      Song s = (Song) this.listSong.elementAt(this.index);

      if (playerHttpMethod == 1) {
        System.gc();

        HttpConnection httpConn = null;
        InputStream inputStream = null;
        FileConnection fileConn = null;
        OutputStream outputStream = null;

        try {
          String privateDir = System.getProperty("fileconn.dir.private");
          this.tempFilePath = privateDir + "file.mp3";
          FileConnection tempDir =
              (FileConnection) Connector.open(privateDir, Connector.READ_WRITE);
          if (!tempDir.exists()) {
            tempDir.mkdir();
          }
          tempDir.close();

          httpConn = RestClient.getInstance().getStreamConnection(s.getStreamUrl());
          totalLength = (int) httpConn.getLength();
          inputStream = httpConn.openInputStream();

          fileConn = (FileConnection) Connector.open(this.tempFilePath, Connector.READ_WRITE);
          if (fileConn.exists()) {
            fileConn.delete();
          }
          fileConn.create();
          outputStream = fileConn.openOutputStream();

          byte[] buffer = new byte[4096];
          int bytesRead;
          int oldPerc = 0;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            curLength += bytesRead;
            int perc = (int) (curLength * 100L / totalLength);
            if (perc - oldPerc >= 10) {
              this.setStatus(I18N.tr("loading") + " (" + perc + "%)");
              oldPerc = perc;
            }
            outputStream.write(buffer, 0, bytesRead);
          }

          playUrl = this.tempFilePath;

        } finally {
          if (outputStream != null) outputStream.close();
          if (fileConn != null) fileConn.close();
          if (inputStream != null) inputStream.close();
          if (httpConn != null) httpConn.close();
          System.gc();
        }

        this.player = Manager.createPlayer(playUrl);
      } else {
        playUrl = s.getStreamUrl();
        this.player = Manager.createPlayer(playUrl);
      }
      this.player.addPlayerListener(this);
      this.player.realize();
      this.player.prefetch();
      this.parent.setupDisplay();
    } catch (Throwable var2) {
      this.player = null;
      this.setStatus(var2.toString());
      var2.printStackTrace();
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
                    var3.printStackTrace();
                  }

                  PlayerGUI.this.setStatus(I18N.tr("start"));
                  PlayerGUI.this.player.start();
                  if (PlayerGUI.this.player.getState() >= 400) {
                    PlayerGUI.this.setStatus(I18N.tr("playing"));
                  }
                } catch (Throwable var4) {
                  var4.printStackTrace();
                }
              }
            }))
        .start();
  }

  public void closePlayer() {
    this.stopDisplayTimer();

    if (this.player != null) {
      this.setStatus(I18N.tr("stopping"));

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
        e.printStackTrace();
      } finally {
        this.player = null;
      }

      this.setStatus("");
    }

    if (this.guiTimer != null) {
      this.guiTimer.cancel();
      this.guiTimer = null;
    }

    if (this.tempFilePath != null) {
      try {
        FileConnection fc =
            (FileConnection) Connector.open(this.tempFilePath, Connector.READ_WRITE);
        if (fc.exists()) {
          fc.delete();
        }
        fc.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.tempFilePath = null;
    }

    System.gc();
  }

  public void pausePlayer() {
    if (this.player != null) {
      this.setStatus(I18N.tr("stopping"));

      try {
        this.player.stop();
      } catch (Exception var2) {
        var2.printStackTrace();
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
        var4.printStackTrace();
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
      int cv = vc.getLevel();
      cv += diff;
      vc.setLevel(cv);
    }
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
              this.setStatus(I18N.tr("end_of_media"));
              new Thread(
                      new Runnable() {
                        public void run() {
                          try {
                            getNextSong();
                          } catch (Throwable t) {
                            t.printStackTrace();
                          }
                        }
                      })
                  .start();
            } finally {
              isTransitioning = false;
            }
          }
        }
        return;
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
        var2.printStackTrace();
      }
    }

    this.restartOnResume = false;
  }

  private class SPTimerTask extends TimerTask {

    private SPTimerTask() {}

    public void run() {
      PlayerGUI.this.parent.updateDisplay();
    }

    SPTimerTask(Object x1) {
      this();
    }
  }
}
