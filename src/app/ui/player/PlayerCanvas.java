package app.ui.player;

import app.MIDPlay;
import app.core.data.FavoritesCallback;
import app.core.data.FavoritesManager;
import app.core.data.LoadDataObserver;
import app.core.settings.SettingsManager;
import app.core.threading.ThreadManagerIntegration;
import app.models.Playlist;
import app.models.Song;
import app.ui.FavoritesList;
import app.ui.MainObserver;
import app.ui.SongList;
import app.utils.I18N;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public final class PlayerCanvas extends Canvas implements CommandListener, LoadDataObserver {

  private static final int PLAYER_STATUS_TOP = 2;
  private static final int SONG_TITLE_GAP = 5;
  private static final int TIME_GAP = 10;

  private final Command backCommand = new Command(I18N.tr("back"), Command.BACK, 1);
  private final Command playCommand = new Command(I18N.tr("play"), Command.OK, 1);
  private final Command pauseCommand = new Command(I18N.tr("pause"), Command.OK, 2);
  private final Command nextCommand = new Command(I18N.tr("next"), Command.SCREEN, 3);
  private final Command prevCommand = new Command(I18N.tr("previous"), Command.SCREEN, 4);
  private final Command stopCommand = new Command(I18N.tr("stop"), Command.SCREEN, 7);
  private final Command addToPlaylistCommand =
      new Command(I18N.tr("add_to_playlist"), Command.SCREEN, 8);
  private final Command showPlaylistCommand =
      new Command(I18N.tr("show_playlist"), Command.SCREEN, 9);
  private Command repeatCommand;
  private Command shuffleCommand;

  private String title;
  private PlayerGUI gui;
  private MainObserver parent;
  private String status = "";
  private final FavoritesManager favoritesManager;

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
  private int repeatButtonX = 0;
  private int repeatButtonY = 0;
  private int shuffleButtonX = 0;
  private int shuffleButtonY = 0;
  private final int buttonWidth = 40;
  private final int buttonHeight = 40;
  private final int controlButtonWidth = 40;
  private final int controlButtonHeight = 40;

  private MainObserver observer = null;
  private Playlist playlist;
  private boolean _play = false;

  private Image _imgPlay = null;
  private Image _imgPause = null;
  private Image _imgBack = null;
  private Image _imgNext = null;
  private Image _imgBackActive = null;
  private Image _imgNextActive = null;
  private Image _imgRepeat = null;
  private Image _imgRepeatOne = null;
  private Image _imgRepeatOff = null;
  private Image _imgShuffle = null;
  private Image _imgShuffleOff = null;
  private Image _albumArt = null;
  private String _albumArtUrl = null;
  private boolean _loadingAlbumArt = false;

  public long timeBack = 0L;
  public long timeNext = 0L;
  public boolean goBack = false;
  public boolean goNext = false;

  private final SettingsManager settingManager = SettingsManager.getInstance();
  private int cachedThemeColorRGB;
  private int cachedBackgroundColorRGB;
  private String lastThemeColor = "";
  private String lastBackgroundColor = "";

  public PlayerCanvas(String title, Vector lst, int index, Playlist _playlist) {
    this.playlist = _playlist;
    this.favoritesManager = FavoritesManager.getInstance();
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
    this._albumArt = null;
    this._albumArtUrl = null;
    this._loadingAlbumArt = false;

    if (this.gui != null) {
      this.gui.shutdown();
      this.gui.closePlayer();
      this.gui = null;
    }

    ThreadManagerIntegration.clearPlayerQueues();
  }

  private boolean isLoading() {
    return this.status.indexOf(I18N.tr("loading")) != -1;
  }

  private synchronized PlayerGUI getGUI() {
    if (this.gui == null) {
      this.gui = new PlayerGUI(this);

      updateRepeatCommand();
      updateShuffleCommand();
    }
    return this.gui;
  }

  public void updateRepeatCommand() {
    if (this.repeatCommand != null) {
      this.removeCommand(this.repeatCommand);
    }

    String commandText = "";
    if (this.gui != null) {
      switch (this.gui.getRepeatMode()) {
        case PlayerGUI.REPEAT_OFF:
          commandText = I18N.tr("repeat") + ": " + I18N.tr("off");
          break;
        case PlayerGUI.REPEAT_ONE:
          commandText = I18N.tr("repeat") + ": " + I18N.tr("one");
          break;
        case PlayerGUI.REPEAT_ALL:
          commandText = I18N.tr("repeat") + ": " + I18N.tr("all");
          break;
      }
    } else {
      commandText = I18N.tr("repeat") + ": " + I18N.tr("all");
    }

    this.repeatCommand = new Command(commandText, Command.SCREEN, 5);
    this.addCommand(this.repeatCommand);
  }

  public void updateShuffleCommand() {
    if (this.shuffleCommand != null) {
      this.removeCommand(this.shuffleCommand);
    }

    String commandText;
    if (this.gui != null) {
      commandText =
          I18N.tr("shuffle") + ": " + (this.gui.getShuffleMode() ? I18N.tr("on") : I18N.tr("off"));
    } else {
      commandText = I18N.tr("shuffle") + ": " + I18N.tr("off");
    }

    this.shuffleCommand = new Command(commandText, Command.SCREEN, 6);
    this.addCommand(this.shuffleCommand);
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

    final String imageUrl = this._albumArtUrl;
    ThreadManagerIntegration.executePlayerImageLoading(
        imageUrl,
        72,
        new ThreadManagerIntegration.ImageLoadCallback() {
          public void onImageLoaded(Object image) {
            _albumArt = (javax.microedition.lcdui.Image) image;
            if (_albumArt != null) {
              updateDisplay();
            }
            _loadingAlbumArt = false;
          }

          public void onImageLoadError(Exception e) {
            _albumArt = null;
            _loadingAlbumArt = false;
          }
        });
  }

  protected void keyPressed(int keycode) {
    try {
      int action = this.getGameAction(keycode);
      handleAction(action);
    } catch (Throwable e) {
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

      if (isPointInButton(
          x, y, repeatButtonX, repeatButtonY, controlButtonWidth, controlButtonHeight)) {
        this.gui.toggleRepeatMode();
        this.updateDisplay();
        return;
      }

      if (isPointInButton(
          x, y, shuffleButtonX, shuffleButtonY, controlButtonWidth, controlButtonHeight)) {
        this.gui.toggleShuffleMode();
        this.updateDisplay();
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
        this._imgPlay = Image.createImage("/images/player/play.png");
      } catch (Exception e) {
        this._imgPlay = null;
      }
    }
    return this._imgPlay;
  }

  public Image imgPause() {
    if (this._imgPause == null) {
      try {
        this._imgPause = Image.createImage("/images/player/pause.png");
      } catch (Exception e) {
        this._imgPause = null;
      }
    }
    return this._imgPause;
  }

  public Image imgBack() {
    if (this._imgBack == null) {
      try {
        this._imgBack = Image.createImage("/images/player/previous.png");
      } catch (Exception e) {
        this._imgBack = null;
      }
    }
    return this._imgBack;
  }

  public Image imgNext() {
    if (this._imgNext == null) {
      try {
        this._imgNext = Image.createImage("/images/player/next.png");
      } catch (Exception e) {
        this._imgNext = null;
      }
    }
    return this._imgNext;
  }

  public Image imgBackActive() {
    if (this._imgBackActive == null) {
      try {
        this._imgBackActive = Image.createImage("/images/player/previous.png");
      } catch (Exception e) {
        this._imgBackActive = null;
      }
    }
    return this._imgBackActive;
  }

  public Image imgNextActive() {
    if (this._imgNextActive == null) {
      try {
        this._imgNextActive = Image.createImage("/images/player/next.png");
      } catch (Exception e) {
        this._imgNextActive = null;
      }
    }
    return this._imgNextActive;
  }

  public Image imgRepeat() {
    if (this._imgRepeat == null) {
      try {
        this._imgRepeat = Image.createImage("/images/player/repeat.png");
      } catch (Exception e) {
        this._imgRepeat = null;
      }
    }
    return this._imgRepeat;
  }

  public Image imgRepeatOne() {
    if (this._imgRepeatOne == null) {
      try {
        this._imgRepeatOne = Image.createImage("/images/player/repeat-one.png");
      } catch (Exception e) {
        this._imgRepeatOne = null;
      }
    }
    return this._imgRepeatOne;
  }

  public Image imgRepeatOff() {
    if (this._imgRepeatOff == null) {
      try {
        this._imgRepeatOff = Image.createImage("/images/player/repeat-off.png");
      } catch (Exception e) {
        this._imgRepeatOff = null;
      }
    }
    return this._imgRepeatOff;
  }

  public Image imgShuffle() {
    if (this._imgShuffle == null) {
      try {
        this._imgShuffle = Image.createImage("/images/player/shuffle.png");
      } catch (Exception e) {
        this._imgShuffle = null;
      }
    }
    return this._imgShuffle;
  }

  public Image imgShuffleOff() {
    if (this._imgShuffleOff == null) {
      try {
        this._imgShuffleOff = Image.createImage("/images/player/shuffle-off.png");
      } catch (Exception e) {
        this._imgShuffleOff = null;
      }
    }
    return this._imgShuffleOff;
  }

  private int getThemeColor() {
    String currentThemeColor = settingManager.getThemeColor();
    if (!currentThemeColor.equals(lastThemeColor)) {
      try {
        cachedThemeColorRGB = Integer.parseInt(currentThemeColor, 16);
        lastThemeColor = currentThemeColor;
      } catch (Exception e) {
        cachedThemeColorRGB = 0x410A4A;
      }
    }
    return cachedThemeColorRGB;
  }

  private void setThemeColor(Graphics g) {
    int color = getThemeColor();
    int r = (color >> 16) & 0xFF;
    int g1 = (color >> 8) & 0xFF;
    int b = color & 0xFF;
    g.setColor(r, g1, b);
  }

  private int getBackgroundColor() {
    String currentBackgroundColor = settingManager.getBackgroundColor();
    if (!currentBackgroundColor.equals(lastBackgroundColor)) {
      try {
        cachedBackgroundColorRGB = Integer.parseInt(currentBackgroundColor, 16);
        lastBackgroundColor = currentBackgroundColor;
      } catch (Exception e) {
        cachedBackgroundColorRGB = 0xF0F0F0;
      }
    }
    return cachedBackgroundColorRGB;
  }

  private void setBackgroundColor(Graphics g) {
    int color = getBackgroundColor();
    int r = (color >> 16) & 0xFF;
    int g1 = (color >> 8) & 0xFF;
    int b = color & 0xFF;
    g.setColor(r, g1, b);
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

      setBackgroundColor(g);
      g.fillRect(0, 0, this.displayWidth, this.displayHeight);

      setThemeColor(g);
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
          setThemeColor(g);
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

        setThemeColor(g);
        g.fillRect(this.sliderLeft, this.sliderTop, (int) this.slidervalue, this.sliderHeight);

        if (this.intersects(clipY, clipHeight, this.timeRateTop, this.textHeight)) {
          g.setColor(140, 140, 140);
          g.drawString(strDuration, this.displayWidth - 5, this.timeRateTop, 24);
        }

        if (this.playButtonX == 0) {
          int screenCenter = this.displayWidth >> 1;
          int buttonGap = this.displayWidth / 5;
          int margin = 8;

          this.playButtonX = screenCenter;
          this.playButtonY = this.playtop;

          this.prevButtonX = screenCenter - buttonGap;
          this.prevButtonY = this.playtop;

          this.nextButtonX = screenCenter + buttonGap;
          this.nextButtonY = this.playtop;

          this.repeatButtonX = margin + (this.controlButtonWidth / 2);
          this.repeatButtonY = this.playtop;

          this.shuffleButtonX = this.displayWidth - margin - (this.controlButtonWidth / 2);
          this.shuffleButtonY = this.playtop;
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

        Image repeatImg = null;
        switch (this.getGUI().getRepeatMode()) {
          case PlayerGUI.REPEAT_ONE:
            repeatImg = this.imgRepeatOne();
            break;
          case PlayerGUI.REPEAT_ALL:
            repeatImg = this.imgRepeat();
            break;
          default:
            repeatImg = this.imgRepeatOff();
            break;
        }

        if (repeatImg != null) {
          g.drawImage(repeatImg, this.repeatButtonX, this.repeatButtonY, 3);
        }

        Image shuffleImg =
            this.getGUI().getShuffleMode() ? this.imgShuffle() : this.imgShuffleOff();
        if (shuffleImg != null) {
          g.drawImage(shuffleImg, this.shuffleButtonX, this.shuffleButtonY, 3);
        }
      }
    } catch (Throwable e4) {
    }

    if (this._play) {
      this._play = false;
      this.getGUI().closePlayer();
      this.getGUI().startPlayer();
    }
  }

  public void setObserver(MainObserver _observer) {
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
        } catch (Throwable e) {
        }
      }
    } else if (c == this.prevCommand) {
      if (!this.isLoading()) {
        try {
          this.goBack = true;
          this.timeBack = System.currentTimeMillis();
          this.getGUI().getPrevSong();
        } catch (Throwable e) {
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
    } else if (c == this.repeatCommand) {
      this.getGUI().toggleRepeatMode();
    } else if (c == this.shuffleCommand) {
      this.getGUI().toggleShuffleMode();
    } else if (c == this.addToPlaylistCommand) {
      showAddToPlaylistDialog();
    } else if (c == this.showPlaylistCommand) {
      showCurrentPlaylist();
    }
  }

  private void showAddToPlaylistDialog() {
    Song currentSong = this.gui.getCurrentSong();
    if (currentSong == null) {
      return;
    }

    final Vector customPlaylists = getCustomPlaylists();

    if (customPlaylists.isEmpty()) {
      showAlert(I18N.tr("alert_no_custom_playlists"), AlertType.INFO);
      return;
    }

    final List playlistList = new List(I18N.tr("select_playlist"), List.IMPLICIT);

    for (int i = 0; i < customPlaylists.size(); i++) {
      FavoritesList.FavoriteItem item = (FavoritesList.FavoriteItem) customPlaylists.elementAt(i);
      try {
        playlistList.append(item.data.getString("name"), null);
      } catch (Exception e) {
        playlistList.append(I18N.tr("playlist") + " " + i, null);
      }
    }

    final Command cancelAddToPlaylistCommand = new Command(I18N.tr("cancel"), Command.BACK, 2);
    playlistList.addCommand(cancelAddToPlaylistCommand);

    final Song finalCurrentSong = currentSong;
    playlistList.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == List.SELECT_COMMAND) {
              final int selectedIndex = playlistList.getSelectedIndex();
              if (selectedIndex >= 0 && selectedIndex < customPlaylists.size()) {
                final FavoritesList.FavoriteItem selectedPlaylist =
                    (FavoritesList.FavoriteItem) customPlaylists.elementAt(selectedIndex);

                MIDPlay.getInstance().getDisplay().setCurrent(PlayerCanvas.this);

                ThreadManagerIntegration.executeBackgroundTask(
                    new Runnable() {
                      public void run() {
                        try {
                          addSongToCustomPlaylist(finalCurrentSong, selectedPlaylist);
                        } catch (Exception e) {
                          showAlert(e.toString(), AlertType.ERROR);
                        }
                      }
                    },
                    "AddSongToPlaylist");
              } else {
                MIDPlay.getInstance().getDisplay().setCurrent(PlayerCanvas.this);
              }
            } else if (c == cancelAddToPlaylistCommand) {
              MIDPlay.getInstance().getDisplay().setCurrent(PlayerCanvas.this);
            }
          }
        });

    MIDPlay.getInstance().getDisplay().setCurrent(playlistList);
  }

  private Vector getCustomPlaylists() {
    try {
      return favoritesManager.getCustomPlaylists();
    } catch (Exception e) {
      return new Vector();
    }
  }

  private void addSongToCustomPlaylist(final Song song, final FavoritesList.FavoriteItem playlist) {
    favoritesManager.addSongToCustomPlaylist(
        song,
        playlist,
        new FavoritesCallback() {
          public void onFavoritesLoaded(Vector favorites) {}

          public void onFavoriteRemoved() {}

          public void onCustomPlaylistCreated() {}

          public void onCustomPlaylistRenamed() {}

          public void onCustomPlaylistDeletedWithSongs() {}

          public void onCustomPlaylistSongsLoaded(Vector songs) {}

          public void onFavoriteAdded() {
            showAlert(I18N.tr("alert_song_added_to_playlist"), AlertType.CONFIRMATION);
          }

          public void onError(String message) {
            showAlert(message, AlertType.ERROR);
          }
        });
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, PlayerCanvas.this);
  }

  private void showCurrentPlaylist() {
    if (this.playlist == null || this.gui == null) {
      return;
    }

    Vector currentSongList = this.gui.getListSong();
    if (currentSongList == null || currentSongList.isEmpty()) {
      return;
    }

    int currentIndex = this.gui.getCurrentIndex();
    SongList songList = new SongList(this.playlist.getName(), currentSongList, this.playlist);
    songList.setObserver(this.observer);
    songList.setCurrentlyPlayingIndex(currentIndex);
    this.observer.go(songList);
  }

  private void createCommand() {
    this.addCommand(this.playCommand);
    this.addCommand(this.nextCommand);
    this.addCommand(this.prevCommand);
    this.addCommand(this.stopCommand);
    this.addCommand(this.addToPlaylistCommand);
    this.addCommand(this.showPlaylistCommand);

    if (this.repeatCommand == null) {
      this.repeatCommand =
          new Command(I18N.tr("repeat") + ": " + I18N.tr("all"), Command.SCREEN, 5);
      this.addCommand(this.repeatCommand);
    }

    if (this.shuffleCommand == null) {
      this.shuffleCommand =
          new Command(I18N.tr("shuffle") + ": " + I18N.tr("off"), Command.SCREEN, 6);
      this.addCommand(this.shuffleCommand);
    }
  }

  public void cancel() {
    ThreadManagerIntegration.cancelPendingDataOperations();
  }
}
