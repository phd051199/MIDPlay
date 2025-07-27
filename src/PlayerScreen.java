import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import model.Track;
import model.Tracks;

public final class PlayerScreen extends Canvas
    implements CommandListener, SleepTimerManager.SleepTimerCallback {
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
  private static final int THEME_COLOR = 0x410A4A;
  private static final int BACKGROUND_COLOR = 0xF0F0F0;

  private String title;
  private PlayerGUI gui;
  private Navigator navigator;
  private boolean touchSupported = false;
  private String currentStatusKey = "player.status.stopped";
  private final SettingsManager settingsManager = SettingsManager.getInstance();
  private Image albumArt = null;
  private String albumArtUrl = null;
  private boolean loadingAlbumArt = false;
  private ImageLoadOperation currentImageLoadOperation = null;
  private long previousButtonPressTime = 0L;
  private long nextButtonPressTime = 0L;
  private boolean showingPreviousActive = false;
  private boolean showingNextActive = false;
  private boolean volumeAlertShowing = false;
  private int currentVolumeLevel = 0;
  private final SleepTimerManager sleepTimerManager;

  private int displayWidth = -1, displayHeight = -1, textHeight = 10;
  private int trackInfoLeft = 0, statusBarHeight = 0;
  private int trackNameTop = 0, singerNameTop = 0;
  private int timeRateTop = 0, timeWidth = 0, playTop = 0;
  private int sliderTop = 0, sliderLeft = 0, sliderWidth = 12;
  private float sliderValue = 0.0F;

  private int playX = 0, playY = 0;
  private int prevX = 0, prevY = 0;
  private int nextX = 0, nextY = 0;
  private int repeatX = 0, repeatY = 0;
  private int shuffleX = 0, shuffleY = 0;
  private String statusCurrent = "", statusOriginal = "", realPlayerStatus = "";
  private boolean timerOverrideActive = false;

  public PlayerScreen(String title, Tracks tracks, int index, Navigator navigator) {
    this.navigator = navigator;
    this.sleepTimerManager = new SleepTimerManager();
    this.setTitle(title);
    this.addCommands();
    this.setCommandListener(this);
    this.touchSupported = this.hasPointerEvents();
    this.change(title, tracks, index, navigator);
  }

  public void addCommands() {
    this.addCommand(Commands.back());
    this.addCommand(Commands.playerPlay());
    this.addCommand(Commands.playerNext());
    this.addCommand(Commands.playerPrevious());
    this.addCommand(Commands.playerStop());
    this.addCommand(Commands.playerVolume());
    this.addCommand(Commands.playerAddToPlaylist());
    this.addCommand(Commands.playerShowPlaylist());
    this.addCommand(Commands.playerRepeat());
    this.addCommand(Commands.playerShuffle());
    updateSleepTimerCommands();
  }

  public void clearCommands() {
    this.removeCommand(Commands.back());
    this.removeCommand(Commands.playerPlay());
    this.removeCommand(Commands.playerNext());
    this.removeCommand(Commands.playerPrevious());
    this.removeCommand(Commands.playerStop());
    this.removeCommand(Commands.playerVolume());
    this.removeCommand(Commands.playerAddToPlaylist());
    this.removeCommand(Commands.playerShowPlaylist());
    this.removeCommand(Commands.playerRepeat());
    this.removeCommand(Commands.playerShuffle());
    this.removeCommand(Commands.playerSleepTimer());
  }

  public void refreshStatus() {
    if (this.currentStatusKey != null) {
      this.setStatus(Lang.tr(this.currentStatusKey));
    }
  }

  public void showNotify() {
    if ("player.status.playing".equals(this.currentStatusKey)
        || "player.status.paused".equals(this.currentStatusKey)
        || "player.status.stopped".equals(this.currentStatusKey)) {
      determinePlayerStatus();
    }
  }

  private void determinePlayerStatus() {
    if (this.getPlayerGUI().isPlaying()) {
      this.setStatusByKey("player.status.playing");
    } else if ("player.status.stopped".equals(this.currentStatusKey)) {
      this.setStatusByKey("player.status.stopped");
    } else {
      this.setStatusByKey("player.status.paused");
    }
  }

  public void change(String title, Tracks tracks, int index, Navigator navigator) {
    this.title = title;
    this.setTitle(title);
    this.navigator = navigator;
    this.resetAlbumArt();
    this.getPlayerGUI().setPlaylist(tracks, index);
    this.getPlayerGUI().play();
  }

  public void setupDisplay() {
    this.displayHeight = -1;
    this.updateDisplay();
  }

  public String getTitle() {
    return this.title;
  }

  public void setStatus(String s) {
    updateStatus(s);
    this.updateDisplay();
  }

  public String getStatus() {
    return statusCurrent;
  }

  public void setStatusByKey(String statusKey) {
    this.currentStatusKey = statusKey;
    this.setStatus(Lang.tr(statusKey));
  }

  public String getCurrentStatusKey() {
    return this.currentStatusKey;
  }

  private void resetAlbumArt() {
    if (this.currentImageLoadOperation != null) {
      this.currentImageLoadOperation.stop();
      this.currentImageLoadOperation = null;
    }
    this.albumArt = null;
    this.albumArtUrl = null;
    this.loadingAlbumArt = false;
  }

  public void showVolumeAlert(int volumeLevel) {
    currentVolumeLevel = volumeLevel;
    if (!volumeAlertShowing) {
      volumeAlertShowing = true;
    }
    this.updateDisplay();
  }

  public void hideVolumeAlert() {
    if (volumeAlertShowing) {
      volumeAlertShowing = false;
      this.getPlayerGUI().saveVolumeLevel();
      this.updateDisplay();
    }
  }

  public void updateDisplay() {
    this.repaint();
    this.serviceRepaints();
  }

  public void close() {
    if (this.currentImageLoadOperation != null) {
      this.currentImageLoadOperation.stop();
      this.currentImageLoadOperation = null;
    }
    sleepTimerManager.setCallback(null);
  }

  public synchronized PlayerGUI getPlayerGUI() {
    if (this.gui == null) {
      this.gui = new PlayerGUI(this);
    }
    return this.gui;
  }

  public void setAlbumArtUrl(String url) {
    if (url != null && !url.equals(this.albumArtUrl)) {
      if (this.currentImageLoadOperation != null) {
        this.currentImageLoadOperation.stop();
        this.currentImageLoadOperation = null;
      }
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
    if (this.currentImageLoadOperation != null) {
      this.currentImageLoadOperation.stop();
    }
    this.loadingAlbumArt = true;
    final String imageUrl = this.albumArtUrl;
    this.currentImageLoadOperation =
        new ImageLoadOperation(
            imageUrl,
            ART_SIZE,
            new ImageLoadOperation.Listener() {
              public void onImageLoaded(Image image) {
                albumArt = image;
                if (albumArt != null) {
                  updateDisplay();
                }
                loadingAlbumArt = false;
                currentImageLoadOperation = null;
              }

              public void onImageLoadError(Exception e) {
                albumArt = null;
                loadingAlbumArt = false;
                currentImageLoadOperation = null;
              }
            });
    this.currentImageLoadOperation.start();
  }

  protected void keyPressed(int keycode) {
    try {
      int action = this.getGameAction(keycode);
      handleAction(action);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private boolean isLoading() {
    return statusCurrent != null && statusCurrent.indexOf(Lang.tr("status.loading")) != -1;
  }

  protected void pointerPressed(int x, int y) {
    if (!this.touchSupported || this.isLoading()) {
      return;
    }
    try {
      if (volumeAlertShowing) {
        handleVolumeAlertTouch(x, y);
        return;
      }
      if (isPointInButton(x, y, playX, playY, PLAY_BUTTON_WIDTH, PLAY_BUTTON_HEIGHT)) {
        handleAction(Canvas.FIRE);
      } else if (isPointInButton(x, y, prevX, prevY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        handleAction(Canvas.LEFT);
      } else if (isPointInButton(x, y, nextX, nextY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        handleAction(Canvas.RIGHT);
      } else if (isPointInButton(x, y, repeatX, repeatY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        toggleRepeat();
      } else if (isPointInButton(x, y, shuffleX, shuffleY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
        toggleShuffle();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void handleVolumeAlertTouch(int x, int y) {
    int alertWidth = displayWidth - 40;
    int alertHeight = 100;
    int alertX = 20;
    int alertY = (displayHeight - alertHeight) / 2;
    if (x >= alertX && x <= alertX + alertWidth && y >= alertY && y <= alertY + alertHeight) {
      int barWidth = alertWidth - 40;
      int barX = alertX + 20;
      int barY = alertY + 40;
      int barHeight = 20;
      if (x >= barX && x <= barX + barWidth && y >= barY && y <= barY + barHeight) {
        int tapPosition = x - barX;
        int newVolume = (tapPosition * Configuration.PLAYER_MAX_VOLUME) / barWidth;
        newVolume = Math.max(0, Math.min(Configuration.PLAYER_MAX_VOLUME, newVolume));
        getPlayerGUI().setVolumeLevel(newVolume);
        currentVolumeLevel = newVolume;
        updateDisplay();
      }
    } else {
      hideVolumeAlert();
    }
  }

  private void handleAction(int action) {
    if (this.isLoading()) {
      return;
    }
    try {
      if (volumeAlertShowing) {
        switch (action) {
          case Canvas.UP:
          case Canvas.RIGHT:
            this.getPlayerGUI().adjustVolume(true);
            break;
          case Canvas.DOWN:
          case Canvas.LEFT:
            this.getPlayerGUI().adjustVolume(false);
            break;
          case Canvas.FIRE:
            this.hideVolumeAlert();
            break;
          default:
            break;
        }
        return;
      }
      switch (action) {
        case Canvas.FIRE:
          this.getPlayerGUI().toggle();
          break;
        case Canvas.RIGHT:
          this.showingNextActive = true;
          this.nextButtonPressTime = System.currentTimeMillis();
          this.getPlayerGUI().next();
          break;
        case Canvas.LEFT:
          this.showingPreviousActive = true;
          this.previousButtonPressTime = System.currentTimeMillis();
          this.getPlayerGUI().previous();
          break;
        case Canvas.UP:
          this.getPlayerGUI().adjustVolume(true);
          break;
        case Canvas.DOWN:
          this.getPlayerGUI().adjustVolume(false);
          break;
        default:
          break;
      }
    } catch (Throwable e) {
      e.printStackTrace();
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

  public void paint(Graphics g) {
    try {
      if (displayHeight == -1) {
        initDisplayMetrics(g);
      }
      int clipY = g.getClipY();
      int clipHeight = g.getClipHeight();
      paintBackground(g);
      paintStatusBar(g, clipY, clipHeight);
      paintAlbumArt(g);
      paintTrackInfo(g, clipY, clipHeight);
      paintTimeSlider(g, clipY, clipHeight);
      paintControlButtons(g);
      if (volumeAlertShowing) {
        paintVolumeAlert(g);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void initDisplayMetrics(Graphics g) {
    displayWidth = this.getWidth();
    displayHeight = this.getHeight();
    textHeight = g.getFont().getHeight();
    statusBarHeight = textHeight + 5;
    trackInfoLeft = ART_LEFT + ART_SIZE + 15;
    int currentTop = PLAYER_STATUS_TOP + statusBarHeight + 15;
    trackNameTop = currentTop;
    currentTop += SONG_TITLE_GAP + textHeight;
    singerNameTop = currentTop;
    currentTop += TIME_GAP + textHeight + 24;
    timeRateTop = currentTop;
    timeWidth = g.getFont().stringWidth("0:00:0  ");
    sliderWidth = displayWidth - (timeWidth * 2) - 8;
    sliderTop = timeRateTop + (textHeight - SLIDER_HEIGHT) / 2;
    sliderLeft = timeWidth + 4;
    playTop = timeRateTop + textHeight + 24;
  }

  private void paintBackground(Graphics g) {
    g.setColor(BACKGROUND_COLOR);
    g.fillRect(0, 0, displayWidth, displayHeight);
  }

  private void paintStatusBar(Graphics g, int clipY, int clipHeight) {
    g.setColor(THEME_COLOR);
    g.fillRect(0, 0, displayWidth, statusBarHeight);
    if (intersects(clipY, clipHeight, PLAYER_STATUS_TOP, textHeight)) {
      g.setColor(255, 255, 255);
      g.drawString(statusCurrent, displayWidth >> 1, PLAYER_STATUS_TOP, 17);
    }
  }

  private void paintAlbumArt(Graphics g) {
    int artTop = PLAYER_STATUS_TOP + statusBarHeight + 8;
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

  private void paintTrackInfo(Graphics g, int clipY, int clipHeight) {
    model.Track currentTrack = this.getPlayerGUI().getCurrentTrack();
    if (currentTrack == null) {
      return;
    }
    if (intersects(clipY, clipHeight, trackNameTop, textHeight)) {
      g.setColor(THEME_COLOR);
      int maxTrackNameWidth = displayWidth - trackInfoLeft - 10;
      String trackName = currentTrack.getName();
      String truncatedTrackName = truncateText(trackName, g, maxTrackNameWidth);
      g.drawString(truncatedTrackName, trackInfoLeft, trackNameTop, 20);
    }
    if (intersects(clipY, clipHeight, singerNameTop, textHeight)) {
      g.setColor(140, 140, 140);
      int maxSingerWidth = displayWidth - trackInfoLeft - 10;
      String artistName = currentTrack.getArtist();
      String truncatedSinger = truncateText(artistName, g, maxSingerWidth);
      g.drawString(truncatedSinger, trackInfoLeft, singerNameTop, 20);
    }
  }

  private void paintTimeSlider(Graphics g, int clipY, int clipHeight) {
    long duration = this.getPlayerGUI().getDuration();
    long current = this.getPlayerGUI().getCurrentTime();
    String strDuration = timeDisplay(duration);
    String strCurrent = timeDisplay(current);
    if (duration > 0) {
      sliderValue = (float) sliderWidth * ((float) current / (float) duration);
    } else {
      sliderValue = 0;
    }
    if (intersects(clipY, clipHeight, timeRateTop, textHeight)) {
      g.setColor(140, 140, 140);
      g.drawString(strCurrent, 5, timeRateTop, 20);
    }
    g.setColor(220, 220, 220);
    g.fillRect(sliderLeft, sliderTop, sliderWidth, SLIDER_HEIGHT);
    g.setColor(THEME_COLOR);
    g.fillRect(sliderLeft, sliderTop, (int) sliderValue, SLIDER_HEIGHT);
    if (intersects(clipY, clipHeight, timeRateTop, textHeight)) {
      g.setColor(140, 140, 140);
      g.drawString(strDuration, displayWidth - 5, timeRateTop, 24);
    }
  }

  private void paintControlButtons(Graphics g) {
    initButtonPositions();
    updateButtonStates();
    boolean isPlaying = this.getPlayerGUI().isPlaying();
    Image playPauseImg = isPlaying ? Configuration.pauseIcon : Configuration.playIcon;
    if (playPauseImg != null) {
      g.drawImage(playPauseImg, playX, playY, 3);
    }
    Image prevImg = Configuration.previousIcon;
    if (prevImg != null) {
      g.drawImage(prevImg, prevX, prevY, 3);
    }
    Image nextImg = Configuration.nextIcon;
    if (nextImg != null) {
      g.drawImage(nextImg, nextX, nextY, 3);
    }
    int repeatMode = this.getPlayerGUI().getRepeatMode();
    Image repeatImg;
    if (Configuration.PLAYER_REPEAT_ONE == repeatMode) {
      repeatImg = Configuration.repeatOneIcon;
    } else if (Configuration.PLAYER_REPEAT_ALL == repeatMode) {
      repeatImg = Configuration.repeatIcon;
    } else {
      repeatImg = Configuration.repeatOffIcon;
    }
    if (repeatImg != null) {
      g.drawImage(repeatImg, repeatX, repeatY, 3);
    }
    Image shuffleImg =
        this.getPlayerGUI().isShuffleEnabled()
            ? Configuration.shuffleIcon
            : Configuration.shuffleOffIcon;
    if (shuffleImg != null) {
      g.drawImage(shuffleImg, shuffleX, shuffleY, 3);
    }
  }

  private void initButtonPositions() {
    if (playX == 0) {
      int screenCenter = displayWidth >> 1;
      int buttonGap = displayWidth / 5;
      int margin = 8;
      playX = screenCenter;
      playY = playTop;
      prevX = screenCenter - buttonGap;
      prevY = playTop;
      nextX = screenCenter + buttonGap;
      nextY = playTop;
      repeatX = margin + (BUTTON_WIDTH / 2);
      repeatY = playTop;
      shuffleX = displayWidth - margin - (BUTTON_WIDTH / 2);
      shuffleY = playTop;
    }
  }

  private void updateButtonStates() {
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

  private void paintVolumeAlert(Graphics g) {
    int alertWidth = displayWidth - 40;
    int alertHeight = 100;
    int alertX = 20;
    int alertY = (displayHeight - alertHeight) / 2;
    g.setColor(BACKGROUND_COLOR);
    g.fillRect(alertX, alertY, alertWidth, alertHeight);
    g.setColor(0x000000);
    g.drawRect(alertX, alertY, alertWidth, alertHeight);
    g.setColor(0x000000);
    String title = Lang.tr("player.volume");
    g.drawString(title, alertX + alertWidth / 2, alertY + 15, Graphics.HCENTER | Graphics.TOP);
    int barWidth = alertWidth - 40;
    int barHeight = 20;
    int barX = alertX + 20;
    int barY = alertY + 40;
    g.setColor(220, 220, 220);
    g.fillRect(barX, barY, barWidth, barHeight);
    g.setColor(THEME_COLOR);
    int progressWidth = (barWidth * currentVolumeLevel) / Configuration.PLAYER_MAX_VOLUME;
    g.fillRect(barX, barY, progressWidth, barHeight);
    g.setColor(0x000000);
    g.drawRect(barX, barY, barWidth, barHeight);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == Commands.back()) {
      if (volumeAlertShowing) {
        hideVolumeAlert();
      } else {
        this.navigator.back();
      }
    } else if (c == Commands.playerNext()) {
      next();
    } else if (c == Commands.playerPrevious()) {
      previous();
    } else if (c == Commands.playerPlay()) {
      togglePlayPause();
    } else if (c == Commands.playerStop()) {
      stop();
    } else if (c == Commands.playerRepeat()) {
      toggleRepeat();
    } else if (c == Commands.playerShuffle()) {
      toggleShuffle();
    } else if (c == Commands.playerVolume()) {
      showVolumeAlert(getPlayerGUI().getVolumeLevel());
    } else if (c == Commands.playerAddToPlaylist()) {
      addCurrentTrackToPlaylist();
    } else if (c == Commands.playerShowPlaylist()) {
      showCurrentPlaylist();
    } else if (c == Commands.playerSleepTimer()) {
      showSleepTimerDialog();
    } else if (c == Commands.playerCancelTimer()) {
      sleepTimerManager.cancelTimer();
      updateSleepTimerCommands();
    }
  }

  private void togglePlayPause() {
    if (!this.isLoading()) {
      this.getPlayerGUI().toggle();
    }
  }

  private void toggleRepeat() {
    this.getPlayerGUI().toggleRepeat();
    this.updateDisplay();
  }

  private void toggleShuffle() {
    this.getPlayerGUI().toggleShuffle();
    this.updateDisplay();
  }

  private void stop() {
    this.getPlayerGUI().pause();
    this.getPlayerGUI().seek(0L);
    this.setStatusByKey("player.status.stopped");
  }

  private void next() {
    navigate(true);
  }

  private void previous() {
    navigate(false);
  }

  private void navigate(boolean isNext) {
    if (!this.isLoading()) {
      try {
        if (isNext) {
          this.showingNextActive = true;
          this.nextButtonPressTime = System.currentTimeMillis();
          this.getPlayerGUI().next();
        } else {
          this.showingPreviousActive = true;
          this.previousButtonPressTime = System.currentTimeMillis();
          this.getPlayerGUI().previous();
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  private void addCurrentTrackToPlaylist() {
    Track currentTrack = getPlayerGUI().getCurrentTrack();
    if (currentTrack == null) {
      return;
    }
    FavoritesScreen selectionScreen = new FavoritesScreen(navigator, currentTrack, this);
    navigator.forward(selectionScreen);
  }

  private void showCurrentPlaylist() {
    Tracks currentTracks = this.getPlayerGUI().getCurrentTracks();
    if (currentTracks == null
        || currentTracks.getTracks() == null
        || currentTracks.getTracks().length == 0) {
      return;
    }
    String playlistTitle = this.title != null ? this.title : Lang.tr("player.show_playlist");
    TrackListScreen trackListScreen =
        new TrackListScreen(playlistTitle, currentTracks, this.navigator);
    trackListScreen.setSelectedIndex(this.getPlayerGUI().getCurrentIndex(), true);
    this.navigator.forward(trackListScreen);
  }

  public void cancel() {}

  private void showSleepTimerDialog() {
    SleepTimerForm form =
        new SleepTimerForm(
            navigator,
            new SleepTimerForm.SleepTimerCallback() {
              public void onTimerSet(
                  int mode, int durationMinutes, int targetHour, int targetMinute, int action) {
                sleepTimerManager.setCallback(PlayerScreen.this);
                if (mode == SleepTimerForm.MODE_COUNTDOWN) {
                  sleepTimerManager.startCountdownTimer(durationMinutes, action);
                } else {
                  sleepTimerManager.startAbsoluteTimer(targetHour, targetMinute, action);
                }
                navigator.back();
                navigator.showAlert(
                    Lang.tr("timer.status.set"), AlertType.CONFIRMATION, PlayerScreen.this);
                updateSleepTimerCommands();
              }
            });
    navigator.forward(form);
  }

  private void updateSleepTimerCommands() {
    if (sleepTimerManager.isActive()) {
      removeCommand(Commands.playerSleepTimer());
      addCommand(Commands.playerCancelTimer());
    } else {
      removeCommand(Commands.playerCancelTimer());
      addCommand(Commands.playerSleepTimer());
    }
  }

  public void onTimerExpired(int action) {
    if (action == SleepTimerForm.ACTION_STOP_PLAYBACK) {
      stop();
      navigator.showAlert(Lang.tr("timer.status.expired"), AlertType.INFO);
    } else if (action == SleepTimerForm.ACTION_EXIT_APP) {
      navigator.showAlert(Lang.tr("timer.status.expired"), AlertType.INFO);
    }
    updateSleepTimerCommands();
  }

  public void onTimerUpdate(String remainingTime) {
    if (sleepTimerManager.isActive()) {
      setTimerOverride(Lang.tr("timer.sleep_timer") + ": " + remainingTime);
      updateDisplay();
    }
  }

  public void onTimerCancelled() {
    clearTimerOverride();
    navigator.showAlert(Lang.tr("timer.status.cancelled"), AlertType.CONFIRMATION);
    updateSleepTimerCommands();
    updateDisplay();
  }

  public void showError(String message) {
    navigator.showAlert(message, AlertType.ERROR);
  }

  private void updateStatus(String s) {
    if (s != null
        && s.indexOf(Lang.tr("timer.status.remaining").substring(0, 3)) == -1
        && s.indexOf(Lang.tr("player.volume")) == -1) {
      this.realPlayerStatus = s;
    }
    if (!timerOverrideActive) {
      this.statusCurrent = s;
      this.statusOriginal = s;
    } else if (isCriticalStatus(s)) {
      this.statusCurrent = s;
      this.statusOriginal = s;
      this.timerOverrideActive = false;
    }
  }

  private boolean isCriticalStatus(String status) {
    return status != null
        && (status.indexOf(Lang.tr("status.loading")) != -1
            || status.toLowerCase().indexOf("error") != -1
            || status.indexOf(Lang.tr("player.volume")) != -1);
  }

  private void setTimerOverride(String timerStatus) {
    this.timerOverrideActive = true;
    this.statusCurrent = timerStatus;
  }

  private void clearTimerOverride() {
    this.timerOverrideActive = false;
    this.statusCurrent = this.realPlayerStatus;
  }

  private boolean isTimerOverrideActive() {
    return timerOverrideActive;
  }
}
