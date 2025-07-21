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
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class PlayerCanvas extends Canvas implements CommandListener, LoadDataObserver {

  private static final int PLAYER_STATUS_TOP = 2;
  private static final int SONG_TITLE_GAP = 5;
  private static final int TIME_GAP = 10;
  private static final int ART_SIZE = 72;
  private static final int ART_LEFT = 8;
  private static final int SLIDER_HEIGHT = 6;
  private static final int BUTTON_WIDTH = 40;
  private static final int BUTTON_HEIGHT = 40;
  private static final int PLAY_BUTTON_WIDTH = 50;
  private static final int PLAY_BUTTON_HEIGHT = 50;
  private static final long BUTTON_ACTIVE_DURATION = 1000L;

  private final Commands commands = new Commands();
  private final DisplayMetrics displayMetrics = new DisplayMetrics();
  private final ButtonPositions buttonPositions = new ButtonPositions();
  private final ImageCache imageCache = new ImageCache();
  private final ColorCache colorCache = new ColorCache();
  private final StatusManager statusManager = new StatusManager();

  private String title;
  private PlayerGUI gui;
  private MainObserver observer;
  private Playlist playlist;
  private boolean isPlaying = false;
  private boolean touchSupported = false;

  private final FavoritesManager favoritesManager;
  private final SleepTimerManager sleepTimerManager = SleepTimerManager.getInstance();
  private final SettingsManager settingManager = SettingsManager.getInstance();

  private Image albumArt = null;
  private String albumArtUrl = null;
  private boolean loadingAlbumArt = false;

  private long previousButtonPressTime = 0L;
  private long nextButtonPressTime = 0L;
  private boolean showingPreviousActive = false;
  private boolean showingNextActive = false;

  public PlayerCanvas(String title, Vector songList, int index, Playlist playlist) {
    this.playlist = playlist;
    this.favoritesManager = FavoritesManager.getInstance();
    this.setTitle(title);
    this.initializeCommands();
    this.setCommandListener(this);
    this.touchSupported = this.hasPointerEvents();
    this.setupSleepTimerCallback();
    this.change(title, songList, index, this.playlist);
  }

  public void showNotify() {
    String currentStatus = statusManager.current;
    if (currentStatus.equals(I18N.tr("playing")) || currentStatus.equals(I18N.tr("paused"))) {
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
    this.resetAlbumArt();
  }

  public void setupDisplay() {
    this.displayMetrics.height = -1;
    this.updateDisplay();
  }

  public String getTitle() {
    return this.title;
  }

  public void setStatus(String s) {
    statusManager.updateStatus(s);
    this.updateDisplay();
  }

  private boolean isCriticalStatus(String status) {
    return status != null
        && (status.indexOf(I18N.tr("loading")) != -1
            || status.toLowerCase().indexOf("error") != -1
            || status.indexOf(I18N.tr("volume")) != -1);
  }

  public String getStatus() {
    return statusManager.current;
  }

  private void resetAlbumArt() {
    this.albumArt = null;
    this.albumArtUrl = null;
    this.loadingAlbumArt = false;
  }

  private Display getDisplay() {
    return MIDPlay.getInstance().getDisplay();
  }

  public void restoreStatusAfterVolume(String previousStatus) {
    statusManager.original = statusManager.realPlayerStatus;

    if (sleepTimerManager.isActive()) {
      if (statusManager.realPlayerStatus.equals(I18N.tr("playing"))
          || statusManager.realPlayerStatus.equals(I18N.tr("paused"))) {
        String remaining = sleepTimerManager.getRemainingTime();
        String timerStatus = TextUtils.replace(I18N.tr("timer_remaining"), "{0}", remaining);
        statusManager.setTimerOverride(timerStatus);
      } else {
        statusManager.clearTimerOverride();
      }
    } else {
      statusManager.clearTimerOverride();
    }

    this.updateDisplay();
  }

  public void updateDisplay() {
    this.repaint();
    this.serviceRepaints();
  }

  public synchronized void close() {
    this.resetAlbumArt();
    this.imageCache.clearAll();

    if (this.gui != null) {
      this.gui.shutdown();
      this.gui.closePlayer();
      this.gui = null;
    }

    sleepTimerManager.shutdown();
    ThreadManagerIntegration.clearPlayerQueues();
  }

  public synchronized PlayerGUI getPlayerGUI() {
    if (this.gui == null) {
      this.gui = new PlayerGUI(this);
      updateRepeatCommand();
      updateShuffleCommand();
    }
    return this.gui;
  }

  public void updateRepeatCommand() {
    if (commands.repeat != null) {
      this.removeCommand(commands.repeat);
    }

    String commandText = I18N.tr("repeat") + ": ";
    if (this.gui != null) {
      switch (this.gui.getRepeatMode()) {
        case PlayerGUI.REPEAT_OFF:
          commandText += I18N.tr("off");
          break;
        case PlayerGUI.REPEAT_ONE:
          commandText += I18N.tr("one");
          break;
        case PlayerGUI.REPEAT_ALL:
          commandText += I18N.tr("all");
          break;
      }
    } else {
      commandText += I18N.tr("all");
    }

    commands.repeat = new Command(commandText, Command.SCREEN, 7);
    this.addCommand(commands.repeat);
  }

  public void updateShuffleCommand() {
    if (commands.shuffle != null) {
      this.removeCommand(commands.shuffle);
    }

    String commandText = I18N.tr("shuffle") + ": ";
    if (this.gui != null) {
      commandText += (this.gui.isShuffleMode() ? I18N.tr("on") : I18N.tr("off"));
    } else {
      commandText += I18N.tr("off");
    }

    commands.shuffle = new Command(commandText, Command.SCREEN, 8);
    this.addCommand(commands.shuffle);
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
        ART_SIZE,
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
    return statusManager.current != null && statusManager.current.indexOf(I18N.tr("loading")) != -1;
  }

  protected void pointerPressed(int x, int y) {
    if (!this.touchSupported || this.isLoading()) {
      return;
    }

    try {
      if (isPointInButton(
          x,
          y,
          buttonPositions.playX,
          buttonPositions.playY,
          PLAY_BUTTON_WIDTH,
          PLAY_BUTTON_HEIGHT)) {
        handleAction(Canvas.FIRE);
        return;
      }

      if (isPointInButton(
          x, y, buttonPositions.prevX, buttonPositions.prevY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        handleAction(Canvas.LEFT);
        return;
      }

      if (isPointInButton(
          x, y, buttonPositions.nextX, buttonPositions.nextY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        handleAction(Canvas.RIGHT);
        return;
      }

      if (isPointInButton(
          x, y, buttonPositions.repeatX, buttonPositions.repeatY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        this.gui.toggleRepeatMode();
        this.updateDisplay();
        return;
      }

      if (isPointInButton(
          x, y, buttonPositions.shuffleX, buttonPositions.shuffleY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        this.gui.toggleShuffleMode();
        this.updateDisplay();
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
        StringBuffer result = new StringBuffer(truncated);
        result.append(ellipsis);
        return result.toString();
      }
      len--;
    }

    return ellipsis;
  }

  private String timeDisplay(long us) {
    long ts = us / 100000L;
    long minutes = ts / 600L;
    long seconds = ts % 600L / 10L;

    StringBuffer timeBuffer = new StringBuffer(8);
    if (minutes < 10) {
      timeBuffer.append('0');
    }
    timeBuffer.append(minutes);
    timeBuffer.append(':');
    if (seconds < 10) {
      timeBuffer.append('0');
    }
    timeBuffer.append(seconds);
    return timeBuffer.toString();
  }

  private String formatNumber(long num, int len, boolean leadingZeros) {
    String numStr = String.valueOf(num);
    if (numStr.length() >= len) {
      return numStr;
    }

    StringBuffer ret = new StringBuffer(len);
    if (leadingZeros) {
      for (int i = numStr.length(); i < len; i++) {
        ret.append('0');
      }
      ret.append(numStr);
    } else {
      ret.append(numStr);
      for (int i = numStr.length(); i < len; i++) {
        ret.append('0');
      }
    }
    return ret.toString();
  }

  private int getThemeColor() {
    String currentThemeColor = settingManager.getThemeColor();
    if (!currentThemeColor.equals(colorCache.lastThemeColor)) {
      try {
        colorCache.themeColorRGB = Integer.parseInt(currentThemeColor, 16);
        colorCache.lastThemeColor = currentThemeColor;
      } catch (Exception e) {
        colorCache.themeColorRGB = 0x410A4A;
      }
    }
    return colorCache.themeColorRGB;
  }

  private void setThemeColor(Graphics g) {
    int color = getThemeColor();
    g.setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
  }

  private int getBackgroundColor() {
    String currentBackgroundColor = settingManager.getBackgroundColor();
    if (!currentBackgroundColor.equals(colorCache.lastBackgroundColor)) {
      try {
        colorCache.backgroundColorRGB = Integer.parseInt(currentBackgroundColor, 16);
        colorCache.lastBackgroundColor = currentBackgroundColor;
      } catch (Exception e) {
        colorCache.backgroundColorRGB = 0xF0F0F0;
      }
    }
    return colorCache.backgroundColorRGB;
  }

  private void setBackgroundColor(Graphics g) {
    int color = getBackgroundColor();
    g.setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
  }

  public void paint(Graphics g) {
    try {
      if (displayMetrics.height == -1) {
        initializeDisplayMetrics(g);
      }

      int clipY = g.getClipY();
      int clipHeight = g.getClipHeight();

      paintBackground(g);
      paintStatusBar(g, clipY, clipHeight);

      if (this.gui != null) {
        paintAlbumArt(g);
        paintSongInfo(g, clipY, clipHeight);
        paintTimeSlider(g, clipY, clipHeight);
        paintControlButtons(g);
      }
    } catch (Throwable e) {
    }

    if (this.isPlaying) {
      this.isPlaying = false;
      this.getPlayerGUI().closePlayer();
      this.getPlayerGUI().startPlayer();
    }
  }

  private void initializeDisplayMetrics(Graphics g) {
    displayMetrics.width = this.getWidth();
    displayMetrics.height = this.getHeight();
    displayMetrics.textHeight = g.getFont().getHeight();
    displayMetrics.statusBarHeight = displayMetrics.textHeight + 5;
    displayMetrics.songInfoLeft = ART_LEFT + ART_SIZE + 15;

    int currentTop = PLAYER_STATUS_TOP + displayMetrics.statusBarHeight + 15;
    displayMetrics.songNameTop = currentTop;
    currentTop += SONG_TITLE_GAP + displayMetrics.textHeight;
    displayMetrics.singerNameTop = currentTop;
    currentTop += TIME_GAP + displayMetrics.textHeight + 24;

    displayMetrics.timeRateTop = currentTop;
    displayMetrics.timeWidth = g.getFont().stringWidth("0:00:0  ");
    displayMetrics.sliderWidth = displayMetrics.width - (displayMetrics.timeWidth * 2) - 8;
    displayMetrics.sliderTop =
        displayMetrics.timeRateTop + (displayMetrics.textHeight - SLIDER_HEIGHT) / 2;
    displayMetrics.sliderLeft = displayMetrics.timeWidth + 4;
    displayMetrics.playTop = displayMetrics.timeRateTop + displayMetrics.textHeight + 24;
  }

  private void paintBackground(Graphics g) {
    setBackgroundColor(g);
    g.fillRect(0, 0, displayMetrics.width, displayMetrics.height);
  }

  private void paintStatusBar(Graphics g, int clipY, int clipHeight) {
    setThemeColor(g);
    g.fillRect(0, 0, displayMetrics.width, displayMetrics.statusBarHeight);

    if (intersects(clipY, clipHeight, PLAYER_STATUS_TOP, displayMetrics.textHeight)) {
      g.setColor(255, 255, 255);
      g.drawString(statusManager.current, displayMetrics.width >> 1, PLAYER_STATUS_TOP, 17);
    }
  }

  private void paintAlbumArt(Graphics g) {
    int artTop = PLAYER_STATUS_TOP + displayMetrics.statusBarHeight + 8;

    if (this.albumArt != null) {
      g.drawImage(
          this.albumArt,
          ART_LEFT + ART_SIZE / 2,
          artTop + ART_SIZE / 2,
          Graphics.HCENTER | Graphics.VCENTER);
      g.setColor(150, 150, 150);
      g.drawRect(ART_LEFT, artTop, ART_SIZE, ART_SIZE);
    } else {
      g.setColor(200, 200, 200);
      g.fillRect(ART_LEFT, artTop, ART_SIZE, ART_SIZE);
      g.setColor(150, 150, 150);
      g.drawRect(ART_LEFT, artTop, ART_SIZE, ART_SIZE);
      g.setColor(100, 100, 100);
      g.drawString("♪", ART_LEFT + ART_SIZE / 2, artTop + ART_SIZE / 2, 17);
    }
  }

  private void paintSongInfo(Graphics g, int clipY, int clipHeight) {
    if (intersects(clipY, clipHeight, displayMetrics.songNameTop, displayMetrics.textHeight)) {
      setThemeColor(g);
      int maxSongNameWidth = displayMetrics.width - displayMetrics.songInfoLeft - 10;
      String truncatedSongName = truncateText(this.gui.getSongName(), g, maxSongNameWidth);
      g.drawString(truncatedSongName, displayMetrics.songInfoLeft, displayMetrics.songNameTop, 20);
    }

    if (intersects(clipY, clipHeight, displayMetrics.singerNameTop, displayMetrics.textHeight)) {
      g.setColor(140, 140, 140);
      int maxSingerWidth = displayMetrics.width - displayMetrics.songInfoLeft - 10;
      String truncatedSinger = truncateText(this.gui.getSinger(), g, maxSingerWidth);
      g.drawString(truncatedSinger, displayMetrics.songInfoLeft, displayMetrics.singerNameTop, 20);
    }
  }

  private void paintTimeSlider(Graphics g, int clipY, int clipHeight) {
    long duration = this.gui.getDuration();
    long current = this.gui.getCurrentTime();
    String strDuration = timeDisplay(duration);
    String strCurrent = timeDisplay(current);

    if (duration > 0) {
      displayMetrics.sliderValue =
          (float) displayMetrics.sliderWidth * ((float) current / (float) duration);
    } else {
      displayMetrics.sliderValue = 0;
    }

    if (intersects(clipY, clipHeight, displayMetrics.timeRateTop, displayMetrics.textHeight)) {
      g.setColor(140, 140, 140);
      g.drawString(strCurrent, 5, displayMetrics.timeRateTop, 20);
    }

    g.setColor(220, 220, 220);
    g.fillRect(
        displayMetrics.sliderLeft,
        displayMetrics.sliderTop,
        displayMetrics.sliderWidth,
        SLIDER_HEIGHT);

    setThemeColor(g);
    g.fillRect(
        displayMetrics.sliderLeft,
        displayMetrics.sliderTop,
        (int) displayMetrics.sliderValue,
        SLIDER_HEIGHT);

    if (intersects(clipY, clipHeight, displayMetrics.timeRateTop, displayMetrics.textHeight)) {
      g.setColor(140, 140, 140);
      g.drawString(strDuration, displayMetrics.width - 5, displayMetrics.timeRateTop, 24);
    }
  }

  private void paintControlButtons(Graphics g) {
    initializeButtonPositions();
    updateButtonActiveStates();

    boolean isPlaying = this.gui.isPlaying();
    Image playPauseImg = isPlaying ? imageCache.getPauseImage() : imageCache.getPlayImage();
    if (playPauseImg != null) {
      g.drawImage(playPauseImg, buttonPositions.playX, buttonPositions.playY, 3);
    }

    Image prevImg =
        showingPreviousActive ? imageCache.getPreviousActiveImage() : imageCache.getPreviousImage();
    if (prevImg != null) {
      g.drawImage(prevImg, buttonPositions.prevX, buttonPositions.prevY, 3);
    }

    Image nextImg = showingNextActive ? imageCache.getNextActiveImage() : imageCache.getNextImage();
    if (nextImg != null) {
      g.drawImage(nextImg, buttonPositions.nextX, buttonPositions.nextY, 3);
    }

    Image repeatImg = getRepeatModeImage();
    if (repeatImg != null) {
      g.drawImage(repeatImg, buttonPositions.repeatX, buttonPositions.repeatY, 3);
    }

    Image shuffleImg =
        this.getPlayerGUI().isShuffleMode()
            ? imageCache.getShuffleImage()
            : imageCache.getShuffleOffImage();
    if (shuffleImg != null) {
      g.drawImage(shuffleImg, buttonPositions.shuffleX, buttonPositions.shuffleY, 3);
    }
  }

  private void initializeButtonPositions() {
    if (buttonPositions.playX == 0) {
      int screenCenter = displayMetrics.width >> 1;
      int buttonGap = displayMetrics.width / 5;
      int margin = 8;

      buttonPositions.playX = screenCenter;
      buttonPositions.playY = displayMetrics.playTop;
      buttonPositions.prevX = screenCenter - buttonGap;
      buttonPositions.prevY = displayMetrics.playTop;
      buttonPositions.nextX = screenCenter + buttonGap;
      buttonPositions.nextY = displayMetrics.playTop;
      buttonPositions.repeatX = margin + (BUTTON_WIDTH / 2);
      buttonPositions.repeatY = displayMetrics.playTop;
      buttonPositions.shuffleX = displayMetrics.width - margin - (BUTTON_WIDTH / 2);
      buttonPositions.shuffleY = displayMetrics.playTop;
    }
  }

  private void updateButtonActiveStates() {
    if (showingPreviousActive || showingNextActive) {
      long time = System.currentTimeMillis();
      if (time - previousButtonPressTime >= BUTTON_ACTIVE_DURATION) {
        showingPreviousActive = false;
      }
      if (time - nextButtonPressTime >= BUTTON_ACTIVE_DURATION) {
        showingNextActive = false;
      }
    }
  }

  private Image getRepeatModeImage() {
    switch (this.getPlayerGUI().getRepeatMode()) {
      case PlayerGUI.REPEAT_ONE:
        return imageCache.getRepeatOneImage();
      case PlayerGUI.REPEAT_ALL:
        return imageCache.getRepeatImage();
      default:
        return imageCache.getRepeatOffImage();
    }
  }

  public void setObserver(MainObserver observer) {
    this.observer = observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == commands.back) {
      this.observer.goBack();
    } else if (c == commands.next) {
      handleNextCommand();
    } else if (c == commands.prev) {
      handlePrevCommand();
    } else if (c == commands.playPause) {
      if (!this.isLoading()) {
        this.getPlayerGUI().togglePlayer();
      }
    } else if (c == commands.stop) {
      this.gui.pausePlayer();
      this.gui.setMediaTime(0L);
      this.setStatus(I18N.tr("paused"));
    } else if (c == commands.repeat) {
      this.getPlayerGUI().toggleRepeatMode();
    } else if (c == commands.shuffle) {
      this.getPlayerGUI().toggleShuffleMode();
    } else if (c == commands.addToPlaylist) {
      showAddToPlaylistDialog();
    } else if (c == commands.showPlaylist) {
      showCurrentPlaylist();
    } else if (c == commands.sleepTimer) {
      showSleepTimerDialog();
    } else if (c == commands.cancelTimer) {
      sleepTimerManager.cancelTimer();
    }
  }

  private void handleNextCommand() {
    if (!this.isLoading()) {
      try {
        this.showingNextActive = true;
        this.nextButtonPressTime = System.currentTimeMillis();
        this.getPlayerGUI().getNextSong();
      } catch (Throwable e) {
      }
    }
  }

  private void handlePrevCommand() {
    if (!this.isLoading()) {
      try {
        this.showingPreviousActive = true;
        this.previousButtonPressTime = System.currentTimeMillis();
        this.getPlayerGUI().getPrevSong();
      } catch (Throwable e) {
      }
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

    final Command cancelAddToPlaylistCommand = new Command(I18N.tr("cancel"), Command.BACK, 1);
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

                getDisplay().setCurrent(PlayerCanvas.this);

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
                getDisplay().setCurrent(PlayerCanvas.this);
              }
            } else if (c == cancelAddToPlaylistCommand) {
              getDisplay().setCurrent(PlayerCanvas.this);
            }
          }
        });

    getDisplay().setCurrent(playlistList);
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
    getDisplay().setCurrent(alert, PlayerCanvas.this);
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

  private void initializeCommands() {
    commands.playPause = new Command(I18N.tr("play"), Command.OK, 1);
    commands.next = new Command(I18N.tr("next"), Command.SCREEN, 2);
    commands.prev = new Command(I18N.tr("previous"), Command.SCREEN, 3);
    commands.stop = new Command(I18N.tr("stop"), Command.SCREEN, 4);
    commands.addToPlaylist = new Command(I18N.tr("add_to_playlist"), Command.SCREEN, 5);
    commands.showPlaylist = new Command(I18N.tr("show_playlist"), Command.SCREEN, 6);
    commands.sleepTimer = new Command(I18N.tr("sleep_timer"), Command.SCREEN, 9);
    commands.back = new Command(I18N.tr("back"), Command.BACK, 11);

    this.addCommand(commands.back);
    this.addCommand(commands.playPause);
    this.addCommand(commands.next);
    this.addCommand(commands.prev);
    this.addCommand(commands.stop);
    this.addCommand(commands.addToPlaylist);
    this.addCommand(commands.showPlaylist);
    updateTimerCommands();

    if (commands.repeat == null) {
      commands.repeat = new Command(I18N.tr("repeat") + ": " + I18N.tr("all"), Command.SCREEN, 7);
      this.addCommand(commands.repeat);
    }

    if (commands.shuffle == null) {
      commands.shuffle = new Command(I18N.tr("shuffle") + ": " + I18N.tr("off"), Command.SCREEN, 8);
      this.addCommand(commands.shuffle);
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
            statusManager.clearTimerOverride();
            if (action == SleepTimerForm.ACTION_STOP_PLAYBACK) {
              if (gui != null) {
                gui.pausePlayer();
                setStatus(I18N.tr("paused"));
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
            statusManager.clearTimerOverride();
            setStatus(statusManager.original);
            updateTimerCommands();
          }
        });
  }

  private void updateTimerCommands() {
    if (commands.cancelTimer != null) {
      this.removeCommand(commands.cancelTimer);
      commands.cancelTimer = null;
    }

    if (sleepTimerManager.isActive()) {
      this.removeCommand(commands.sleepTimer);
      commands.cancelTimer = new Command(I18N.tr("cancel_timer"), Command.SCREEN, 10);
      this.addCommand(commands.cancelTimer);
    } else {
      this.addCommand(commands.sleepTimer);
    }
  }

  private void updateSleepTimerDisplay() {
    if (sleepTimerManager.isActive()) {
      String remaining = sleepTimerManager.getRemainingTime();
      String timerStatus = TextUtils.replace(I18N.tr("timer_remaining"), "{0}", remaining);

      if (statusManager.original.equals(I18N.tr("playing"))
          || statusManager.original.equals(I18N.tr("paused"))) {
        boolean wasActive = statusManager.isTimerOverrideActive();
        String previousStatus = statusManager.current;

        statusManager.setTimerOverride(timerStatus);

        if (!wasActive || !timerStatus.equals(previousStatus)) {
          updateDisplay();
        }
      }
    } else {
      if (statusManager.isTimerOverrideActive()) {
        statusManager.clearTimerOverride();
        updateDisplay();
      }
    }
  }

  private boolean isVolumeStatusShowing() {
    return statusManager.current != null && statusManager.current.indexOf(I18N.tr("volume")) != -1;
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

      if (!isCriticalStatus(statusManager.current)) {
        statusManager.setTimerOverride(timerStatus);
        updateDisplay();
      }
    } else {
      if (statusManager.isTimerOverrideActive()) {
        statusManager.clearTimerOverride();
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

  private static class Commands {
    Command back, playPause, next, prev, stop;
    Command addToPlaylist, showPlaylist, sleepTimer, cancelTimer;
    Command repeat, shuffle;
  }

  private static class DisplayMetrics {
    int width = -1, height = -1, textHeight = 10;
    int songInfoLeft = 0, statusBarHeight = 0;
    int songNameTop = 0, singerNameTop = 0;
    int timeRateTop = 0, timeWidth = 0, playTop = 0;
    int sliderTop = 0, sliderLeft = 0, sliderWidth = 12;
    float sliderValue = 0.0F;
  }

  private static class ButtonPositions {
    int playX = 0, playY = 0;
    int prevX = 0, prevY = 0;
    int nextX = 0, nextY = 0;
    int repeatX = 0, repeatY = 0;
    int shuffleX = 0, shuffleY = 0;
  }

  private static class ColorCache {
    int themeColorRGB, backgroundColorRGB;
    String lastThemeColor = "", lastBackgroundColor = "";
  }

  private static class StatusManager {
    String current = "", original = "", realPlayerStatus = "";
    boolean timerOverrideActive = false;

    void updateStatus(String s) {
      if (s != null
          && s.indexOf(I18N.tr("timer_remaining").substring(0, 3)) == -1
          && s.indexOf(I18N.tr("volume")) == -1) {
        this.realPlayerStatus = s;
      }

      if (!timerOverrideActive) {
        this.current = s;
        this.original = s;
      } else if (isCriticalStatus(s)) {
        this.current = s;
        this.original = s;
        this.timerOverrideActive = false;
      }
    }

    private boolean isCriticalStatus(String status) {
      return status != null
          && (status.indexOf(I18N.tr("loading")) != -1
              || status.toLowerCase().indexOf("error") != -1
              || status.indexOf(I18N.tr("volume")) != -1);
    }

    void setTimerOverride(String timerStatus) {
      this.timerOverrideActive = true;
      this.current = timerStatus;
    }

    void clearTimerOverride() {
      this.timerOverrideActive = false;
      this.current = this.realPlayerStatus;
    }

    boolean isTimerOverrideActive() {
      return timerOverrideActive;
    }
  }

  private static class ImageCache {
    Image play, pause, previous, next;
    Image previousActive, nextActive;
    Image repeat, repeatOne, repeatOff;
    Image shuffle, shuffleOff;

    Image getPlayImage() {
      if (play == null) {
        try {
          play = Image.createImage("/images/player/play.png");
        } catch (Exception e) {
          play = null;
        }
      }
      return play;
    }

    Image getPauseImage() {
      if (pause == null) {
        try {
          pause = Image.createImage("/images/player/pause.png");
        } catch (Exception e) {
          pause = null;
        }
      }
      return pause;
    }

    Image getPreviousImage() {
      if (previous == null) {
        try {
          previous = Image.createImage("/images/player/previous.png");
        } catch (Exception e) {
          previous = null;
        }
      }
      return previous;
    }

    Image getNextImage() {
      if (next == null) {
        try {
          next = Image.createImage("/images/player/next.png");
        } catch (Exception e) {
          next = null;
        }
      }
      return next;
    }

    Image getPreviousActiveImage() {
      if (previousActive == null) {
        try {
          previousActive = Image.createImage("/images/player/previous.png");
        } catch (Exception e) {
          previousActive = null;
        }
      }
      return previousActive;
    }

    Image getNextActiveImage() {
      if (nextActive == null) {
        try {
          nextActive = Image.createImage("/images/player/next.png");
        } catch (Exception e) {
          nextActive = null;
        }
      }
      return nextActive;
    }

    Image getRepeatImage() {
      if (repeat == null) {
        try {
          repeat = Image.createImage("/images/player/repeat.png");
        } catch (Exception e) {
          repeat = null;
        }
      }
      return repeat;
    }

    Image getRepeatOneImage() {
      if (repeatOne == null) {
        try {
          repeatOne = Image.createImage("/images/player/repeat-one.png");
        } catch (Exception e) {
          repeatOne = null;
        }
      }
      return repeatOne;
    }

    Image getRepeatOffImage() {
      if (repeatOff == null) {
        try {
          repeatOff = Image.createImage("/images/player/repeat-off.png");
        } catch (Exception e) {
          repeatOff = null;
        }
      }
      return repeatOff;
    }

    Image getShuffleImage() {
      if (shuffle == null) {
        try {
          shuffle = Image.createImage("/images/player/shuffle.png");
        } catch (Exception e) {
          shuffle = null;
        }
      }
      return shuffle;
    }

    Image getShuffleOffImage() {
      if (shuffleOff == null) {
        try {
          shuffleOff = Image.createImage("/images/player/shuffle-off.png");
        } catch (Exception e) {
          shuffleOff = null;
        }
      }
      return shuffleOff;
    }

    void clearAll() {
      play = pause = previous = next = null;
      previousActive = nextActive = null;
      repeat = repeatOne = repeatOff = null;
      shuffle = shuffleOff = null;
    }
  }
}
