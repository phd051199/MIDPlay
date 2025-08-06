import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import model.Track;
import model.Tracks;

public final class PlayerScreen extends Canvas
    implements CommandListener, SleepTimerManager.SleepTimerCallback {

  // UI Utility methods
  private static void drawCenteredText(Graphics g, String text, int x, int y) {
    g.drawString(text, x, y, Graphics.HCENTER | Graphics.TOP);
  }

  private static void drawLeftAlignedText(Graphics g, String text, int x, int y) {
    g.drawString(text, x, y, Graphics.LEFT | Graphics.TOP);
  }

  private static boolean isPointInBounds(
      int x, int y, int boundsX, int boundsY, int width, int height) {
    return x >= boundsX && x <= boundsX + width && y >= boundsY && y <= boundsY + height;
  }

  private static boolean isPointInButton(
      int x, int y, int buttonX, int buttonY, int buttonW, int buttonH) {
    return x >= buttonX - buttonW / 2
        && x <= buttonX + buttonW / 2
        && y >= buttonY - buttonH / 2
        && y <= buttonY + buttonH / 2;
  }

  private static boolean intersects(int clipY, int clipHeight, int y, int h) {
    return clipY <= y + h && clipY + clipHeight >= y;
  }

  // Layout helper methods
  private static boolean isLargeScreen(int width, int height) {
    if (width <= 0 || height <= 0) {
      return false;
    }
    int minDimension = Math.min(width, height);
    int maxDimension = Math.max(width, height);
    float aspectRatio = (float) maxDimension / minDimension;
    return minDimension >= 240 && maxDimension >= 320 && aspectRatio >= 1.5F;
  }

  private static boolean isLandscape(int width, int height) {
    return width > height;
  }

  private static int clamp(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  private static void calculateResponsiveSizes(int screenWidth, int screenHeight, int[] sizes) {
    int minDimension = Math.min(screenWidth, screenHeight);

    int buttonSize = minDimension / 10;
    int playButtonSize = (buttonSize * 5) / 4;
    int sliderHeight = Math.max(4, minDimension / 80);

    buttonSize = clamp(buttonSize, 30, 60);
    playButtonSize = clamp(playButtonSize, 38, 75);

    sizes[0] = buttonSize; // buttonWidth
    sizes[1] = buttonSize; // buttonHeight
    sizes[2] = playButtonSize; // playButtonWidth
    sizes[3] = playButtonSize; // playButtonHeight
    sizes[4] = sliderHeight; // sliderHeight
  }

  private static int calculateAlbumArtSize(
      int screenWidth, int screenHeight, boolean isLandscape, boolean isLargeScreen) {
    if (!isLargeScreen) {
      return 72;
    }

    if (isLandscape) {
      int statusBarHeight = screenHeight / 100;
      int availableHeight = screenHeight - statusBarHeight;
      return Math.min(availableHeight - (screenHeight / 15), screenWidth / 2 - (screenWidth / 30));
    } else {
      return Math.min(screenWidth - (screenWidth / 15), (screenHeight * 40) / 100);
    }
  }

  private static void calculateButtonPositions(
      int screenWidth,
      int screenHeight,
      boolean isLandscape,
      boolean isLargeScreen,
      int[] positions) {
    int centerX = screenWidth / 2;
    int buttonGap = screenWidth / 5;
    int margin = isLargeScreen ? (screenWidth / 40) : 8;
    int buttonWidth = positions[4]; // Get buttonWidth from passed array

    if (isLargeScreen && isLandscape) {
      int leftPanelWidth = screenWidth * 45 / 100;
      int rightPanelLeft = leftPanelWidth + (screenWidth / 40);
      int rightPanelWidth = screenWidth - rightPanelLeft - (screenWidth / 30);
      int rightPanelCenter = rightPanelLeft + (rightPanelWidth >> 1);
      buttonGap = rightPanelWidth / 5;

      positions[0] = rightPanelCenter; // play
      positions[1] = rightPanelCenter - buttonGap; // prev
      positions[2] = rightPanelCenter + buttonGap; // next
      positions[3] = rightPanelLeft + margin + (buttonWidth / 2); // repeat
      positions[4] = rightPanelLeft + rightPanelWidth - margin - (buttonWidth / 2); // shuffle
    } else {
      positions[0] = centerX; // play
      positions[1] = centerX - buttonGap; // prev
      positions[2] = centerX + buttonGap; // next
      positions[3] = margin + (buttonWidth / 2); // repeat
      positions[4] = screenWidth - margin - (buttonWidth / 2); // shuffle
    }
  }

  private final SleepTimerManager sleepTimerManager = new SleepTimerManager();
  private final boolean touchSupported;

  private int displayWidth = -1, displayHeight = -1;
  private int textHeight = 10;
  private boolean isLandscape, isLargeScreen;
  private boolean volumeAlertShowing, timerOverrideActive;
  private int currentVolumeLevel;
  private String currentStatusKey = Configuration.PLAYER_STATUS_STOPPED;
  private String statusCurrent = "", realPlayerStatus = "";
  private Font titleFont, artistFont, defaultFont;
  private int statusBarHeight, albumSize = 72, albumX = 8, albumY;
  private int textX, titleY, artistY, timeY, timeWidth;
  private int buttonWidth = 40, buttonHeight = 40, playButtonWidth = 50, playButtonHeight = 50;
  private int playTop,
      playX,
      playY,
      prevX,
      prevY,
      nextX,
      nextY,
      repeatX,
      repeatY,
      shuffleX,
      shuffleY;
  private int sliderTop, sliderLeft, sliderWidth = 12, sliderHeight = 6;

  private Image albumArt;
  private Image scaledAlbumArt;
  private int lastScaledSize = -1;
  private String albumArtUrl;
  private boolean loadingAlbumArt, albumArtLoaded;
  private ImageLoadOperation currentImageLoadOperation;

  private long lastDuration = -1;
  private String durationText = "";

  private float sliderValue;

  private String title;
  private PlayerGUI gui;
  private Navigator navigator;
  private String truncatedTrackName, truncatedSinger;

  public PlayerScreen(String title, Tracks tracks, int index, Navigator navigator) {
    this.title = title;
    this.navigator = navigator;

    setTitle(title);
    addCommands();
    setCommandListener(this);
    touchSupported = hasPointerEvents();
    change(title, tracks, index, navigator);
    initializeFonts();
  }

  private void manageCommands(boolean add) {
    Command[] commands = {
      Commands.back(),
      Commands.playerPlay(),
      Commands.playerNext(),
      Commands.playerPrevious(),
      Commands.playerStop(),
      Commands.playerVolume(),
      Commands.playerAddToPlaylist(),
      Commands.playerShowPlaylist(),
      Commands.playerRepeat(),
      Commands.playerShuffle()
    };

    for (int i = 0; i < commands.length; i++) {
      if (add) {
        addCommand(commands[i]);
      } else {
        removeCommand(commands[i]);
      }
    }

    if (add) {
      updateSleepTimerCommands();
    } else {
      removeSleepTimerCommand();
    }
  }

  public void addCommands() {
    manageCommands(true);
  }

  public void clearCommands() {
    manageCommands(false);
  }

  public void refreshStatus() {
    if (currentStatusKey != null) {
      setStatus(Lang.tr(currentStatusKey));
    }
  }

  public void showNotify() {
    if (isPlayingState() || isPausedState() || isStoppedState()) {
      determinePlayerStatus();
    }
  }

  public void change(String title, Tracks tracks, int index, Navigator navigator) {
    this.title = title;
    setTitle(title);
    this.navigator = navigator;
    getPlayerGUI().setPlaylist(tracks, index);
    getPlayerGUI().play();
  }

  public void setupDisplay() {
    displayHeight = -1;
    updateDisplay();
  }

  public void setStatus(String s) {
    updateStatus(s);
    updateDisplay();
  }

  public void setStatusByKey(String statusKey) {
    currentStatusKey = statusKey;
    setStatus(Lang.tr(statusKey));
  }

  public void showVolumeAlert(int volumeLevel) {
    currentVolumeLevel = volumeLevel;
    volumeAlertShowing = true;
    updateDisplay();
  }

  public void hideVolumeAlert() {
    if (volumeAlertShowing) {
      volumeAlertShowing = false;
      getPlayerGUI().saveVolumeLevel();
      updateDisplay();
    }
  }

  public void updateDisplay() {
    repaint();
    serviceRepaints();
  }

  protected void sizeChanged(int w, int h) {
    detectOrientationChange(w, h);
    displayWidth = displayHeight = -1;
    scaledAlbumArt = null;
    lastScaledSize = -1;
    resetButtonPositions();
    resetTruncatedText();
  }

  public void resetTruncatedText() {
    truncatedTrackName = null;
    truncatedSinger = null;
  }

  public void close() {
    resetImage();
    sleepTimerManager.setCallback(null);
  }

  public synchronized PlayerGUI getPlayerGUI() {
    if (gui == null) {
      gui = new PlayerGUI(this);
    }
    return gui;
  }

  public void setAlbumArtUrl(String url) {
    if (url != null && !url.equals(albumArtUrl)) {
      stopCurrentImageLoad();
      resetImageState(url);
      if (displayWidth > 0 && !isLargeScreen) {
        loadAlbumArt();
      }
    }
  }

  protected void keyPressed(int keycode) {
    try {
      int action = getGameAction(keycode);

      if (action != 0) {
        handleAction(action);
      } else {
        handleMusicKey(keycode);
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void handleMusicKey(int code) {
    switch (code) {
      case -20: // Play/Resume
        this.getPlayerGUI().toggle();
        break;
      case -21: // Previous
        this.getPlayerGUI().previous();
        break;
      case -22: // Next
        this.getPlayerGUI().next();
        break;
    }
  }

  protected void pointerPressed(int x, int y) {
    if (!touchSupported || isLoading()) {
      return;
    }

    try {
      if (volumeAlertShowing) {
        handleVolumeAlertTouch(x, y);
      } else {
        handleButtonTouch(x, y);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
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

  public void commandAction(Command c, Displayable d) {
    if (c == Commands.back()) {
      handleBackCommand();
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
      handleCancelTimerCommand();
    }
  }

  public void onTimerExpired(int action) {
    handleTimerAction(action);
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

  public void cancel() {}

  private boolean isPlayingState() {
    return Configuration.PLAYER_STATUS_PLAYING.equals(currentStatusKey);
  }

  private boolean isPausedState() {
    return Configuration.PLAYER_STATUS_PAUSED.equals(currentStatusKey);
  }

  private boolean isStoppedState() {
    return Configuration.PLAYER_STATUS_STOPPED.equals(currentStatusKey);
  }

  private void determinePlayerStatus() {
    if (getPlayerGUI().isPlaying()) {
      setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
    } else if (isStoppedState()) {
      setStatusByKey(Configuration.PLAYER_STATUS_STOPPED);
    } else {
      setStatusByKey(Configuration.PLAYER_STATUS_PAUSED);
    }
  }

  private void stopCurrentImageLoad() {
    if (currentImageLoadOperation != null) {
      currentImageLoadOperation.stop();
      currentImageLoadOperation = null;
    }
  }

  private void clearImageData() {
    albumArt = null;
    scaledAlbumArt = null;
    lastScaledSize = -1;
    loadingAlbumArt = albumArtLoaded = false;
  }

  public void resetImage() {
    stopCurrentImageLoad();
    clearImageData();
    albumArtUrl = null;
  }

  private void resetImageState(String url) {
    albumArtUrl = url;
    clearImageData();
  }

  private void loadAlbumArtAfterLayout() {
    if (canLoadAlbumArt()) {
      loadAlbumArt();
    }
  }

  private void loadAlbumArt() {
    if (!canLoadAlbumArt()) {
      return;
    }

    stopCurrentImageLoad();
    loadingAlbumArt = true;
    final String imageUrl = albumArtUrl;
    int targetSize = calculateAlbumArtSize();

    currentImageLoadOperation =
        new ImageLoadOperation(imageUrl, targetSize, new ImageLoadCallback());
    currentImageLoadOperation.start();
  }

  private int calculateAlbumArtSize() {
    return calculateAlbumArtSize(displayWidth, displayHeight, isLandscape, isLargeScreen);
  }

  private boolean isLoading() {
    return statusCurrent != null && statusCurrent.indexOf(Lang.tr("status.loading")) != -1;
  }

  private void handleButtonTouch(int x, int y) {
    if (isPointInButton(x, y, playX, playY, playButtonWidth, playButtonHeight)) {
      handleAction(Canvas.FIRE);
    } else if (isPointInButton(x, y, prevX, prevY, buttonWidth, buttonHeight)) {
      handleAction(Canvas.LEFT);
    } else if (isPointInButton(x, y, nextX, nextY, buttonWidth, buttonHeight)) {
      handleAction(Canvas.RIGHT);
    } else if (isPointInButton(x, y, repeatX, repeatY, buttonWidth, buttonHeight)) {
      toggleRepeat();
    } else if (isPointInButton(x, y, shuffleX, shuffleY, buttonWidth, buttonHeight)) {
      toggleShuffle();
    }
  }

  private void handleVolumeAlertTouch(int x, int y) {
    int alertWidth = displayWidth - 40;
    int alertHeight = 100;
    int alertX = 20;
    int alertY = (displayHeight - alertHeight) / 2;
    int barWidth = alertWidth - 40;
    int barX = alertX + 20;
    int barY = alertY + 40;
    int barHeight = 20;

    if (isPointInBounds(x, y, alertX, alertY, alertWidth, alertHeight)) {
      if (isPointInBounds(x, y, barX, barY, barWidth, barHeight)) {
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
    if (isLoading()) {
      return;
    }

    try {
      if (volumeAlertShowing) {
        handleVolumeAction(action);
      } else {
        handlePlayerAction(action);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void handleVolumeAction(int action) {
    switch (action) {
      case Canvas.UP:
      case Canvas.RIGHT:
        getPlayerGUI().adjustVolume(true);
        break;
      case Canvas.DOWN:
      case Canvas.LEFT:
        getPlayerGUI().adjustVolume(false);
        break;
      case Canvas.FIRE:
        hideVolumeAlert();
        break;
    }
  }

  private void handlePlayerAction(int action) {
    switch (action) {
      case Canvas.FIRE:
        getPlayerGUI().toggle();
        break;
      case Canvas.RIGHT:
        getPlayerGUI().next();
        break;
      case Canvas.LEFT:
        getPlayerGUI().previous();
        break;
      case Canvas.UP:
        getPlayerGUI().adjustVolume(true);
        break;
      case Canvas.DOWN:
        getPlayerGUI().adjustVolume(false);
        break;
    }
  }

  private void drawTrackText(Graphics g, String text, int x, int y) {
    if (isLargeScreen) {
      drawCenteredText(g, text, x, y);
    } else {
      drawLeftAlignedText(g, text, x, y);
    }
  }

  private void initDisplayMetrics(Graphics g) {
    displayWidth = getWidth();
    displayHeight = getHeight();
    detectOrientationChange(displayWidth, displayHeight);

    if (isLargeScreen) {
      initLargeScreenLayout(g);
    } else {
      initSmallScreenLayout(g);
    }
  }

  private void initLargeScreenLayout(Graphics g) {
    calculateResponsiveSizes();
    updateFontsForLargeScreen();
    statusBarHeight = textHeight + (displayHeight / 100);

    if (isLandscape) {
      initLargeLandscapeLayout(g);
    } else {
      initLargePortraitLayout(g);
    }
    loadAlbumArtAfterLayout();
  }

  private void initSmallScreenLayout(Graphics g) {
    textHeight = defaultFont.getHeight();
    statusBarHeight = textHeight + 5;

    calculateSmallScreenPositions();
    calculateSmallScreenSlider(g);
    calculateSmallScreenArt();
    textX = albumX + albumSize + 16;
    loadAlbumArtAfterLayout();
  }

  private void calculateSmallScreenPositions() {
    int currentTop = 2 + statusBarHeight + 15;
    titleY = currentTop;
    currentTop += 5 + textHeight;
    artistY = currentTop;
    currentTop += 10 + textHeight + 24;
    timeY = currentTop;
    playTop = timeY + textHeight + 24;
  }

  private void calculateSmallScreenSlider(Graphics g) {
    timeWidth = g.getFont().stringWidth("0:00:0  ");
    sliderWidth = displayWidth - (timeWidth * 2) - 8;
    sliderTop = timeY + (textHeight - sliderHeight) / 2;
    sliderLeft = timeWidth + 4;
  }

  private void calculateSmallScreenArt() {
    albumSize = 72;
    albumX = 8;
    albumY = 2 + statusBarHeight + 8;
  }

  private void initLargeLandscapeLayout(Graphics g) {
    int margin = displayWidth / 30;
    int availableHeight = displayHeight - statusBarHeight - margin * 2;
    int leftPanelWidth = displayWidth * 45 / 100;

    calculateLandscapeArtDimensions(margin, availableHeight, leftPanelWidth);
    calculateLandscapeTrackInfo(leftPanelWidth, availableHeight);
    calculateLandscapeSlider(leftPanelWidth, margin, availableHeight);
    calculateLandscapeControls(availableHeight);
  }

  private void calculateLandscapeArtDimensions(
      int margin, int availableHeight, int leftPanelWidth) {
    albumSize = Math.min(availableHeight * 85 / 100, leftPanelWidth - margin * 2);
    albumX = (leftPanelWidth - albumSize) / 2;
    albumY = statusBarHeight + (availableHeight - albumSize) / 2;
  }

  private void calculateLandscapeTrackInfo(int leftPanelWidth, int availableHeight) {
    int rightPanelLeft = leftPanelWidth + (displayWidth / 40);
    int rightPanelWidth = displayWidth - rightPanelLeft - (displayWidth / 30);
    int rightPanelCenter = rightPanelLeft + (rightPanelWidth >> 1);

    textX = rightPanelCenter;
    int trackInfoSection = availableHeight / 4;
    titleY = statusBarHeight + trackInfoSection - titleFont.getHeight();
    artistY = titleY + (displayHeight / 80) + titleFont.getHeight();
  }

  private void calculateLandscapeSlider(int leftPanelWidth, int margin, int availableHeight) {
    int rightPanelLeft = leftPanelWidth + (displayWidth / 40);
    int rightPanelWidth = displayWidth - rightPanelLeft - margin;

    int timeSliderSection = availableHeight / 2;
    sliderTop = statusBarHeight + timeSliderSection - sliderHeight / 2;
    timeWidth = defaultFont.stringWidth("0:00:0  ");
    sliderLeft = rightPanelLeft + (displayWidth / 80);
    sliderWidth = rightPanelWidth - (displayWidth / 40);

    timeY = sliderTop + sliderHeight + (displayHeight / 60);
  }

  private void calculateLandscapeControls(int availableHeight) {
    int controlSection = availableHeight * 3 / 4;
    playTop = statusBarHeight + controlSection + (displayHeight / 80);
  }

  private void initLargePortraitLayout(Graphics g) {
    int margin = displayWidth / 20;
    albumSize = Math.min(displayWidth - margin * 2, (displayHeight * 40) / 100);
    albumX = (displayWidth - albumSize) / 2;

    int availableHeight = displayHeight - statusBarHeight;
    int contentHeight = calculateContentHeight();

    int startY = statusBarHeight + (availableHeight - contentHeight) / 2;
    if (startY < statusBarHeight + (displayHeight / 30)) {
      startY = statusBarHeight + (displayHeight / 30);
    }

    calculatePortraitPositions(startY, margin);
  }

  private void calculatePortraitPositions(int startY, int margin) {
    albumY = startY;
    int currentTop = startY + albumSize + (displayHeight / 18);

    textX = displayWidth / 2;
    titleY = currentTop;
    currentTop += (displayHeight / 80) + titleFont.getHeight();
    artistY = currentTop;
    currentTop += (displayHeight / 50) + artistFont.getHeight() + (displayHeight / 25);

    sliderLeft = margin;
    sliderWidth = displayWidth - margin * 2;
    sliderTop = currentTop + (textHeight - sliderHeight) / 2;

    timeY = sliderTop + sliderHeight + (displayHeight / 60);
    timeWidth = defaultFont.stringWidth("0:00:0  ");
    playTop = timeY + textHeight + (displayHeight / 20);
  }

  private int calculateContentHeight() {
    return albumSize
        + (displayHeight / 18)
        + (displayHeight / 80)
        + titleFont.getHeight()
        + (displayHeight / 50)
        + artistFont.getHeight()
        + (displayHeight / 25)
        + sliderHeight
        + (displayHeight / 60)
        + textHeight
        + (displayHeight / 20)
        + playButtonHeight;
  }

  private void paintBackground(Graphics g) {
    g.setColor(Theme.getBackgroundColor());
    g.fillRect(0, 0, displayWidth, displayHeight);
  }

  private void paintStatusBar(Graphics g, int clipY, int clipHeight) {
    g.setColor(Theme.getSecondaryContainerColor());
    g.fillRect(0, 0, displayWidth, statusBarHeight);

    if (intersects(clipY, clipHeight, 2, textHeight)) {
      g.setColor(Theme.getOnSecondaryContainerColor());
      g.drawString(statusCurrent, displayWidth >> 1, 2, 17);
    }
  }

  private void paintAlbumArt(Graphics g) {
    if (isLargeScreen) {
      paintLargeScreenAlbumArt(g);
    } else {
      paintSmallScreenAlbumArt(g);
    }
  }

  private void paintLargeScreenAlbumArt(Graphics g) {
    drawAlbumArtBackground(g);

    if (albumArt != null) {
      int innerSize = albumSize - 1;
      int innerX = albumX + 1;
      int innerY = albumY + 1;

      if (scaledAlbumArt == null || lastScaledSize != innerSize) {
        scaledAlbumArt = Utils.resizeImageToFit(albumArt, innerSize, innerSize);
        lastScaledSize = innerSize;
      }

      g.drawImage(scaledAlbumArt, innerX, innerY, Graphics.LEFT | Graphics.TOP);
    } else {
      drawAlbumArtPlaceholder(g, albumX + albumSize / 2, albumY + albumSize / 2);
    }
  }

  private void paintSmallScreenAlbumArt(Graphics g) {
    if (albumArt != null) {
      g.drawImage(albumArt, 8 + 36, albumY + 36, Graphics.HCENTER | Graphics.VCENTER);
      g.setColor(Theme.getOutlineColor());
      g.drawRect(8, albumY, 72, 72);
    } else {
      g.setColor(Theme.getSurfaceVariantColor());
      g.fillRect(8, albumY, 72, 72);
      g.setColor(Theme.getOutlineColor());
      g.drawRect(8, albumY, 72, 72);
      drawAlbumArtPlaceholder(g, 44, albumY + 36);
    }
  }

  private void drawAlbumArtBackground(Graphics g) {
    g.setColor(Theme.getSurfaceVariantColor());
    g.fillRect(albumX, albumY, albumSize, albumSize);
    g.setColor(Theme.getOutlineColor());
    g.drawRect(albumX, albumY, albumSize, albumSize);
  }

  private void drawAlbumArtPlaceholder(Graphics g, int centerX, int centerY) {
    g.setColor(Theme.getOnSurfaceVariantColor());
    g.drawString("♪", centerX, centerY, 17);
  }

  private void paintTrackInfo(Graphics g, int clipY, int clipHeight) {
    Track currentTrack = getPlayerGUI().getCurrentTrack();
    if (currentTrack == null) {
      return;
    }

    if (isLargeScreen) {
      paintLargeScreenTrackInfo(g, clipY, clipHeight, currentTrack);
    } else {
      paintSmallScreenTrackInfo(g, clipY, clipHeight, currentTrack);
    }
  }

  private void paintLargeScreenTrackInfo(
      Graphics g, int clipY, int clipHeight, Track currentTrack) {
    paintTrackName(g, clipY, clipHeight, currentTrack, true);
    paintArtistName(g, clipY, clipHeight, currentTrack, true);
    g.setFont(defaultFont);
  }

  private void paintSmallScreenTrackInfo(
      Graphics g, int clipY, int clipHeight, Track currentTrack) {
    paintTrackName(g, clipY, clipHeight, currentTrack, false);
    paintArtistName(g, clipY, clipHeight, currentTrack, false);
  }

  private void paintTrackInfoText(
      Graphics g,
      int clipY,
      int clipHeight,
      String text,
      String[] truncatedTextRef,
      Font font,
      int fontHeight,
      int yPosition,
      int color,
      boolean isLargeScreen) {
    g.setFont(font);
    if (intersects(clipY, clipHeight, yPosition, fontHeight)) {
      g.setColor(color);
      int maxWidth = calculateMaxTextWidth();
      if (truncatedTextRef[0] == null) {
        truncatedTextRef[0] = Utils.truncateText(text, font, maxWidth);
      }

      if (isLargeScreen) {
        drawTrackText(g, truncatedTextRef[0], textX, yPosition);
      } else {
        g.drawString(truncatedTextRef[0], textX, yPosition, 20);
      }
    }
  }

  private void paintTrackName(
      Graphics g, int clipY, int clipHeight, Track currentTrack, boolean isLargeScreen) {
    Font font = isLargeScreen ? titleFont : defaultFont;
    int fontHeight = isLargeScreen ? titleFont.getHeight() : textHeight;
    String[] truncatedRef = new String[] {truncatedTrackName};

    paintTrackInfoText(
        g,
        clipY,
        clipHeight,
        currentTrack.getName(),
        truncatedRef,
        font,
        fontHeight,
        titleY,
        Theme.getOnBackgroundColor(),
        isLargeScreen);

    truncatedTrackName = truncatedRef[0];
  }

  private void paintArtistName(
      Graphics g, int clipY, int clipHeight, Track currentTrack, boolean isLargeScreen) {
    Font font = isLargeScreen ? artistFont : defaultFont;
    int fontHeight = isLargeScreen ? artistFont.getHeight() : textHeight;
    String[] truncatedRef = new String[] {truncatedSinger};

    paintTrackInfoText(
        g,
        clipY,
        clipHeight,
        currentTrack.getArtist(),
        truncatedRef,
        font,
        fontHeight,
        artistY,
        Theme.getOnSurfaceVariantColor(),
        isLargeScreen);

    truncatedSinger = truncatedRef[0];
  }

  private int calculateMaxTextWidth() {
    if (isLandscape && isLargeScreen) {
      return calculateLandscapeTextWidth();
    }

    int padding = 20;
    int textOffset = !isLargeScreen ? textX : 0;
    return displayWidth - padding - textOffset;
  }

  private int calculateLandscapeTextWidth() {
    int leftPanelWidth = displayWidth * 45 / 100;
    int rightPanelLeft = leftPanelWidth + (displayWidth / 40);
    int rightPanelWidth = displayWidth - rightPanelLeft - (displayWidth / 30);
    return rightPanelWidth - 20;
  }

  private void paintTimeSlider(Graphics g, int clipY, int clipHeight) {
    long duration = getPlayerGUI().getDuration();
    long current = getPlayerGUI().getCurrentTime();

    String strDuration = getDurationText(duration);
    String strCurrent = Utils.formatTime(current);

    calculateSliderValue(duration, current, sliderWidth);

    if (isLargeScreen) {
      paintLargeScreenTimeSlider(g, clipY, clipHeight, strCurrent, strDuration);
    } else {
      paintSmallScreenTimeSlider(g, clipY, clipHeight, strCurrent, strDuration);
    }
  }

  private void paintLargeScreenTimeSlider(
      Graphics g, int clipY, int clipHeight, String strCurrent, String strDuration) {
    int currentTimeX, durationTimeX;

    if (isLandscape) {
      int leftPanelWidth = displayWidth * 45 / 100;
      int rightPanelLeft = leftPanelWidth + 15;
      int rightPanelWidth = displayWidth - rightPanelLeft - 20;
      sliderLeft = rightPanelLeft + 8;
      sliderWidth = rightPanelWidth - 16;
      currentTimeX = sliderLeft;
      durationTimeX = sliderLeft + sliderWidth;
    } else {
      currentTimeX = sliderLeft;
      durationTimeX = sliderLeft + sliderWidth;
    }

    paintSliderBar(g);
    paintTimeLabels(g, clipY, clipHeight, strCurrent, strDuration, currentTimeX, durationTimeX);
  }

  private void paintTimeLabels(
      Graphics g,
      int clipY,
      int clipHeight,
      String strCurrent,
      String strDuration,
      int currentTimeX,
      int durationTimeX) {
    if (intersects(clipY, clipHeight, timeY, textHeight)) {
      g.setColor(Theme.getOnSurfaceVariantColor());
      g.drawString(strCurrent, currentTimeX, timeY, 20);
      g.drawString(strDuration, durationTimeX, timeY, 24);
    }
  }

  private void paintSmallScreenTimeSlider(
      Graphics g, int clipY, int clipHeight, String strCurrent, String strDuration) {
    paintCurrentTime(g, clipY, clipHeight, strCurrent);
    paintSliderBar(g);
    paintDurationTime(g, clipY, clipHeight, strDuration);
  }

  private void paintTimeText(
      Graphics g, int clipY, int clipHeight, String timeText, int x, int anchor) {
    if (intersects(clipY, clipHeight, timeY, textHeight)) {
      g.setColor(Theme.getOnSurfaceVariantColor());
      g.drawString(timeText, x, timeY, anchor);
    }
  }

  private void paintCurrentTime(Graphics g, int clipY, int clipHeight, String strCurrent) {
    paintTimeText(g, clipY, clipHeight, strCurrent, 5, 20);
  }

  private void paintDurationTime(Graphics g, int clipY, int clipHeight, String strDuration) {
    paintTimeText(g, clipY, clipHeight, strDuration, displayWidth - 5, 24);
  }

  private void paintSliderBar(Graphics g) {
    g.setColor(Theme.getOutlineVariantColor());
    g.fillRect(sliderLeft, sliderTop, sliderWidth, sliderHeight);
    g.setColor(Theme.getPrimaryColor());
    g.fillRect(sliderLeft, sliderTop, (int) sliderValue, sliderHeight);
  }

  private void paintControlButtons(Graphics g) {
    initButtonPositions();

    paintPlayPauseButton(g);
    paintNavigationButtons(g);
    paintModeButtons(g);
  }

  private void paintPlayPauseButton(Graphics g) {
    boolean isPlaying = getPlayerGUI().isPlaying();
    Image playPauseImg = isPlaying ? Configuration.pauseIcon : Configuration.playIcon;
    if (playPauseImg != null) {
      g.drawImage(playPauseImg, playX, playY, 3);
    }
  }

  private void paintNavigationButtons(Graphics g) {
    Image prevImg = isLoading() ? Configuration.prevDimIcon : Configuration.prevIcon;
    if (prevImg != null) {
      g.drawImage(prevImg, prevX, prevY, 3);
    }

    Image nextImg = isLoading() ? Configuration.nextDimIcon : Configuration.nextIcon;
    if (nextImg != null) {
      g.drawImage(nextImg, nextX, nextY, 3);
    }
  }

  private void paintModeButtons(Graphics g) {
    int repeatMode = getPlayerGUI().getRepeatMode();
    Image repeatImg = getRepeatIcon(repeatMode);
    if (repeatImg != null) {
      g.drawImage(repeatImg, repeatX, repeatY, 3);
    }

    Image shuffleImg =
        getPlayerGUI().isShuffleEnabled()
            ? Configuration.shuffleIcon
            : Configuration.shuffleOffIcon;
    if (shuffleImg != null) {
      g.drawImage(shuffleImg, shuffleX, shuffleY, 3);
    }
  }

  private Image getRepeatIcon(int repeatMode) {
    if (Configuration.PLAYER_REPEAT_ONE == repeatMode) {
      return Configuration.repeatOneIcon;
    } else if (Configuration.PLAYER_REPEAT_ALL == repeatMode) {
      return Configuration.repeatIcon;
    } else {
      return Configuration.repeatOffIcon;
    }
  }

  private void initButtonPositions() {
    if (playX != 0) {
      return;
    }

    int[] positions = new int[5];
    positions[4] = buttonWidth; // Pass buttonWidth for calculations
    calculateButtonPositions(displayWidth, displayHeight, isLandscape, isLargeScreen, positions);

    playX = positions[0];
    playY = playTop;
    prevX = positions[1];
    prevY = playTop;
    nextX = positions[2];
    nextY = playTop;
    repeatX = positions[3];
    repeatY = playTop;
    shuffleX = positions[4];
    shuffleY = playTop;
  }

  private void paintVolumeAlert(Graphics g) {
    int alertWidth = displayWidth - 40;
    int alertHeight = 100;
    int alertX = 20;
    int alertY = (displayHeight - alertHeight) / 2;
    int barWidth = alertWidth - 40;
    int barX = alertX + 20;
    int barY = alertY + 40;
    int barHeight = 20;

    paintVolumeAlertBackground(g, alertX, alertY, alertWidth, alertHeight);
    paintVolumeAlertTitle(g, alertX, alertY, alertWidth);
    paintVolumeBar(g, barX, barY, barWidth, barHeight);
  }

  private void paintVolumeAlertBackground(
      Graphics g, int alertX, int alertY, int alertWidth, int alertHeight) {
    g.setColor(Theme.getSurfaceColor());
    g.fillRect(alertX, alertY, alertWidth, alertHeight);
    g.setColor(Theme.getOutlineColor());
    g.drawRect(alertX, alertY, alertWidth, alertHeight);
  }

  private void paintVolumeAlertTitle(Graphics g, int alertX, int alertY, int alertWidth) {
    g.setColor(Theme.getOnSurfaceColor());
    g.drawString(
        Lang.tr("player.volume"),
        alertX + alertWidth / 2,
        alertY + 15,
        Graphics.HCENTER | Graphics.TOP);
  }

  private void paintVolumeBar(Graphics g, int barX, int barY, int barWidth, int barHeight) {
    g.setColor(Theme.getOutlineVariantColor());
    g.fillRect(barX, barY, barWidth, barHeight);

    int progressWidth = (barWidth * currentVolumeLevel) / Configuration.PLAYER_MAX_VOLUME;
    g.setColor(Theme.getPrimaryColor());
    g.fillRect(barX, barY, progressWidth, barHeight);

    g.setColor(Theme.getOutlineColor());
    g.drawRect(barX, barY, barWidth, barHeight);
  }

  private void handleBackCommand() {
    if (volumeAlertShowing) {
      hideVolumeAlert();
    } else {
      navigator.back();
    }
  }

  private void handleCancelTimerCommand() {
    sleepTimerManager.cancelTimer();
    updateSleepTimerCommands();
  }

  private void togglePlayPause() {
    if (!isLoading()) {
      getPlayerGUI().toggle();
    }
  }

  private void toggleRepeat() {
    getPlayerGUI().toggleRepeat();
    updateDisplay();
  }

  private void toggleShuffle() {
    getPlayerGUI().toggleShuffle();
    updateDisplay();
  }

  private void stop() {
    getPlayerGUI().pause();
    getPlayerGUI().seek(0L);
    setStatusByKey(Configuration.PLAYER_STATUS_STOPPED);
  }

  private void next() {
    if (!isLoading()) {
      getPlayerGUI().next();
    }
  }

  private void previous() {
    if (!isLoading()) {
      getPlayerGUI().previous();
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
    Tracks currentTracks = getPlayerGUI().getCurrentTracks();
    if (isPlaylistEmpty(currentTracks)) {
      return;
    }

    String playlistTitle = title != null ? title : Lang.tr("player.show_playlist");
    TrackListScreen trackListScreen = new TrackListScreen(playlistTitle, currentTracks, navigator);
    trackListScreen.setSelectedIndex(getPlayerGUI().getCurrentIndex(), true);
    navigator.forward(trackListScreen);
  }

  private boolean isPlaylistEmpty(Tracks tracks) {
    return tracks == null || tracks.getTracks() == null || tracks.getTracks().length == 0;
  }

  private void showSleepTimerDialog() {
    SleepTimerForm form = new SleepTimerForm(navigator, new SleepTimerCallback());
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

  private void removeSleepTimerCommand() {
    if (sleepTimerManager.isActive()) {
      removeCommand(Commands.playerCancelTimer());
    } else {
      removeCommand(Commands.playerSleepTimer());
    }
  }

  private void handleTimerAction(int action) {
    if (action == SleepTimerForm.ACTION_STOP_PLAYBACK) {
      stop();
      navigator.showAlert(Lang.tr("timer.status.expired"), AlertType.INFO);
    } else if (action == SleepTimerForm.ACTION_EXIT_APP) {
      navigator.showAlert(Lang.tr("timer.status.expired"), AlertType.INFO);
    }
  }

  private void updateStatus(String s) {
    if (isValidPlayerStatus(s)) {
      realPlayerStatus = s;
    }

    if (!timerOverrideActive) {
      statusCurrent = s;
    } else if (isCriticalStatus(s)) {
      statusCurrent = s;
      timerOverrideActive = false;
    }
  }

  private boolean isValidPlayerStatus(String s) {
    return s != null
        && s.indexOf(Lang.tr("timer.status.remaining").substring(0, 3)) == -1
        && s.indexOf(Lang.tr("player.volume")) == -1;
  }

  private boolean isCriticalStatus(String status) {
    return status != null
        && (status.indexOf(Lang.tr("status.loading")) != -1
            || status.toLowerCase().indexOf("error") != -1
            || status.indexOf(Lang.tr("player.volume")) != -1);
  }

  private void setTimerOverride(String timerStatus) {
    timerOverrideActive = true;
    statusCurrent = timerStatus;
  }

  private void clearTimerOverride() {
    timerOverrideActive = false;
    statusCurrent = realPlayerStatus;
  }

  private void initializeFonts() {
    defaultFont = Font.getDefaultFont();
    titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    artistFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
  }

  private void updateFontsForLargeScreen() {
    titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    artistFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    textHeight = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM).getHeight();
  }

  private void detectOrientationChange(int w, int h) {
    if (w <= 0 || h <= 0) {
      return;
    }
    isLargeScreen = isLargeScreen(w, h);
    isLandscape = isLandscape(w, h);
  }

  private void calculateResponsiveSizes() {
    int[] sizes = new int[5];
    calculateResponsiveSizes(displayWidth, displayHeight, sizes);
    buttonWidth = sizes[0];
    buttonHeight = sizes[1];
    playButtonWidth = sizes[2];
    playButtonHeight = sizes[3];
    sliderHeight = sizes[4];
  }

  private void resetButtonPositions() {
    playX = 0;
  }

  private boolean canLoadAlbumArt() {
    return albumArtUrl != null && !loadingAlbumArt && !albumArtLoaded;
  }

  private void calculateSliderValue(long duration, long current, int sliderWidth) {
    sliderValue = duration > 0 ? (float) sliderWidth * current / duration : 0;
  }

  private String getDurationText(long duration) {
    if (lastDuration != duration) {
      lastDuration = duration;
      durationText = Utils.formatTime(duration);
    }
    return durationText;
  }

  public void resetDurationText() {
    lastDuration = -1;
    durationText = "";
  }

  private class ImageLoadCallback implements ImageLoadOperation.Listener {
    private void finishImageLoad() {
      scaledAlbumArt = null;
      lastScaledSize = -1;
      albumArtLoaded = true;
      loadingAlbumArt = false;
      currentImageLoadOperation = null;
    }

    public void onImageLoaded(Image image) {
      albumArt = image;
      finishImageLoad();
      if (albumArt != null) {
        updateDisplay();
      }
    }

    public void onImageLoadError(Exception e) {
      albumArt = null;
      finishImageLoad();
    }
  }

  private class SleepTimerCallback implements SleepTimerForm.SleepTimerCallback {
    public void onTimerSet(int durationMinutes, int action) {
      sleepTimerManager.setCallback(PlayerScreen.this);
      sleepTimerManager.startCountdownTimer(durationMinutes, action);
      navigator.back();
      navigator.showAlert(Lang.tr("timer.status.set"), AlertType.CONFIRMATION, PlayerScreen.this);
      updateSleepTimerCommands();
    }
  }
}
