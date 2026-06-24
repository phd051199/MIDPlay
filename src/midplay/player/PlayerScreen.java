package midplay.player;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.screen.PlaylistPickerScreen;
import midplay.ui.screen.QueueTrackListScreen;
import midplay.ui.screen.SleepTimerScreen;
import midplay.util.Lang;
import midplay.util.Utils;

public final class PlayerScreen extends Canvas
    implements CommandListener, SleepTimerManager.SleepTimerCallback {

  private static final int KEY_MEDIA_PLAY = -20;
  private static final int KEY_MEDIA_PREVIOUS = -21;
  private static final int KEY_MEDIA_NEXT = -22;

  static final int VOLUME_ALERT_MARGIN = 20;
  static final int VOLUME_ALERT_HEIGHT = 100;
  static final int VOLUME_BAR_INSET = 20;
  static final int VOLUME_BAR_TOP_OFFSET = 40;
  static final int VOLUME_BAR_HEIGHT = 20;

  static final class VolumeAlertLayout {
    final int alertX, alertY, alertWidth, alertHeight;
    final int barX, barY, barWidth, barHeight;

    VolumeAlertLayout(
        int alertX,
        int alertY,
        int alertWidth,
        int alertHeight,
        int barX,
        int barY,
        int barWidth,
        int barHeight) {
      this.alertX = alertX;
      this.alertY = alertY;
      this.alertWidth = alertWidth;
      this.alertHeight = alertHeight;
      this.barX = barX;
      this.barY = barY;
      this.barWidth = barWidth;
      this.barHeight = barHeight;
    }
  }

  VolumeAlertLayout volumeAlertLayout() {
    int alertWidth = displayWidth - 2 * VOLUME_ALERT_MARGIN;
    int alertHeight = VOLUME_ALERT_HEIGHT;
    int alertX = VOLUME_ALERT_MARGIN;
    int alertY = (displayHeight - alertHeight) / 2;
    int barWidth = alertWidth - 2 * VOLUME_BAR_INSET;
    int barX = alertX + VOLUME_BAR_INSET;
    int barY = alertY + VOLUME_BAR_TOP_OFFSET;
    int barHeight = VOLUME_BAR_HEIGHT;
    return new VolumeAlertLayout(
        alertX, alertY, alertWidth, alertHeight, barX, barY, barWidth, barHeight);
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

  private final SleepTimerManager sleepTimerManager = new SleepTimerManager();
  private final boolean touchSupported;
  private boolean seeking;
  private boolean volumeDragging;

  private static final int SEEK_REPEAT_THRESHOLD = 2;
  private static final long SEEK_STEP_MICROS = 2000000L;
  private int heldKey;
  private int heldAction;
  private int heldRepeatCount;
  private long pendingSeekMicros = -1;

  volatile int displayWidth = -1, displayHeight = -1;
  volatile int textHeight = 10;
  boolean isLandscape, isLargeScreen;
  boolean volumeAlertShowing;
  final StatusBar statusBar = new StatusBar();
  Font titleFont, artistFont, defaultFont;
  int statusBarHeight, albumSize = 72, albumX = 8, albumY;
  int textX, titleY, artistY, timeWidth;
  volatile int timeY;
  int buttonWidth = 40, buttonHeight = 40, playButtonWidth = 50, playButtonHeight = 50;
  int playTop, playX, playY, prevX, prevY, nextX, nextY, repeatX, repeatY, shuffleX, shuffleY;
  int sliderLeft, sliderWidth = 12, sliderHeight = 6;
  volatile int sliderTop;

  final AlbumArtLoader albumArtLoader = new AlbumArtLoader(this);
  final TrackTextRenderer trackTextRenderer = new TrackTextRenderer(this);
  final PlayerPainter painter = new PlayerPainter(this);

  long lastDuration = -1;
  String durationText = "";
  long lastCurrent = -1;
  String currentText = "";

  volatile boolean layoutValid = false;
  boolean buttonPositionsInitialized = false;

  int sliderValue;

  private String title;
  private volatile PlayerGUI gui;
  Navigator navigator;

  public PlayerScreen(
      String title, Tracks tracks, int index, long positionMicros, Navigator navigator) {
    this.title = title;
    this.navigator = navigator;

    setTitle(title);
    addCommands();
    setCommandListener(this);
    touchSupported = hasPointerEvents();
    painter.initializeFonts();
    change(title, tracks, index, positionMicros, navigator);
  }

  private void manageCommands(boolean add) {
    Command[] commands = {
      Commands.back(),
      Commands.playerPlay(),
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
      navNextAdded = false;
      navPrevAdded = false;
      updateNavCommands();
    } else {
      removeSleepTimerCommand();
      removeCommand(Commands.playerNext());
      removeCommand(Commands.playerPrevious());
      navNextAdded = false;
      navPrevAdded = false;
    }
  }

  public void addCommands() {
    manageCommands(true);
  }

  public void clearCommands() {
    manageCommands(false);
  }

  public void showNotify() {
    getPlayerGUI().resumeRepaintTimer();
    if (!albumArtLoader.albumArtLoaded && albumArtLoader.canLoadAlbumArt()) {
      albumArtLoader.loadAlbumArt();
    }
    String statusKey = statusBar.getCurrentStatusKey();
    if (Configuration.PLAYER_STATUS_PLAYING.equals(statusKey)
        || Configuration.PLAYER_STATUS_PAUSED.equals(statusKey)
        || Configuration.PLAYER_STATUS_STOPPED.equals(statusKey)) {
      determinePlayerStatus();
    }
  }

  public void hideNotify() {
    getPlayerGUI().pauseRepaintTimer();
    albumArtLoader.cancelImageLoads();
  }

  public void change(
      String title, Tracks tracks, int index, long positionMicros, Navigator navigator) {
    this.title = title;
    setTitle(title);
    this.navigator = navigator;
    PlayerGUI playerGUI = getPlayerGUI();
    if (playerGUI.setPlaylist(tracks, index)) {
      playerGUI.setPendingResumeSeek(positionMicros);
      playerGUI.play();
    }
  }

  public void setupDisplay() {
    displayHeight = -1;
    updateDisplayAsync();
  }

  public void setStatus(String s) {
    statusBar.setStatus(s);
    updateDisplayAsync();
  }

  public void setStatusByKey(String statusKey) {
    statusBar.setStatusByKey(statusKey);
    updateDisplayAsync();
  }

  String getStatusCurrent() {
    return statusBar.getStatusCurrent();
  }

  public void showVolumeAlert() {
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

  public void updateDisplayAsync() {
    repaint();
  }

  void onRepaintTick() {
    updateNavCommands();
    if (displayWidth <= 0 || displayHeight <= 0 || !layoutValid) {
      updateDisplayAsync();
      return;
    }
    int rowTop = Math.min(sliderTop, timeY);
    int rowBottom = Math.max(sliderTop + sliderHeight, timeY + textHeight);
    if (rowBottom <= rowTop) {
      updateDisplayAsync();
      return;
    }
    repaint(0, rowTop, displayWidth, rowBottom - rowTop);
  }

  protected void sizeChanged(int w, int h) {
    painter.detectOrientationChange(w, h);
    displayWidth = displayHeight = -1;
    layoutValid = false;
    albumArtLoader.scaledAlbumArt = null;
    albumArtLoader.lastScaledSize = -1;
    painter.resetButtonPositions();
    resetTruncatedText();
  }

  public void resetTruncatedText() {
    trackTextRenderer.resetTrackTextImages();
  }

  public void close() {
    albumArtLoader.resetImage();
    trackTextRenderer.resetTrackTextImages();
    sleepTimerManager.setCallback(null);
    sleepTimerManager.shutdown();
  }

  public synchronized PlayerGUI getPlayerGUI() {
    if (gui == null) {
      gui = new PlayerGUI(this);
    }
    return gui;
  }

  protected void keyPressed(int keycode) {
    try {
      int action = getGameAction(keycode);

      if (!volumeAlertShowing && (action == Canvas.LEFT || action == Canvas.RIGHT)) {
        if (heldKey == keycode) {
          onSeekRepeat();
        } else {
          heldKey = keycode;
          heldAction = action;
          heldRepeatCount = 1;
        }
        return;
      }

      if (action != 0) {
        handleAction(action);
      } else {
        handleMusicKey(keycode);
      }
    } catch (Throwable e) {
    }
  }

  protected void keyRepeated(int keycode) {
    try {
      if (heldKey == keycode
          && !volumeAlertShowing
          && (heldAction == Canvas.LEFT || heldAction == Canvas.RIGHT)) {
        onSeekRepeat();
      }
    } catch (Throwable e) {
    }
  }

  protected void keyReleased(int keycode) {
    try {
      if (keycode != heldKey) {
        return;
      }
      boolean wasTap = heldRepeatCount < SEEK_REPEAT_THRESHOLD;
      int action = heldAction;
      heldKey = 0;
      heldAction = 0;
      heldRepeatCount = 0;
      long pending = pendingSeekMicros;
      pendingSeekMicros = -1;
      if (wasTap) {
        if (action == Canvas.LEFT) {
          previous();
        } else if (action == Canvas.RIGHT) {
          next();
        }
      } else if (pending >= 0) {
        getPlayerGUI().seek(pending);
      }
    } catch (Throwable e) {
    }
  }

  private void onSeekRepeat() {
    heldRepeatCount++;
    if (heldRepeatCount < SEEK_REPEAT_THRESHOLD || heldAction == 0) {
      return;
    }
    PlayerGUI gui = getPlayerGUI();
    long duration = gui.getDuration();
    if (duration <= 0) {
      return;
    }
    if (pendingSeekMicros < 0) {
      pendingSeekMicros = gui.getCurrentTime();
    }
    long target =
        heldAction == Canvas.RIGHT
            ? pendingSeekMicros + SEEK_STEP_MICROS
            : pendingSeekMicros - SEEK_STEP_MICROS;
    previewClamped(target, duration);
  }

  private void previewClamped(long target, long duration) {
    if (target < 0) {
      target = 0;
    } else if (target > duration) {
      target = duration;
    }
    pendingSeekMicros = target;
    getPlayerGUI().seekPreview(target);
  }

  private void handleMusicKey(int code) {
    switch (code) {
      case KEY_MEDIA_PLAY:
        getPlayerGUI().toggle();
        break;
      case KEY_MEDIA_PREVIOUS:
        previous();
        break;
      case KEY_MEDIA_NEXT:
        next();
        break;
    }
  }

  protected void pointerPressed(int x, int y) {
    if (!touchSupported) {
      return;
    }

    try {
      if (volumeAlertShowing) {
        handleVolumeAlertTouch(x, y);
      } else if (isPointOnSlider(x, y)) {
        seeking = true;
        handleSliderSeek(x);
      } else {
        handleButtonTouch(x, y);
      }
    } catch (Throwable e) {
    }
  }

  protected void pointerDragged(int x, int y) {
    if (!touchSupported) {
      return;
    }
    try {
      if (volumeAlertShowing) {
        if (volumeDragging) {
          setVolumeFromBarX(x);
        }
        return;
      }
      if (seeking) {
        handleSliderSeek(x);
      }
    } catch (Throwable e) {
    }
  }

  protected void pointerReleased(int x, int y) {
    try {
      if (seeking) {
        seeking = false;
        if (pendingSeekMicros >= 0) {
          long pending = pendingSeekMicros;
          pendingSeekMicros = -1;
          getPlayerGUI().seek(pending);
        }
      }
      volumeDragging = false;
    } catch (Throwable e) {
    }
  }

  public void paint(Graphics g) {
    painter.paint(g);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == Commands.back()) {
      handleBackCommand();
    } else if (c == Commands.playerNext()) {
      next();
    } else if (c == Commands.playerPrevious()) {
      previous();
    } else if (c == Commands.playerPlay()) {
      getPlayerGUI().toggle();
    } else if (c == Commands.playerStop()) {
      stop();
    } else if (c == Commands.playerRepeat()) {
      toggleRepeat();
    } else if (c == Commands.playerShuffle()) {
      toggleShuffle();
    } else if (c == Commands.playerVolume()) {
      showVolumeAlert();
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
    clearTimerOverride();
    handleTimerAction(action);
    updateSleepTimerCommands();
    updateDisplayAsync();
  }

  public void onTimerUpdate(String remainingTime) {
    if (sleepTimerManager.isActive()) {
      setTimerOverride(Lang.tr("timer.sleep_timer") + ": " + remainingTime);
      if (displayWidth > 0 && statusBarHeight > 0) {
        repaint(0, 0, displayWidth, statusBarHeight);
      } else {
        updateDisplayAsync();
      }
    }
  }

  public void onTimerCancelled() {
    clearTimerOverride();
    navigator.showAlert(Lang.tr("timer.status.cancelled"), AlertType.CONFIRMATION);
    updateSleepTimerCommands();
    updateDisplayAsync();
  }

  public void showError(String message) {
    navigator.showAlert(message, AlertType.ERROR);
  }

  private void determinePlayerStatus() {
    PlayerGUI gui = getPlayerGUI();
    if (gui.isLoading()) {
      setStatusByKey(Configuration.PLAYER_STATUS_LOADING);
    } else if (gui.isPlaying()) {
      setStatusByKey(Configuration.PLAYER_STATUS_PLAYING);
    } else if (!gui.hasPlayer()) {
      setStatusByKey(Configuration.PLAYER_STATUS_STOPPED);
    } else {
      setStatusByKey(Configuration.PLAYER_STATUS_PAUSED);
    }
  }

  boolean isLoading() {
    return getPlayerGUI().isLoading();
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

  private boolean isPointOnSlider(int x, int y) {
    int touchHeight = Math.max(sliderHeight + 18, 24);
    int top = sliderTop + (sliderHeight >> 1) - (touchHeight >> 1);
    return x >= sliderLeft && x <= sliderLeft + sliderWidth && y >= top && y <= top + touchHeight;
  }

  private void handleSliderSeek(int x) {
    int left = sliderLeft;
    int width = sliderWidth;
    if (width <= 0) {
      return;
    }
    if (x < left) {
      x = left;
    } else if (x > left + width) {
      x = left + width;
    }
    PlayerGUI gui = getPlayerGUI();
    long duration = gui.getDuration();
    if (duration <= 0) {
      return;
    }
    previewClamped((duration * (long) (x - left)) / width, duration);
  }

  private void handleVolumeAlertTouch(int x, int y) {
    VolumeAlertLayout layout = volumeAlertLayout();
    if (isPointInBounds(
        x, y, layout.alertX, layout.alertY, layout.alertWidth, layout.alertHeight)) {
      if (isPointInBounds(x, y, layout.barX, layout.barY, layout.barWidth, layout.barHeight)) {
        volumeDragging = true;
        setVolumeFromBarX(x);
      }
    } else {
      hideVolumeAlert();
    }
  }

  private void setVolumeFromBarX(int x) {
    VolumeAlertLayout layout = volumeAlertLayout();
    if (x < layout.barX) {
      x = layout.barX;
    } else if (x > layout.barX + layout.barWidth) {
      x = layout.barX + layout.barWidth;
    }
    int tapPosition = x - layout.barX;
    int newVolume = (tapPosition * Configuration.PLAYER_MAX_VOLUME) / layout.barWidth;
    newVolume = Math.max(0, Math.min(Configuration.PLAYER_MAX_VOLUME, newVolume));
    getPlayerGUI().setVolumeLevel(newVolume);
    updateDisplay();
  }

  private void handleAction(int action) {
    if (isLoading() && !isQueueableNavigationAction(action)) {
      return;
    }

    try {
      if (volumeAlertShowing) {
        handleVolumeAction(action);
      } else {
        handlePlayerAction(action);
      }
    } catch (Throwable e) {
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

  private boolean isQueueableNavigationAction(int action) {
    return action == Canvas.RIGHT || action == Canvas.LEFT;
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

  private void toggleRepeat() {
    getPlayerGUI().toggleRepeat();
    updateDisplay();
  }

  private void toggleShuffle() {
    getPlayerGUI().toggleShuffle();
    updateDisplay();
  }

  private void stop() {
    getPlayerGUI().stop();
  }

  private void next() {
    getPlayerGUI().next();
  }

  private void previous() {
    getPlayerGUI().previous();
  }

  private void addCurrentTrackToPlaylist() {
    Track currentTrack = getPlayerGUI().getCurrentTrack();
    if (currentTrack == null) {
      return;
    }

    PlaylistPickerScreen selectionScreen = new PlaylistPickerScreen(navigator, currentTrack, this);
    navigator.forward(selectionScreen);
  }

  private void showCurrentPlaylist() {
    Tracks currentTracks = getPlayerGUI().getCurrentTracks();
    if (isPlaylistEmpty(currentTracks)) {
      return;
    }

    String playlistTitle = title != null ? title : Lang.tr("player.show_playlist");
    QueueTrackListScreen trackListScreen =
        new QueueTrackListScreen(playlistTitle, currentTracks, navigator);
    Utils.clampAndSelect(trackListScreen, getPlayerGUI().getCurrentIndex());
    navigator.forward(trackListScreen);
  }

  private boolean isPlaylistEmpty(Tracks tracks) {
    return tracks == null || tracks.getTracks() == null || tracks.getTracks().length == 0;
  }

  private void showSleepTimerDialog() {
    SleepTimerScreen form = new SleepTimerScreen(navigator, new SleepTimerCallback());
    navigator.forward(form);
  }

  private boolean navNextAdded;
  private boolean navPrevAdded;

  private void updateNavCommands() {
    PlayerGUI gui = getPlayerGUI();
    if (gui == null) {
      return;
    }
    boolean wantNext = gui.canSkipForward();
    boolean wantPrev = gui.canSkipBackward();
    if (wantNext != navNextAdded) {
      if (wantNext) {
        addCommand(Commands.playerNext());
      } else {
        removeCommand(Commands.playerNext());
      }
      navNextAdded = wantNext;
    }
    if (wantPrev != navPrevAdded) {
      if (wantPrev) {
        addCommand(Commands.playerPrevious());
      } else {
        removeCommand(Commands.playerPrevious());
      }
      navPrevAdded = wantPrev;
    }
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
    if (action == SleepTimerScreen.ACTION_STOP_PLAYBACK) {
      stop();
      navigator.showAlert(Lang.tr("timer.status.expired"), AlertType.INFO);
    } else if (action == SleepTimerScreen.ACTION_EXIT_APP) {
      navigator.showAlert(Lang.tr("timer.status.expired"), AlertType.INFO);
    }
  }

  private void setTimerOverride(String timerStatus) {
    statusBar.setTimerOverride(timerStatus);
  }

  private void clearTimerOverride() {
    statusBar.clearTimerOverride();
  }

  private class SleepTimerCallback implements SleepTimerScreen.SleepTimerCallback {
    public void onTimerSet(int durationMinutes, int action) {
      sleepTimerManager.setCallback(PlayerScreen.this);
      sleepTimerManager.startCountdownTimer(durationMinutes, action);
      navigator.back();
      navigator.showAlert(Lang.tr("timer.status.set"), AlertType.CONFIRMATION, PlayerScreen.this);
      updateSleepTimerCommands();
    }
  }
}
