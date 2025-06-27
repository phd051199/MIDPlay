package app.ui.player;

import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class PlayerCanvas extends Canvas implements CommandListener, LoadDataObserver {

  private static final int PLAYER_STATUS_TOP = 2;
  private static final int SONG_TITLE_GAP = 5;
  private static final int TIME_GAP = 10;

  private final Command backCommand = new Command(I18N.tr("back"), Command.BACK, 1);
  private final Command playCommand = new Command(I18N.tr("play"), Command.OK, 5);
  private final Command pauseCommand = new Command(I18N.tr("pause"), Command.SCREEN, 1);
  private final Command stopCommand = new Command(I18N.tr("stop"), Command.SCREEN, 1);
  private final Command nextCommand = new Command(I18N.tr("next"), Command.SCREEN, 5);
  private final Command prevCommand = new Command(I18N.tr("previous"), Command.SCREEN, 5);
  private String title;
  private PlayerGUI gui;
  private Utils.BreadCrumbTrail parent;
  private String status = "";
  int displayWidth = -1;
  int displayHeight = -1;
  int textHeight = 10;
  int logoTop = 0;
  int songTitleTop = 0;
  int timeRateTop = 0;
  int timeWidth = 0;
  int feedbackTop = 0;
  int statusTop = 0;
  private Utils.BreadCrumbTrail observer = null;
  private Playlist playlist;
  private boolean _play = false;
  private Image _imgPlay = null;
  private Image _imgPause = null;
  private Image _imgBack = null;
  private Image _imgNext = null;
  private Image _imgBackActive = null;
  private Image _imgNextActive = null;
  int statusBarTop = 0;
  int statusBarHeight = 0;
  int songNameTop = 0;
  int SignerNameTop = 0;
  int sliderTop = 0;
  int sliderLeft = 0;
  int sliderHeight = 10;
  int sliderWidth = 10;
  float slidervalue = 0.0F;
  int playtop = 0;
  public long timeBack = 0L;
  public long timeNext = 0L;
  public boolean goBack = false;
  public boolean goNext = false;

  public PlayerCanvas(String title, Vector lst, int index, Playlist _playlist) {
    this.playlist = _playlist;
    this.setTitle(title);
    this.addCommand(this.backCommand);
    this.setCommandListener(this);
    this.createCommand();
    this.change(title, lst, index, this.playlist);
  }

  public void showNotify() {
    if (this.status.equals(I18N.tr("playing")) || this.status.equals(I18N.tr("paused"))) {
      if (this.getGUI().getIsPlaying()) {
        this.setStatus(I18N.tr("playing"));
      } else {
        this.setStatus(I18N.tr("paused"));
      }
    }
  }

  public void change(String title, Vector lst, int index, Playlist _playlist) {
    this.title = title;
    this.setTitle(title);
    this.playlist = _playlist;
    this.getGUI().setListSong(lst, index);
    this._play = true;
  }

  public void setupDisplay() {
    this.displayHeight = -1;
    this.updateDisplay();
  }

  public String getTitle() {
    return this.title;
  }

  public void setStatus(String s) {
    this.status = s;
    this.updateDisplay();
  }

  public void updateDisplay() {
    this.repaint();
    this.serviceRepaints();
  }

  public synchronized void close() {
    if (this.gui != null) {
      this.gui.closePlayer();
      this.gui = null;
    }
  }

  private boolean isLoading() {
    return this.status.indexOf(I18N.tr("loading")) != -1;
  }

  private synchronized PlayerGUI getGUI() {
    if (this.gui == null) {
      this.gui = new PlayerGUI(this);
    }

    return this.gui;
  }

  protected void keyPressed(int keycode) {
    try {
      int action = this.getGameAction(keycode);
      switch (action) {
        case Canvas.FIRE:
          if (!this.isLoading()) {
            this.gui.togglePlayer();
          }
          break;
        case Canvas.RIGHT:
          if (!this.isLoading()) {
            this.goNext = true;
            this.timeNext = System.currentTimeMillis();
            this.gui.getNextSong();
          }
          break;
        case Canvas.LEFT:
          if (!this.isLoading()) {
            this.goBack = true;
            this.timeBack = System.currentTimeMillis();
            this.gui.getPrevSong();
          }
          break;
        case Canvas.UP:
          this.gui.changeVolume(false);
          break;
        case Canvas.DOWN:
          this.gui.changeVolume(true);
          break;
      }
    } catch (Throwable var3) {
      Utils.error(var3, this.parent);
    }
  }

  private boolean intersects(int clipY, int clipHeight, int y, int h) {
    return clipY <= y + h && clipY + clipHeight >= y;
  }

  private String timeDisplay(long us) {
    long ts = us / 100000L;
    return this.formatNumber(ts / 600L, 2, true)
        + ":"
        + this.formatNumber(ts % 600L / 10L, 2, true);
  }

  private String formatNumber(long num, int len, boolean leadingZeros) {
    StringBuffer ret = new StringBuffer(String.valueOf(num));
    if (leadingZeros) {
      while (ret.length() < len) {
        ret.insert(0, '0');
      }
    } else {
      while (ret.length() < len) {
        ret.append('0');
      }
    }

    return ret.toString();
  }

  public Image imgPlay() {
    if (this._imgPlay == null) {
      try {
        this._imgPlay = Image.createImage("/images/icon-play.png");
      } catch (Exception var2) {
        this._imgPlay = null;
      }
    }

    return this._imgPlay;
  }

  public Image imgPause() {
    if (this._imgPause == null) {
      try {
        this._imgPause = Image.createImage("/images/icon-pause.png");
      } catch (Exception var2) {
        this._imgPause = null;
      }
    }

    return this._imgPause;
  }

  public Image imgBack() {
    if (this._imgBack == null) {
      try {
        this._imgBack = Image.createImage("/images/button-back.png");
      } catch (Exception var2) {
        this._imgBack = null;
      }
    }

    return this._imgBack;
  }

  public Image imgNext() {
    if (this._imgNext == null) {
      try {
        this._imgNext = Image.createImage("/images/button-next.png");
      } catch (Exception var2) {
        this._imgNext = null;
      }
    }

    return this._imgNext;
  }

  public Image imgBackActive() {
    if (this._imgBackActive == null) {
      try {
        this._imgBackActive = Image.createImage("/images/button-back-active.png");
      } catch (Exception var2) {
        this._imgBackActive = null;
      }
    }

    return this._imgBackActive;
  }

  public Image imgNextActive() {
    if (this._imgNextActive == null) {
      try {
        this._imgNextActive = Image.createImage("/images/button-next-active.png");
      } catch (Exception var2) {
        this._imgNextActive = null;
      }
    }

    return this._imgNextActive;
  }

  public void paint(Graphics g) {
    try {
      int clipX;
      if (this.displayHeight == -1) {
        this.displayWidth = this.getWidth();
        this.displayHeight = this.getHeight();
        this.textHeight = g.getFont().getHeight();
        this.statusBarHeight = this.textHeight + 5;
        clipX = PLAYER_STATUS_TOP + this.statusBarHeight + 5;
        this.songNameTop = clipX;
        clipX += SONG_TITLE_GAP + this.textHeight;
        this.SignerNameTop = clipX;
        clipX += TIME_GAP + this.textHeight;
        this.timeRateTop = clipX;
        this.timeWidth = g.getFont().stringWidth("0:00:0  ");
        this.sliderHeight = 10;
        this.sliderWidth = this.displayWidth - 20;
        this.sliderTop = this.timeRateTop + this.textHeight + 10;
        this.sliderLeft = this.displayWidth - this.sliderWidth >> 1;
        this.playtop = this.sliderTop + this.sliderHeight + 30;
        this.feedbackTop = this.sliderTop + this.sliderHeight + this.textHeight;
      }

      clipX = g.getClipX();
      int clipY = g.getClipY();
      int clipWidth = g.getClipWidth();
      int clipHeight = g.getClipHeight();

      g.setColor(245, 245, 245);
      g.fillRect(0, 0, this.displayWidth, this.displayHeight);

      g.setColor(65, 10, 74);
      g.fillRect(0, 0, this.displayWidth, this.statusBarHeight);

      if (this.intersects(clipY, clipHeight, PLAYER_STATUS_TOP, this.textHeight)) {
        g.setColor(255, 255, 255);
        g.drawString(this.status, this.displayWidth >> 1, PLAYER_STATUS_TOP, 17);
      }

      if (this.gui != null) {

        if (this.intersects(clipY, clipHeight, this.songNameTop, this.textHeight)) {
          g.setColor(65, 10, 74);
          g.drawString(this.gui.getSongName(), this.displayWidth >> 1, this.songNameTop, 17);
        }

        if (this.intersects(clipY, clipHeight, this.SignerNameTop, this.textHeight)) {
          g.setColor(140, 140, 140);
          g.drawString(this.gui.getSinger(), this.displayWidth >> 1, this.SignerNameTop, 17);
        }

        g.setColor(210, 210, 210);
        g.drawLine(0, this.timeRateTop - 3, this.displayWidth, this.timeRateTop - 3);
        long duration = this.gui.getDuration();
        long current = this.gui.getCurrentTime();
        String strDuration = this.timeDisplay(duration);
        String strCurrent = this.timeDisplay(current);
        this.slidervalue = (float) this.sliderWidth * ((float) current / (float) duration);
        if (this.intersects(clipY, clipHeight, this.timeRateTop, this.textHeight)
            && this.intersects(clipX, clipWidth, 0, this.timeWidth)) {
          g.setColor(140, 140, 140);
          g.drawString(strCurrent, this.sliderLeft, this.timeRateTop, 20);
          g.drawString(strDuration, this.sliderLeft + this.sliderWidth, this.timeRateTop, 24);
        }

        g.setColor(220, 220, 220);
        g.fillRect(this.sliderLeft, this.sliderTop, this.sliderWidth, this.sliderHeight);

        g.setColor(65, 10, 74);
        g.fillRect(this.sliderLeft, this.sliderTop, (int) this.slidervalue, this.sliderHeight);
        if (!this.gui.getIsPlaying() && this.imgPlay() != null) {
          g.drawImage(this._imgPlay, this.displayWidth >> 1, this.playtop, 3);
        }

        if (this.gui.getIsPlaying() && this.imgPause() != null) {
          g.drawImage(this._imgPause, this.displayWidth >> 1, this.playtop, 3);
        }

        if (this.goBack || this.goNext) {
          long time = System.currentTimeMillis();
          if (time - this.timeBack >= 1000L) {
            this.goBack = false;
          }

          if (time - this.timeNext >= 1000L) {
            this.goNext = false;
          }
        }

        int buttonWidth = 40;
        int playButtonX = this.displayWidth >> 1;
        int buttonGap = 40;

        if (this.goBack && this.imgBack() != null) {
          g.drawImage(this._imgBack, playButtonX - buttonWidth - buttonGap, this.playtop, 6);
        }

        if (!this.goBack && this.imgBackActive() != null) {
          g.drawImage(this._imgBackActive, playButtonX - buttonWidth - buttonGap, this.playtop, 6);
        }

        if (this.goNext && this.imgNext() != null) {
          g.drawImage(this._imgNext, playButtonX + buttonWidth + buttonGap, this.playtop, 10);
        }

        if (!this.goNext && this.imgNextActive() != null) {
          g.drawImage(this._imgNextActive, playButtonX + buttonWidth + buttonGap, this.playtop, 10);
        }
      }
    } catch (Throwable var14) {
    }

    if (this._play) {
      this._play = false;
      this.getGUI().closePlayer();
      this.getGUI().startPlayer();
    }
  }

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.backCommand) {
      this.observer.goBack();
    } else if (c == this.nextCommand) {
      if (!this.isLoading()) {
        try {
          this.goNext = true;
          this.timeNext = System.currentTimeMillis();
          this.getGUI().getNextSong();
        } catch (Throwable var5) {
        }
      }
    } else if (c == this.prevCommand) {
      if (!this.isLoading()) {
        try {
          this.goBack = true;
          this.timeBack = System.currentTimeMillis();
          this.getGUI().getPrevSong();
        } catch (Throwable var4) {
        }
      }
    } else if (c == this.playCommand || c == this.pauseCommand) {
      if (!this.isLoading()) {
        this.getGUI().togglePlayer();
      }
    } else if (c == this.stopCommand) {
      this.gui.pausePlayer();
      this.gui.setMediaTime(0L);
      this.setStatus(I18N.tr("paused"));
    }
  }

  private void createCommand() {
    this.addCommand(this.playCommand);
    this.addCommand(this.stopCommand);
    this.addCommand(this.nextCommand);
    this.addCommand(this.prevCommand);
  }

  public void cancel() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
