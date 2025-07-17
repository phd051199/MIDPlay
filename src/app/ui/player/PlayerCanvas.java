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
import app.utils.TextUtils;
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

public class PlayerCanvas extends Canvas implements CommandListener, LoadDataObserver {

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
  private final Command sleepTimerCommand = new Command(I18N.tr("sleep_timer"), Command.SCREEN, 10);
  private Command cancelTimerCommand;
  private Command repeatCommand;
  private Command shuffleCommand;

  private String title;
  private PlayerGUI gui;
  private MainObserver parent;
  private String status = "";
  private final FavoritesManager favoritesManager;
  private final SleepTimerManager sleepTimerManager = SleepTimerManager.getInstance();
  private String originalStatus = "";
  private String realPlayerStatus = "";
  private boolean timerOverrideActive = false;

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
  private boolean isPlaying = false;

  private Image playImage = null;
  private Image pauseImage = null;
  private Image previousImage = null;
  private Image nextImage = null;
  private Image previousActiveImage = null;
  private Image nextActiveImage = null;
  private Image repeatImage = null;
  private Image repeatOneImage = null;
  private Image repeatOffImage = null;
  private Image shuffleImage = null;
  private Image shuffleOffImage = null;

  private Image albumArt = null;
  private String albumArtUrl = null;
  private boolean loadingAlbumArt = false;

  private long previousButtonPressTime = 0L;
  private long nextButtonPressTime = 0L;
  private boolean showingPreviousActive = false;
  private boolean showingNextActive = false;

  private final SettingsManager settingManager = SettingsManager.getInstance();
  private int cachedThemeColorRGB;
  private int cachedBackgroundColorRGB;
  private String lastThemeColor = "";
  private String lastBackgroundColor = "";

  public PlayerCanvas(String title, Vector songList, int index, Playlist playlist) {
    this.playlist = playlist;
    this.favoritesManager = FavoritesManager.getInstance();
    this.setTitle(title);
    this.addCommand(this.backCommand);
    this.setCommandListener(this);
    this.createCommand();

    this.touchSupported = this.hasPointerEvents();

    this.setupSleepTimerCallback();
    this.change(title, songList, index, this.playlist);
  }

  public void showNotify() {
    if (this.status.equals(I18N.tr("playing")) || this.status.equals(I18N.tr("paused"))) {
      if (this.getPlayerGUI().isPlaying()) {
        this.setStatus(I18N.tr("playing"));
      } else {
        this.setStatus(I18N.tr("paused"));
      }
    }
  }

  public void change(String title, Vector songList, int index, Playlist playlist) {
    this.title = title;
    this.setTitle(title);
    this.playlist = playlist;
    this.getPlayerGUI().setListSong(songList, index);
    this.isPlaying = true;

    this.albumArt = null;
    this.albumArtUrl = null;
    this.loadingAlbumArt = false;
  }

  public void setupDisplay() {
    this.displayHeight = -1;
    this.updateDisplay();
  }

  public String getTitle() {
    return this.title;
  }

  public void setStatus(String s) {

    if (s != null
        && s.indexOf(I18N.tr("timer_remaining").substring(0, 3)) == -1
        && s.indexOf(I18N.tr("volume")) == -1) {
      this.realPlayerStatus = s;
    }

    if (!timerOverrideActive) {
      this.status = s;
      this.originalStatus = s;
    } else if (isCriticalStatus(s)) {
      this.status = s;
      this.originalStatus = s;
      this.timerOverrideActive = false;
    }
    this.updateDisplay();
  }

  private boolean isCriticalStatus(String status) {
    return status != null
        && (status.indexOf(I18N.tr("loading")) != -1
            || status.toLowerCase().indexOf("error") != -1
            || status.indexOf(I18N.tr("volume")) != -1);
  }

  public String getStatus() {
    return this.status;
  }

  public void restoreStatusAfterVolume(String previousStatus) {

    this.originalStatus = this.realPlayerStatus;

    if (sleepTimerManager.isActive()) {
      if (realPlayerStatus.equals(I18N.tr("playing"))
          || realPlayerStatus.equals(I18N.tr("paused"))) {
        String remaining = sleepTimerManager.getRemainingTime();
        String timerStatus = TextUtils.replace(I18N.tr("timer_remaining"), "{0}", remaining);
        this.timerOverrideActive = true;
        this.status = timerStatus;
      } else {
        this.timerOverrideActive = false;
        this.status = realPlayerStatus;
      }
    } else {
      this.timerOverrideActive = false;
      this.status = realPlayerStatus;
    }

    this.updateDisplay();
  }

  public void updateDisplay() {
    this.repaint();
    this.serviceRepaints();
  }

  public synchronized void close() {
    this.albumArt = null;
    this.albumArtUrl = null;
    this.loadingAlbumArt = false;

    if (this.gui != null) {
      this.gui.shutdown();
      this.gui.closePlayer();
      this.gui = null;
    }

    sleepTimerManager.shutdown();
    ThreadManagerIntegration.clearPlayerQueues();
  }

  private synchronized PlayerGUI getPlayerGUI() {
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
          I18N.tr("shuffle") + ": " + (this.gui.isShuffleMode() ? I18N.tr("on") : I18N.tr("off"));
    } else {
      commandText = I18N.tr("shuffle") + ": " + I18N.tr("off");
    }

    this.shuffleCommand = new Command(commandText, Command.SCREEN, 6);
    this.addCommand(this.shuffleCommand);
  }

  public void setAlbumArtUrl(String url) {
    if (url != null && !url.equals(this.albumArtUrl)) {
      this.albumArtUrl = url;
      this.albumArt = null;
      this.loadingAlbumArt = false;
      loadAlbumArt();
    }
  }

  private void loadAlbumArt() {
    if (this.albumArtUrl == null || this.loadingAlbumArt) {
      return;
    }

    this.loadingAlbumArt = true;

    final String imageUrl = this.albumArtUrl;
    ThreadManagerIntegration.executePlayerImageLoading(
        imageUrl,
        72,
        new ThreadManagerIntegration.ImageLoadCallback() {
          public void onImageLoaded(Object image) {
            albumArt = (javax.microedition.lcdui.Image) image;
            if (albumArt != null) {
              updateDisplay();
            }
            loadingAlbumArt = false;
          }

          public void onImageLoadError(Exception e) {
            albumArt = null;
            loadingAlbumArt = false;
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

  private boolean isLoading() {
    return this.status != null && this.status.indexOf(I18N.tr("loading")) != -1;
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
          this.showingNextActive = true;
          this.nextButtonPressTime = System.currentTimeMillis();
          this.gui.getNextSong();
          break;
        case Canvas.LEFT:
          this.showingPreviousActive = true;
          this.previousButtonPressTime = System.currentTimeMillis();
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

  public Image getPlayImage() {
    if (this.playImage == null) {
      try {
        this.playImage = Image.createImage("/images/player/play.png");
      } catch (Exception e) {
        this.playImage = null;
      }
    }
    return this.playImage;
  }

  public Image getPauseImage() {
    if (this.pauseImage == null) {
      try {
        this.pauseImage = Image.createImage("/images/player/pause.png");
      } catch (Exception e) {
        this.pauseImage = null;
      }
    }
    return this.pauseImage;
  }

  public Image getPreviousImage() {
    if (this.previousImage == null) {
      try {
        this.previousImage = Image.createImage("/images/player/previous.png");
      } catch (Exception e) {
        this.previousImage = null;
      }
    }
    return this.previousImage;
  }

  public Image getNextImage() {
    if (this.nextImage == null) {
      try {
        this.nextImage = Image.createImage("/images/player/next.png");
      } catch (Exception e) {
        this.nextImage = null;
      }
    }
    return this.nextImage;
  }

  public Image getPreviousActiveImage() {
    if (this.previousActiveImage == null) {
      try {
        this.previousActiveImage = Image.createImage("/images/player/previous.png");
      } catch (Exception e) {
        this.previousActiveImage = null;
      }
    }
    return this.previousActiveImage;
  }

  public Image getNextActiveImage() {
    if (this.nextActiveImage == null) {
      try {
        this.nextActiveImage = Image.createImage("/images/player/next.png");
      } catch (Exception e) {
        this.nextActiveImage = null;
      }
    }
    return this.nextActiveImage;
  }

  public Image getRepeatImage() {
    if (this.repeatImage == null) {
      try {
        this.repeatImage = Image.createImage("/images/player/repeat.png");
      } catch (Exception e) {
        this.repeatImage = null;
      }
    }
    return this.repeatImage;
  }

  public Image getRepeatOneImage() {
    if (this.repeatOneImage == null) {
      try {
        this.repeatOneImage = Image.createImage("/images/player/repeat-one.png");
      } catch (Exception e) {
        this.repeatOneImage = null;
      }
    }
    return this.repeatOneImage;
  }

  public Image getRepeatOffImage() {
    if (this.repeatOffImage == null) {
      try {
        this.repeatOffImage = Image.createImage("/images/player/repeat-off.png");
      } catch (Exception e) {
        this.repeatOffImage = null;
      }
    }
    return this.repeatOffImage;
  }

  public Image getShuffleImage() {
    if (this.shuffleImage == null) {
      try {
        this.shuffleImage = Image.createImage("/images/player/shuffle.png");
      } catch (Exception e) {
        this.shuffleImage = null;
      }
    }
    return this.shuffleImage;
  }

  public Image getShuffleOffImage() {
    if (this.shuffleOffImage == null) {
      try {
        this.shuffleOffImage = Image.createImage("/images/player/shuffle-off.png");
      } catch (Exception e) {
        this.shuffleOffImage = null;
      }
    }
    return this.shuffleOffImage;
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

        if (this.albumArt != null) {
          g.drawImage(
              this.albumArt,
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

        boolean isPlaying = this.gui.isPlaying();
        Image playPauseImg = isPlaying ? this.getPauseImage() : this.getPlayImage();

        if (playPauseImg != null) {
          g.drawImage(playPauseImg, this.playButtonX, this.playButtonY, 3);
        }

        if (this.showingPreviousActive || this.showingNextActive) {
          long time = System.currentTimeMillis();
          if (time - this.previousButtonPressTime >= 1000L) {
            this.showingPreviousActive = false;
          }
          if (time - this.nextButtonPressTime >= 1000L) {
            this.showingNextActive = false;
          }
        }

        Image prevImg =
            this.showingPreviousActive ? this.getPreviousActiveImage() : this.getPreviousImage();
        if (prevImg != null) {
          g.drawImage(prevImg, this.prevButtonX, this.prevButtonY, 3);
        }

        Image nextImg = this.showingNextActive ? this.getNextActiveImage() : this.getNextImage();
        if (nextImg != null) {
          g.drawImage(nextImg, this.nextButtonX, this.nextButtonY, 3);
        }

        Image repeatImg = null;
        switch (this.getPlayerGUI().getRepeatMode()) {
          case PlayerGUI.REPEAT_ONE:
            repeatImg = this.getRepeatOneImage();
            break;
          case PlayerGUI.REPEAT_ALL:
            repeatImg = this.getRepeatImage();
            break;
          default:
            repeatImg = this.getRepeatOffImage();
            break;
        }

        if (repeatImg != null) {
          g.drawImage(repeatImg, this.repeatButtonX, this.repeatButtonY, 3);
        }

        Image shuffleImg =
            this.getPlayerGUI().isShuffleMode() ? this.getShuffleImage() : this.getShuffleOffImage();
        if (shuffleImg != null) {
          g.drawImage(shuffleImg, this.shuffleButtonX, this.shuffleButtonY, 3);
        }
      }
    } catch (Throwable e4) {
    }

    if (this.isPlaying) {
      this.isPlaying = false;
      this.getPlayerGUI().closePlayer();
      this.getPlayerGUI().startPlayer();
    }
  }

  public void setObserver(MainObserver observer) {
    this.observer = observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.backCommand) {
      this.observer.goBack();
    } else if (c == this.nextCommand) {
      if (!this.isLoading()) {
        try {
          this.showingNextActive = true;
          this.nextButtonPressTime = System.currentTimeMillis();
          this.getPlayerGUI().getNextSong();
        } catch (Throwable e) {
        }
      }
    } else if (c == this.prevCommand) {
      if (!this.isLoading()) {
        try {
          this.showingPreviousActive = true;
          this.previousButtonPressTime = System.currentTimeMillis();
          this.getPlayerGUI().getPrevSong();
        } catch (Throwable e) {
        }
      }
    } else if (c == this.playCommand || c == this.pauseCommand) {
      if (!this.isLoading()) {
        this.getPlayerGUI().togglePlayer();
      }
    } else if (c == this.stopCommand) {
      this.gui.pausePlayer();
      this.gui.setMediaTime(0L);
      this.setStatus(I18N.tr("paused"));
    } else if (c == this.repeatCommand) {
      this.getPlayerGUI().toggleRepeatMode();
    } else if (c == this.shuffleCommand) {
      this.getPlayerGUI().toggleShuffleMode();
    } else if (c == this.addToPlaylistCommand) {
      showAddToPlaylistDialog();
    } else if (c == this.showPlaylistCommand) {
      showCurrentPlaylist();
    } else if (c == this.sleepTimerCommand) {
      showSleepTimerDialog();
    } else if (c == this.cancelTimerCommand) {
      sleepTimerManager.cancelTimer();
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
    updateTimerCommands();

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

  private void showSleepTimerDialog() {
    SleepTimerForm sleepTimerForm =
        new SleepTimerForm(
            this.observer,
            new SleepTimerForm.SleepTimerCallback() {
              public void onTimerSet(
                  final int mode,
                  final int durationMinutes,
                  final int targetHour,
                  final int targetMinute,
                  final int action) {

                observer.goBack();

                ThreadManagerIntegration.executeBackgroundTask(
                    new Runnable() {
                      public void run() {
                        try {
                          if (mode == SleepTimerForm.MODE_COUNTDOWN) {
                            sleepTimerManager.startCountdownTimer(durationMinutes, action);
                            final String message =
                                TextUtils.replace(
                                    I18N.tr("timer_set_success"),
                                    "{0}",
                                    durationMinutes + " " + I18N.tr("minutes"));

                            ThreadManagerIntegration.executeUITask(
                                new Runnable() {
                                  public void run() {
                                    showAlert(message, AlertType.CONFIRMATION);
                                    updateTimerCommands();
                                    updateSleepTimerStatus();
                                  }
                                },
                                "TimerSetupUI");
                          } else {
                            sleepTimerManager.startAbsoluteTimer(targetHour, targetMinute, action);
                            final String timeStr =
                                (targetHour < 10 ? "0" : "")
                                    + targetHour
                                    + ":"
                                    + (targetMinute < 10 ? "0" : "")
                                    + targetMinute;
                            final String message =
                                TextUtils.replace(I18N.tr("timer_set_success"), "{0}", timeStr);

                            ThreadManagerIntegration.executeUITask(
                                new Runnable() {
                                  public void run() {
                                    showAlert(message, AlertType.CONFIRMATION);
                                    updateTimerCommands();
                                    updateSleepTimerStatus();
                                  }
                                },
                                "TimerSetupUI");
                          }
                        } catch (Exception e) {
                          ThreadManagerIntegration.executeUITask(
                              new Runnable() {
                                public void run() {
                                  showAlert(I18N.tr("error"), AlertType.ERROR);
                                }
                              },
                              "TimerSetupError");
                        }
                      }
                    },
                    "SleepTimerSetup");
              }
            });
    this.observer.go(sleepTimerForm);
  }

  private void setupSleepTimerCallback() {
    sleepTimerManager.setCallback(
        new SleepTimerManager.SleepTimerCallback() {
          public void onTimerExpired(int action) {
            timerOverrideActive = false;
            if (action == SleepTimerForm.ACTION_STOP_PLAYBACK) {
              if (gui != null) {
                gui.pausePlayer();
                setStatus(I18N.tr("pause"));
              }
            } else if (action == SleepTimerForm.ACTION_EXIT_APP) {
              setStatus(I18N.tr("timer_expired_exit"));
            }
            updateTimerCommands();
          }

          public void onTimerUpdate(String remainingTime) {

            if (!isVolumeStatusShowing()) {
              updateSleepTimerDisplay();
            } else {

              scheduleDelayedTimerUpdate();
            }
          }

          public void onTimerCancelled() {
            timerOverrideActive = false;
            setStatus(originalStatus);
            updateTimerCommands();
          }
        });
  }

  private void updateTimerCommands() {
    if (this.cancelTimerCommand != null) {
      this.removeCommand(this.cancelTimerCommand);
      this.cancelTimerCommand = null;
    }

    if (sleepTimerManager.isActive()) {
      this.removeCommand(this.sleepTimerCommand);
      this.cancelTimerCommand = new Command(I18N.tr("cancel_timer"), Command.SCREEN, 11);
      this.addCommand(this.cancelTimerCommand);
    } else {
      this.addCommand(this.sleepTimerCommand);
    }
  }

  private void updateSleepTimerDisplay() {
    if (sleepTimerManager.isActive()) {
      String remaining = sleepTimerManager.getRemainingTime();
      String timerStatus = TextUtils.replace(I18N.tr("timer_remaining"), "{0}", remaining);

      if (originalStatus.equals(I18N.tr("playing")) || originalStatus.equals(I18N.tr("paused"))) {
        boolean wasActive = timerOverrideActive;
        String previousStatus = status;

        timerOverrideActive = true;
        status = timerStatus;

        if (!wasActive || !timerStatus.equals(previousStatus)) {
          updateDisplay();
        }
      }
    } else {
      if (timerOverrideActive) {
        timerOverrideActive = false;
        status = originalStatus;
        updateDisplay();
      }
    }
  }

  private boolean isVolumeStatusShowing() {
    return status != null && status.indexOf(I18N.tr("volume")) != -1;
  }

  private void scheduleDelayedTimerUpdate() {

    ThreadManagerIntegration.scheduleDelayedTask(
        new Runnable() {
          public void run() {
            ThreadManagerIntegration.executeUITask(
                new Runnable() {
                  public void run() {
                    if (sleepTimerManager.isActive() && !isVolumeStatusShowing()) {
                      updateSleepTimerDisplay();
                    }
                  }
                },
                "DelayedTimerUpdate");
          }
        },
        "DelayedTimerUpdateScheduler",
        1200);
  }

  private void forceUpdateSleepTimerDisplay() {
    if (sleepTimerManager.isActive()) {
      String remaining = sleepTimerManager.getRemainingTime();
      String timerStatus = TextUtils.replace(I18N.tr("timer_remaining"), "{0}", remaining);

      if (!isCriticalStatus(status)) {
        timerOverrideActive = true;
        status = timerStatus;
        updateDisplay();
      }
    } else {
      if (timerOverrideActive) {
        timerOverrideActive = false;
        status = originalStatus;
        updateDisplay();
      }
    }
  }

  private void updateSleepTimerStatus() {
    updateSleepTimerDisplay();
    updateTimerCommands();
  }

  public void cancel() {
    ThreadManagerIntegration.cancelPendingDataOperations();
  }
}
