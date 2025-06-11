package musicapp;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import musicapp.model.Song;

public class PlayerGUI implements PlayerListener {

    private static final int DEFAULT_TIMER_INTERVAL = 500;
    private int timerInterval = 500;
    private Timer guiTimer = null;
    private TimerTask timeDisplayTask = null;
    private PlayerCanvas parent;
    private Player player = null;
    Vector listSong = null;
    int index = 0;
    private static final int SONG_DISPLAY_COUNTER = 4;
    private boolean restartOnResume = false;
    private boolean isplaying;

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

    public String getSongName() {
        Song s = (Song) this.listSong.elementAt(this.index);
        return s != null ? s.getSongName() : "";
    }

    public String getSinger() {
        Song s = (Song) this.listSong.elementAt(this.index);
        return s != null ? s.getArtistName() : "";
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

        Song s = (Song) this.listSong.elementAt(this.index);
        if (s != null) {
            return (long) s.getDuration() * 1000000L;
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
            this.setStatus("Đang tải...");
            Song s = (Song) this.listSong.elementAt(this.index);
            this.player = Manager.createPlayer(s.getStreamUrl());
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
        this.setStatus("Đang chơi...");
    }

    public void getPrevSong() throws Throwable {
        if (--this.index < 0) {
            this.index = this.listSong.size() - 1;
        }

        this.closePlayer();
        this.startPlayer();
        this.setStatus("Đang chơi...");
    }

    public void startPlayer() {
        (new Thread(new Runnable() {
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

                    PlayerGUI.this.setStatus("Bắt đầu...");
                    PlayerGUI.this.player.start();
                    if (PlayerGUI.this.player.getState() >= 400) {
                        PlayerGUI.this.setStatus("Đang chơi...");
                    }
                } catch (Throwable var4) {
                    var4.printStackTrace();
                }

            }
        })).start();
    }

    public void closePlayer() {
        if (this.player != null) {
            this.setStatus("Đang dừng...");

            try {
                this.player.stop();
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            this.player.close();
            this.setStatus("Tạm dừng...");
            this.player = null;
            this.setStatus("");
            if (this.guiTimer == null) {
                this.guiTimer = new Timer();
            }
        }

    }

    public void pausePlayer() {
        if (this.player != null) {
            this.setStatus("Đang dừng...");

            try {
                this.player.stop();
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            this.setStatus("Tạm dừng...");
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
        if (this.timeDisplayTask == null) {
            this.timeDisplayTask = new PlayerGUI.SPTimerTask();
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
            if (evt == "endOfMedia" && plyr.getState() == 400) {
                return;
            }

            if (evt == "closed" || evt == "error" || evt == "endOfMedia" || evt == "stoppedAtTime" || evt == "stopped") {
                this.stopDisplayTimer();
            }

            if (evt == "endOfMedia" || evt == "stoppedAtTime" || evt == "stopped" || evt == "error") {
                this.parent.updateDisplay();
            }

            if (evt.equals("started")) {
                this.startDisplayTimer();
            } else if (!evt.equals("deviceUnavailable") && evt != "bufferingStarted" && evt != "bufferingStopped" && evt != "closed") {
                if (evt == "durationUpdated") {
                    this.parent.updateDisplay();
                } else if (evt == "endOfMedia") {
                    this.setStatus("End of media.");
                    this.getNextSong();
                } else if (evt != "error") {
                    if (evt == "stoppedAtTime") {
                        this.setStatus("Stopped at time.");
                    } else if (evt == "volumeChanged") {
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

        private SPTimerTask() {
        }

        public void run() {
            PlayerGUI.this.parent.updateDisplay();
        }

        // $FF: synthetic method
        SPTimerTask(Object x1) {
            this();
        }
    }
}
