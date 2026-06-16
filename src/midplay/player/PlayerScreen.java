package midplay.player;

import midplay.player.PlayerGUI;
import midplay.player.SleepTimerManager;
import midplay.store.Configuration;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.screen.PlaylistPickerScreen;
import midplay.ui.screen.SleepTimerForm;
import midplay.ui.screen.TrackListScreen;
import midplay.util.Lang;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import midplay.model.Track;
import midplay.model.Tracks;

public final class PlayerScreen extends Canvas
    implements CommandListener, SleepTimerManager.SleepTimerCallback {

  // Nokia media key codes
  private static final int KEY_MEDIA_PLAY = -20;
  private static final int KEY_MEDIA_PREVIOUS = -21;
  private static final int KEY_MEDIA_NEXT = -22;

  // Volume alert layout (must match between paintVolumeAlert and handleVolumeAlertTouch)
  static final int VOLUME_ALERT_MARGIN = 20;
  static final int VOLUME_ALERT_HEIGHT = 100;
  static final int VOLUME_BAR_INSET = 20;
  static final int VOLUME_BAR_TOP_OFFSET = 40;
  static final int VOLUME_BAR_HEIGHT = 20;

  // UI Utility methods
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

  // Read by the repaint Timer thread in onRepaintTick but written by the UI/
  // paint thread (sizeChanged / initDisplayMetrics); volatile guarantees the
  // timer sees the latest layout instead of a stale value after a resize/rotate.
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
  int playTop,
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
  int sliderLeft, sliderWidth = 12, sliderHeight = 6;
  volatile int sliderTop;

  // Reusable buffer for calculateButtonPositions; avoids per-paint allocation.
  final int[] buttonPositionCache = new int[5];

  final AlbumArtLoader albumArtLoader = new AlbumArtLoader(this);
  final TrackTextRenderer trackTextRenderer = new TrackTextRenderer(this);
  final PlayerPainter painter = new PlayerPainter(this);

  long lastDuration = -1;
  String durationText = "";
  // Cached formatted current-time label. Reparsed only when the second changes
  // instead of every paint (the per-second slider tick repaints this row).
  long lastCurrent = -1;
  String currentText = "";

  // True once initDisplayMetrics has produced a valid layout, false after a
  // sizeChanged until the next paint re-layouts. Lets the per-second repaint
  // tick target the slider/time row immediately instead of falling back to a
  // full repaint during the post-resize window.
  volatile boolean layoutValid = false;

  // Slider fill width in pixels (precomputed with integer math in the painter
  // — float boxing per paint is expensive on low-end J2ME devices).
  int sliderValue;

  private String title;
  private volatile PlayerGUI gui;
  Navigator navigator;

  public PlayerScreen(String title, Tracks tracks, int index, Navigator navigator) {
    this.title = title;
    this.navigator = navigator;

    setTitle(title);
    addCommands();
    setCommandListener(this);
    touchSupported = hasPointerEvents();
    painter.initializeFonts();
    change(title, tracks, index, navigator);
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

  public void change(String title, Tracks tracks, int index, Navigator navigator) {
    this.title = title;
    setTitle(title);
    this.navigator = navigator;
    PlayerGUI playerGUI = getPlayerGUI();
    if (playerGUI.setPlaylist(tracks, index)) {
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

  // Non-blocking refresh for callers that may run off the event/UI thread
  // (Timer thread, MMAPI playerUpdate callback, image-load worker). Plain
  // repaint() is safe from any thread; serviceRepaints() is not.
  public void updateDisplayAsync() {
    repaint();
  }

  // Called every ~1s by the repaint timer during playback. Only the slider
  // thumb and current-time label move, so repaint just that row instead of the
  // whole canvas — the biggest smoothness win on weak devices. The paint
  // pipeline honours the clip (intersects() checks) so album art / buttons /
  // track text are skipped outside the row.
  void onRepaintTick() {
    if (displayWidth <= 0 || displayHeight <= 0 || !layoutValid) {
      updateDisplayAsync();
      return;
    }
    int rowTop = sliderTop;
    int rowBottom = timeY + textHeight;
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
  }

  public synchronized PlayerGUI getPlayerGUI() {
    if (gui == null) {
      gui = new PlayerGUI(this);
    }
    return gui;
  }

  public void setAlbumArtUrl(String url) {
    albumArtLoader.setAlbumArtUrl(url);
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
      } else {
        handleButtonTouch(x, y);
      }
    } catch (Throwable e) {
      e.printStackTrace();
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
      // Only the status-bar text changes each tick; repaint just that rect.
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

  public void resetImage() {
    albumArtLoader.resetImage();
  }

  // Drop decoded album-art bitmaps so the heap is free before a memory-heavy
  // operation (creating a new media Player on low-heap devices). Keeps the URL
  // and load state, so images are re-decoded lazily on the next paint.
  public void freeImageHeap() {
    albumArtLoader.freeImageHeap();
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

  private void handleVolumeAlertTouch(int x, int y) {
    int alertWidth = displayWidth - 2 * VOLUME_ALERT_MARGIN;
    int alertHeight = VOLUME_ALERT_HEIGHT;
    int alertX = VOLUME_ALERT_MARGIN;
    int alertY = (displayHeight - alertHeight) / 2;
    int barWidth = alertWidth - 2 * VOLUME_BAR_INSET;
    int barX = alertX + VOLUME_BAR_INSET;
    int barY = alertY + VOLUME_BAR_TOP_OFFSET;
    int barHeight = VOLUME_BAR_HEIGHT;

    if (isPointInBounds(x, y, alertX, alertY, alertWidth, alertHeight)) {
      if (isPointInBounds(x, y, barX, barY, barWidth, barHeight)) {
        int tapPosition = x - barX;
        int newVolume = (tapPosition * Configuration.PLAYER_MAX_VOLUME) / barWidth;
        newVolume = Math.max(0, Math.min(Configuration.PLAYER_MAX_VOLUME, newVolume));
        getPlayerGUI().setVolumeLevel(newVolume);
        updateDisplay();
      }
    } else {
      hideVolumeAlert();
    }
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
    TrackListScreen trackListScreen = new TrackListScreen(playlistTitle, currentTracks, navigator);
    int index = getPlayerGUI().getCurrentIndex();
    int size = currentTracks.getTracks().length;
    if (size > 0) {
      if (index < 0) {
        index = 0;
      }
      if (index >= size) {
        index = size - 1;
      }
      trackListScreen.setSelectedIndex(index, true);
    }
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

  private void setTimerOverride(String timerStatus) {
    statusBar.setTimerOverride(timerStatus);
  }

  private void clearTimerOverride() {
    statusBar.clearTimerOverride();
  }

  public void resetDurationText() {
    painter.resetDurationText();
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
