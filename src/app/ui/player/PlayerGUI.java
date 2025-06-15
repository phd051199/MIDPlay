package app.ui.player;

import app.common.RestClient;
import app.model.Song;
import app.utils.I18N;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
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

  private int playerHttpMethod = 1; // 0 - pass url, 1 - pass connection stream
  // platform
  private boolean symbianJrt;
  private boolean symbian;

  public PlayerGUI(PlayerCanvas parent) {
    this.parent = parent;
    this.setStatus("");
    this.guiTimer = new Timer();
    this.setPlayerHttpMethod();
  }

  // reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  public void setPlayerHttpMethod() {
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
    this.symbian =
        this.symbianJrt
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
        this.playerHttpMethod = 0;
      } catch (Exception e) {
        // s40v2+ breaks http locator parsing
        this.playerHttpMethod = 1;
      }
    } catch (Exception e) {
      this.playerHttpMethod = 0;
      if (this.symbian) {
        if (this.symbianJrt
            && (p.indexOf("java_build_version=2.") != -1
                || p.indexOf("java_build_version=1.4") != -1)) {
          // emc (s60v5+), supports mp3 streaming
        } else if (checkClass("com.symbian.mmapi.PlayerImpl")) {
          // uiq
        } else {
          // mmf (s60v3.2-)
          this.playerHttpMethod = 1;
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
    try {
      if (player != null) {
        long duration = player.getDuration();
        if (duration > 0) {
          return duration;
        }
      }
    } catch (IllegalStateException e) {
    }

    if (this.listSong != null && this.index >= 0 && this.index < this.listSong.size()) {
      Song s = (Song) this.listSong.elementAt(this.index);
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
      this.setStatus(I18N.tr("loading"));
      Song s = (Song) this.listSong.elementAt(this.index);

      if (this.playerHttpMethod == 1) {
        this.player =
            Manager.createPlayer(
                RestClient.getInstance().getConnection(s.getStreamUrl()).openInputStream(),
                "audio/mpeg");
      } else {
        this.player = Manager.createPlayer(s.getStreamUrl());
      }
      this.player.addPlayerListener(this);
      this.player.realize();
      this.player.prefetch();
      this.parent.setupDisplay();
    } catch (Throwable var2) {
      this.player = null;
      var2.printStackTrace();
    }
  }

  public void getNextSong() throws Throwable {
    if (++this.index >= this.listSong.size()) {
      this.index = 0;
    }

    this.closePlayer();
    this.startPlayer();
    this.setStatus(I18N.tr("playing"));
  }

  public void getPrevSong() throws Throwable {
    if (--this.index < 0) {
      this.index = this.listSong.size() - 1;
    }

    this.closePlayer();
    this.startPlayer();
    this.setStatus(I18N.tr("playing"));
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
    if (this.player != null) {
      this.setStatus(I18N.tr("stopping"));

      try {
        this.player.stop();
      } catch (Exception var2) {
        var2.printStackTrace();
      }

      this.player.close();
      this.setStatus(I18N.tr("paused"));
      this.player = null;
      this.setStatus("");
    }

    this.stopDisplayTimer();

    if (this.guiTimer != null) {
      this.guiTimer.cancel();
      this.guiTimer = null;
    }
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
      if ("endOfMedia".equals(evt) && plyr.getState() == 400) {
        return;
      }

      if ("closed".equals(evt)
          || "error".equals(evt)
          || "endOfMedia".equals(evt)
          || "stoppedAtTime".equals(evt)
          || "stopped".equals(evt)) {
        this.stopDisplayTimer();
      }

      if (evt.equals("endOfMedia")
          || evt.equals("stoppedAtTime")
          || evt.equals("stopped")
          || evt.equals("error")) {
        this.parent.updateDisplay();
      }

      if (evt.equals("started")) {
        this.startDisplayTimer();
      } else if (!evt.equals("deviceUnavailable")
          && !evt.equals("bufferingStarted")
          && !evt.equals("bufferingStopped")
          && !evt.equals("closed")) {
        if (evt.equals("durationUpdated")) {
          this.parent.updateDisplay();
        } else if (evt.equals("endOfMedia")) {
          this.setStatus(I18N.tr("end_of_media"));
          this.getNextSong();
        } else if (!evt.equals("error")) {
          if (evt.equals("stoppedAtTime")) {
            this.setStatus(I18N.tr("stopped_at_time"));
          } else if (evt.equals("volumeChanged")) {
          }
        }
      }
    } catch (Throwable var5) {
      var5.printStackTrace();
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
