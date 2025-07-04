package app.ui.player;

import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.ImageUtils;
import app.utils.Utils;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public final class PlayerCanvas extends Canvas implements CommandListener, LoadDataObserver {

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
  int artSize = 72;
  int artLeft = 8;
  int songInfoLeft = 0;
  int statusBarTop = 0;
  int statusBarHeight = 0;
  int songNameTop = 0;
  int SignerNameTop = 0;
  int sliderTop = 0;
  int sliderLeft = 0;
  int sliderHeight = 6;
  int sliderWidth = 12;
  float slidervalue = 0.0F;
  int playtop = 0;

  private boolean touchSupported = false;
  private int playButtonX = 0;
  private int playButtonY = 0;
  private final int playButtonWidth = 50;
  private final int playButtonHeight = 50;
  private int prevButtonX = 0;
  private int prevButtonY = 0;
  private int nextButtonX = 0;
  private int nextButtonY = 0;
  private final int buttonWidth = 40;
  private final int buttonHeight = 30;

  private Utils.BreadCrumbTrail observer = null;
  private Playlist playlist;
  private boolean _play = false;

  private Image _imgPlay = null;
  private Image _imgPause = null;
  private Image _imgBack = null;
  private Image _imgNext = null;
  private Image _imgBackActive = null;
  private Image _imgNextActive = null;
  private Image _albumArt = null;
  private String _albumArtUrl = null;
  private boolean _loadingAlbumArt = false;

  public long timeBack = 0L;
  public long timeNext = 0L;
  public boolean goBack = false;
  public boolean goNext = false;

  private Thread loadAlbumArtThread = null;

  public PlayerCanvas(String title, Vector lst, int index, Playlist _playlist) {
    this.playlist = _playlist;
    this.setTitle(title);
    this.addCommand(this.backCommand);
    this.setCommandListener(this);
    this.createCommand();

    this.touchSupported = this.hasPointerEvents();

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

    this._albumArt = null;
    this._albumArtUrl = null;
    this._loadingAlbumArt = false;
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

  public String getStatus() {
    return this.status;
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

  public void setAlbumArtUrl(String url) {
    if (url != null && !url.equals(this._albumArtUrl)) {
      this._albumArtUrl = url;
      this._albumArt = null;
      this._loadingAlbumArt = false;
      loadAlbumArt();
    }
  }

  private void loadAlbumArt() {
    if (this._albumArtUrl == null || this._loadingAlbumArt) {
      return;
    }

    this._loadingAlbumArt = true;

    this.loadAlbumArtThread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  _albumArt = ImageUtils.getImage(_albumArtUrl, artSize);
                  if (_albumArt != null) {
                    updateDisplay();
                  }
                } catch (IOException e) {
                  _albumArt = null;
                } finally {
                  _loadingAlbumArt = false;
                }
              }
            });
    this.loadAlbumArtThread.start();
  }

  protected void keyPressed(int keycode) {
    try {
      int action = this.getGameAction(keycode);
      handleAction(action);
    } catch (Throwable var3) {
    }
  }

  protected void pointerPressed(int x, int y) {
    if (!this.touchSupported || this.isLoading()) {
      return;
    }

    try {
      if (isPointInButton(x, y, playButtonX, playButtonY, playButtonWidth, playButtonHeight)) {
        handleAction(Canvas.FIRE);
        return;
      }

      if (isPointInButton(x, y, prevButtonX, prevButtonY, buttonWidth, buttonHeight)) {
        handleAction(Canvas.LEFT);
        return;
      }

      if (isPointInButton(x, y, nextButtonX, nextButtonY, buttonWidth, buttonHeight)) {
        handleAction(Canvas.RIGHT);
        return;
      }

      if (isPointInSlider(x, y)) {
        seekToPosition(x);
      }
    } catch (Throwable e) {
    }
  }

  private void handleAction(int action) {
    if (this.isLoading()) {
      return;
    }

    try {
      switch (action) {
        case Canvas.FIRE:
          this.gui.togglePlayer();
          break;
        case Canvas.RIGHT:
          this.goNext = true;
          this.timeNext = System.currentTimeMillis();
          this.gui.getNextSong();
          break;
        case Canvas.LEFT:
          this.goBack = true;
          this.timeBack = System.currentTimeMillis();
          this.gui.getPrevSong();
          break;
        case Canvas.UP:
          this.gui.changeVolume(false);
          break;
        case Canvas.DOWN:
          this.gui.changeVolume(true);
          break;
      }
    } catch (Throwable e) {
    }
  }

  private boolean isPointInButton(
      int x, int y, int buttonX, int buttonY, int buttonW, int buttonH) {
    return x >= buttonX - buttonW / 2
        && x <= buttonX + buttonW / 2
        && y >= buttonY - buttonH / 2
        && y <= buttonY + buttonH / 2;
  }

  private boolean isPointInSlider(int x, int y) {
    return x >= this.sliderLeft
        && x <= this.sliderLeft + this.sliderWidth
        && y >= this.sliderTop - 15
        && y <= this.sliderTop + this.sliderHeight + 15;
  }

  private void seekToPosition(int x) {
    if (this.gui == null) {
      return;
    }

    int relativeX = x - this.sliderLeft;
    if (relativeX < 0) {
      relativeX = 0;
    }
    if (relativeX > this.sliderWidth) {
      relativeX = this.sliderWidth;
    }

    float percentage = (float) relativeX / (float) this.sliderWidth;
    long duration = this.gui.getDuration();
    long seekTime = (long) (duration * percentage);

    this.gui.setMediaTime(seekTime);
  }

  private boolean intersects(int clipY, int clipHeight, int y, int h) {
    return clipY <= y + h && clipY + clipHeight >= y;
  }

  private String truncateText(String text, Graphics g, int maxWidth) {
    if (text == null) {
      return "";
    }

    int textWidth = g.getFont().stringWidth(text);
    if (textWidth <= maxWidth) {
      return text;
    }

    String ellipsis = "...";
    int ellipsisWidth = g.getFont().stringWidth(ellipsis);
    int availableWidth = maxWidth - ellipsisWidth;

    if (availableWidth <= 0) {
      return ellipsis;
    }

    int len = text.length();
    while (len > 0) {
      String truncated = text.substring(0, len);
      if (g.getFont().stringWidth(truncated) <= availableWidth) {
        return truncated + ellipsis;
      }
      len--;
    }

    return ellipsis;
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

        this.songInfoLeft = this.artLeft + this.artSize + 15;

        clipX = PLAYER_STATUS_TOP + this.statusBarHeight + 15;
        this.songNameTop = clipX;
        clipX += SONG_TITLE_GAP + this.textHeight;
        this.SignerNameTop = clipX;
        clipX += TIME_GAP + this.textHeight + 24;

        this.timeRateTop = clipX;
        this.timeWidth = g.getFont().stringWidth("0:00:0  ");

        this.sliderWidth = this.displayWidth - (this.timeWidth * 2) - 8;
        this.sliderTop = this.timeRateTop + (this.textHeight - this.sliderHeight) / 2;
        this.sliderLeft = this.timeWidth + 4;

        this.playtop = this.timeRateTop + this.textHeight + 24;
        this.feedbackTop = this.timeRateTop + this.textHeight;
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
        int artTop = PLAYER_STATUS_TOP + this.statusBarHeight + 8;

        if (this._albumArt != null) {
          g.drawImage(
              this._albumArt,
              this.artLeft + this.artSize / 2,
              artTop + this.artSize / 2,
              Graphics.HCENTER | Graphics.VCENTER);

          g.setColor(150, 150, 150);
          g.drawRect(this.artLeft, artTop, this.artSize, this.artSize);
        } else {
          g.setColor(200, 200, 200);
          g.fillRect(this.artLeft, artTop, this.artSize, this.artSize);
          g.setColor(150, 150, 150);
          g.drawRect(this.artLeft, artTop, this.artSize, this.artSize);

          g.setColor(100, 100, 100);
          g.drawString("♪", this.artLeft + this.artSize / 2, artTop + this.artSize / 2, 17);
        }

        if (this.intersects(clipY, clipHeight, this.songNameTop, this.textHeight)) {
          g.setColor(65, 10, 74);
          int maxSongNameWidth = this.displayWidth - this.songInfoLeft - 10;
          String truncatedSongName = this.truncateText(this.gui.getSongName(), g, maxSongNameWidth);
          g.drawString(truncatedSongName, this.songInfoLeft, this.songNameTop, 20);
        }

        if (this.intersects(clipY, clipHeight, this.SignerNameTop, this.textHeight)) {
          g.setColor(140, 140, 140);
          int maxSingerWidth = this.displayWidth - this.songInfoLeft - 10;
          String truncatedSinger = this.truncateText(this.gui.getSinger(), g, maxSingerWidth);
          g.drawString(truncatedSinger, this.songInfoLeft, this.SignerNameTop, 20);
        }

        long duration = this.gui.getDuration();
        long current = this.gui.getCurrentTime();
        String strDuration = this.timeDisplay(duration);
        String strCurrent = this.timeDisplay(current);

        if (duration > 0) {
          this.slidervalue = (float) this.sliderWidth * ((float) current / (float) duration);
        } else {
          this.slidervalue = 0;
        }

        if (this.intersects(clipY, clipHeight, this.timeRateTop, this.textHeight)) {
          g.setColor(140, 140, 140);
          g.drawString(strCurrent, 5, this.timeRateTop, 20);
        }

        g.setColor(220, 220, 220);
        g.fillRect(this.sliderLeft, this.sliderTop, this.sliderWidth, this.sliderHeight);

        g.setColor(65, 10, 74);
        g.fillRect(this.sliderLeft, this.sliderTop, (int) this.slidervalue, this.sliderHeight);

        if (this.intersects(clipY, clipHeight, this.timeRateTop, this.textHeight)) {
          g.setColor(140, 140, 140);
          g.drawString(strDuration, this.displayWidth - 5, this.timeRateTop, 24);
        }

        if (this.playButtonX == 0) {
          int buttonWidth = 40;
          int buttonGap = 40;
          this.playButtonX = this.displayWidth >> 1;
          this.playButtonY = this.playtop;

          this.prevButtonX = this.playButtonX - buttonWidth - buttonGap;
          this.prevButtonY = this.playtop;
          this.nextButtonX = this.playButtonX + buttonWidth + buttonGap;
          this.nextButtonY = this.playtop;
        }

        boolean isPlaying = this.gui.getIsPlaying();
        Image playPauseImg = isPlaying ? this.imgPause() : this.imgPlay();

        if (playPauseImg != null) {

          g.drawImage(playPauseImg, this.playButtonX, this.playButtonY, 3);
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

        Image prevImg = this.goBack ? this.imgBackActive() : this.imgBack();
        if (prevImg != null) {

          g.drawImage(prevImg, this.prevButtonX, this.prevButtonY, 3);
        }

        Image nextImg = this.goNext ? this.imgNextActive() : this.imgNext();
        if (nextImg != null) {

          g.drawImage(nextImg, this.nextButtonX, this.nextButtonY, 3);
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

  public void cancel() {}
}
