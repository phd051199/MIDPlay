package midplay.player;

import midplay.store.Configuration;
import midplay.ui.Theme;
import midplay.util.Lang;
import midplay.util.Utils;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import midplay.model.Track;

public final class PlayerPainter {

  // Landscape left-panel width as a percentage of screen width
  // (shared across button / track-info / slider calculations)
  private static final int LANDSCAPE_LEFT_PANEL_PCT = 45;

  // drawString / paintTimeText anchor combos, named so the bare ints (17/20/24)
  // aren't scattered through the paint code.
  private static final int ANCHOR_HCENTER_TOP = Graphics.HCENTER | Graphics.TOP;
  private static final int ANCHOR_LEFT_TOP = Graphics.LEFT | Graphics.TOP;
  private static final int ANCHOR_RIGHT_TOP = Graphics.RIGHT | Graphics.TOP;

  private final PlayerScreen screen;

  // Cached faded-artist color, invalidated automatically when the underlying
  // theme colors change (recomputing the blend on every paint is pure CPU
  // waste — theme colors change rarely).
  private int fadedArtistColor;
  private int fadedArtistFgKey = -1;
  private int fadedArtistBgKey = -1;

  PlayerPainter(PlayerScreen screen) {
    this.screen = screen;
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
      int leftPanelWidth = screenWidth * LANDSCAPE_LEFT_PANEL_PCT / 100;
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

  void paint(Graphics g) {
    try {
      ensureFontsInitialized();
      if (screen.displayHeight == -1) {
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

      if (screen.volumeAlertShowing) {
        paintVolumeAlert(g);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void drawTrackText(
      Graphics g, String textToDraw, int yPosition, int color, boolean isLargeScreen) {
    g.setColor(color);
    int anchor = isLargeScreen ? (Graphics.HCENTER | Graphics.TOP) : (Graphics.LEFT | Graphics.TOP);
    g.drawString(textToDraw, screen.textX, yPosition, anchor);
  }

  private void initDisplayMetrics(Graphics g) {
    ensureFontsInitialized();
    screen.displayWidth = screen.getWidth();
    screen.displayHeight = screen.getHeight();
    detectOrientationChange(screen.displayWidth, screen.displayHeight);

    if (screen.isLargeScreen) {
      initLargeScreenLayout(g);
    } else {
      initSmallScreenLayout(g);
    }
    // Compute button hit-rects during layout so they are valid before the
    // first paint and before any pointer event (handleButtonTouch).
    initButtonPositions();
    // Layout is now consistent — the per-second repaint tick can target just
    // the slider/time row instead of falling back to a full repaint.
    screen.layoutValid = true;
  }

  private void initLargeScreenLayout(Graphics g) {
    calculateResponsiveSizes();
    updateFontsForLargeScreen();
    screen.statusBarHeight = screen.textHeight + (screen.displayHeight / 100);

    if (screen.isLandscape) {
      initLargeLandscapeLayout(g);
    } else {
      initLargePortraitLayout(g);
    }
    screen.albumArtLoader.loadAlbumArt();
  }

  private void initSmallScreenLayout(Graphics g) {
    ensureFontsInitialized();
    screen.textHeight = screen.defaultFont.getHeight();
    screen.statusBarHeight = screen.textHeight + 5;

    calculateSmallScreenPositions();
    calculateSmallScreenSlider(g);
    calculateSmallScreenArt();
    screen.textX = screen.albumX + screen.albumSize + 16;
    screen.albumArtLoader.loadAlbumArt();
  }

  private void calculateSmallScreenPositions() {
    int currentTop = 2 + screen.statusBarHeight + 15;
    screen.titleY = currentTop;
    currentTop += 5 + screen.textHeight;
    screen.artistY = currentTop;
    currentTop += 10 + screen.textHeight + 24;
    screen.timeY = currentTop;
    screen.playTop = screen.timeY + screen.textHeight + 24;
  }

  private void calculateSmallScreenSlider(Graphics g) {
    screen.timeWidth = g.getFont().stringWidth("0:00:0  ");
    screen.sliderWidth = screen.displayWidth - (screen.timeWidth * 2) - 8;
    screen.sliderTop = screen.timeY + (screen.textHeight - screen.sliderHeight) / 2;
    screen.sliderLeft = screen.timeWidth + 4;
  }

  private void calculateSmallScreenArt() {
    screen.albumSize = 72;
    screen.albumX = 8;
    screen.albumY = 2 + screen.statusBarHeight + 8;
  }

  private void initLargeLandscapeLayout(Graphics g) {
    int margin = screen.displayWidth / 30;
    int availableHeight = screen.displayHeight - screen.statusBarHeight - margin * 2;
    int leftPanelWidth = screen.displayWidth * LANDSCAPE_LEFT_PANEL_PCT / 100;

    calculateLandscapeArtDimensions(margin, availableHeight, leftPanelWidth);
    calculateLandscapeTrackInfo(leftPanelWidth, availableHeight);
    calculateLandscapeSlider(leftPanelWidth, margin, availableHeight);
    calculateLandscapeControls(availableHeight);
  }

  private void calculateLandscapeArtDimensions(
      int margin, int availableHeight, int leftPanelWidth) {
    screen.albumSize = Math.min(availableHeight * 85 / 100, leftPanelWidth - margin * 2);
    screen.albumX = (leftPanelWidth - screen.albumSize) / 2;
    screen.albumY = screen.statusBarHeight + (availableHeight - screen.albumSize) / 2;
  }

  private void calculateLandscapeTrackInfo(int leftPanelWidth, int availableHeight) {
    int rightPanelLeft = leftPanelWidth + (screen.displayWidth / 40);
    int rightPanelWidth = screen.displayWidth - rightPanelLeft - (screen.displayWidth / 30);
    int rightPanelCenter = rightPanelLeft + (rightPanelWidth >> 1);

    screen.textX = rightPanelCenter;
    int trackInfoSection = availableHeight / 4;
    screen.titleY = screen.statusBarHeight + trackInfoSection - screen.titleFont.getHeight();
    screen.artistY = screen.titleY + (screen.displayHeight / 80) + screen.titleFont.getHeight();
  }

  private void calculateLandscapeSlider(int leftPanelWidth, int margin, int availableHeight) {
    int rightPanelLeft = leftPanelWidth + (screen.displayWidth / 40);
    int rightPanelWidth = screen.displayWidth - rightPanelLeft - margin;

    int timeSliderSection = availableHeight / 2;
    screen.sliderTop = screen.statusBarHeight + timeSliderSection - screen.sliderHeight / 2;
    screen.timeWidth = screen.defaultFont.stringWidth("0:00:0  ");
    screen.sliderLeft = rightPanelLeft + (screen.displayWidth / 80);
    screen.sliderWidth = rightPanelWidth - (screen.displayWidth / 40);

    screen.timeY = screen.sliderTop + screen.sliderHeight + (screen.displayHeight / 60);
  }

  private void calculateLandscapeControls(int availableHeight) {
    int controlSection = availableHeight * 3 / 4;
    screen.playTop = screen.statusBarHeight + controlSection + (screen.displayHeight / 80);
  }

  private void initLargePortraitLayout(Graphics g) {
    int margin = screen.displayWidth / 20;
    screen.albumSize = Math.min(screen.displayWidth - margin * 2, (screen.displayHeight * 40) / 100);
    screen.albumX = (screen.displayWidth - screen.albumSize) / 2;

    int availableHeight = screen.displayHeight - screen.statusBarHeight;
    int contentHeight = calculateContentHeight();

    int startY = screen.statusBarHeight + (availableHeight - contentHeight) / 2;
    if (startY < screen.statusBarHeight + (screen.displayHeight / 30)) {
      startY = screen.statusBarHeight + (screen.displayHeight / 30);
    }

    calculatePortraitPositions(startY, margin);
  }

  private void calculatePortraitPositions(int startY, int margin) {
    screen.albumY = startY;
    int currentTop = startY + screen.albumSize + (screen.displayHeight / 18);

    screen.textX = screen.displayWidth / 2;
    screen.titleY = currentTop;
    currentTop += (screen.displayHeight / 80) + screen.titleFont.getHeight();
    screen.artistY = currentTop;
    currentTop += (screen.displayHeight / 50) + screen.artistFont.getHeight() + (screen.displayHeight / 25);

    screen.sliderLeft = margin;
    screen.sliderWidth = screen.displayWidth - margin * 2;
    screen.sliderTop = currentTop + (screen.textHeight - screen.sliderHeight) / 2;

    screen.timeY = screen.sliderTop + screen.sliderHeight + (screen.displayHeight / 60);
    screen.timeWidth = screen.defaultFont.stringWidth("0:00:0  ");
    screen.playTop = screen.timeY + screen.textHeight + (screen.displayHeight / 20);
  }

  private int calculateContentHeight() {
    return screen.albumSize
        + (screen.displayHeight / 18)
        + (screen.displayHeight / 80)
        + screen.titleFont.getHeight()
        + (screen.displayHeight / 50)
        + screen.artistFont.getHeight()
        + (screen.displayHeight / 25)
        + screen.sliderHeight
        + (screen.displayHeight / 60)
        + screen.textHeight
        + (screen.displayHeight / 20)
        + screen.playButtonHeight;
  }

  private void paintBackground(Graphics g) {
    // Fill only the dirty clip, not the whole canvas. The per-second slider
    // tick repaints a ~20px row; clearing all 240x320 pixels each tick is pure
    // memory-bandwidth waste on a slow device. Outside the clip the canvas
    // retains its previous content, so this is also correct for full repaints.
    g.setColor(Theme.getBackgroundColor());
    g.fillRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
  }

  private void paintStatusBar(Graphics g, int clipY, int clipHeight) {
    if (!intersects(clipY, clipHeight, 0, screen.statusBarHeight)) {
      return;
    }
    g.setColor(Theme.getSecondaryContainerColor());
    g.fillRect(0, 0, screen.displayWidth, screen.statusBarHeight);

    g.setColor(Theme.getOnSecondaryContainerColor());
    g.drawString(screen.getStatusCurrent(), screen.displayWidth >> 1, 2, ANCHOR_HCENTER_TOP);
  }

  private void paintAlbumArt(Graphics g) {
    if (screen.isLargeScreen) {
      paintLargeScreenAlbumArt(g);
    } else {
      paintSmallScreenAlbumArt(g);
    }
  }

  private void paintLargeScreenAlbumArt(Graphics g) {
    drawAlbumArtBackground(g);

    if (screen.albumArtLoader.albumArt != null) {
      // No-op once the load callback has pre-scaled; only resizes here on a
      // size change (rare) so the common path stays allocation-free in paint.
      screen.albumArtLoader.prepareScaledAlbumArt();
      Image scaled = screen.albumArtLoader.scaledAlbumArt;
      if (scaled != null) {
        g.drawImage(scaled, screen.albumX + 1, screen.albumY + 1, Graphics.LEFT | Graphics.TOP);
      }
    } else {
      drawAlbumArtPlaceholder(g, screen.albumX + screen.albumSize / 2, screen.albumY + screen.albumSize / 2);
    }
  }

  private void paintSmallScreenAlbumArt(Graphics g) {
    if (screen.albumArtLoader.albumArt != null) {
      g.drawImage(screen.albumArtLoader.albumArt, 8 + 36, screen.albumY + 36, Graphics.HCENTER | Graphics.VCENTER);
      g.setColor(Theme.getOutlineColor());
      g.drawRect(8, screen.albumY, 72, 72);
    } else {
      g.setColor(Theme.getSurfaceVariantColor());
      g.fillRect(8, screen.albumY, 72, 72);
      g.setColor(Theme.getOutlineColor());
      g.drawRect(8, screen.albumY, 72, 72);
      drawAlbumArtPlaceholder(g, 44, screen.albumY + 36);
    }
  }

  private void drawAlbumArtBackground(Graphics g) {
    g.setColor(Theme.getSurfaceVariantColor());
    g.fillRect(screen.albumX, screen.albumY, screen.albumSize, screen.albumSize);
    g.setColor(Theme.getOutlineColor());
    g.drawRect(screen.albumX, screen.albumY, screen.albumSize, screen.albumSize);
  }

  private void drawAlbumArtPlaceholder(Graphics g, int centerX, int centerY) {
    g.setColor(Theme.getOnSurfaceVariantColor());
    g.drawString("♪", centerX, centerY, ANCHOR_HCENTER_TOP);
  }

  private void paintTrackInfo(Graphics g, int clipY, int clipHeight) {
    // Skip the (synchronized) track lookup entirely when the repaint clip is
    // outside the track-info band. The per-second slider tick repaints only the
    // time row, so this avoids acquiring the PlayerGUI monitor each tick for a
    // track whose rows aren't even on screen.
    int bandTop = screen.titleY;
    int bandFontHeight = screen.isLargeScreen ? screen.artistFont.getHeight() : screen.textHeight;
    int bandHeight = (screen.artistY + bandFontHeight) - bandTop;
    if (bandHeight <= 0 || !intersects(clipY, clipHeight, bandTop, bandHeight)) {
      return;
    }

    Track currentTrack = screen.getPlayerGUI().getCurrentTrack();
    if (currentTrack == null) {
      return;
    }

    if (screen.isLargeScreen) {
      paintLargeScreenTrackInfo(g, clipY, clipHeight, currentTrack);
    } else {
      paintSmallScreenTrackInfo(g, clipY, clipHeight, currentTrack);
    }
  }

  private void paintLargeScreenTrackInfo(
      Graphics g, int clipY, int clipHeight, Track currentTrack) {
    paintTrackName(g, clipY, clipHeight, currentTrack, true);
    paintArtistName(g, clipY, clipHeight, currentTrack, true);
    g.setFont(screen.defaultFont);
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
      Font font,
      int fontHeight,
      int fontSize,
      int yPosition,
      int color,
      boolean isLargeScreen,
      TrackTextRenderer.TextImageSlot textImageSlot) {
    g.setFont(font);
    if (intersects(clipY, clipHeight, yPosition, fontHeight)) {
      int maxWidth = calculateMaxTextWidth();
      if (!screen.trackTextRenderer.shouldRenderWithTakumi(textImageSlot, text)) {
        // Latin text: stop any stale CJK load and drop a cached CJK image, but
        // keep the truncate cache (drawn text is stable across paints).
        textImageSlot.clearImage();
        String textToDraw =
            screen.trackTextRenderer.truncateTextForWidth(textImageSlot, text, font, maxWidth);
        drawTrackText(g, textToDraw, yPosition, color, isLargeScreen);
        return;
      }

      Image renderedTextImage =
          screen.trackTextRenderer.resolveTrackTextImage(textImageSlot, text, maxWidth, fontSize, color, isLargeScreen);

      if (renderedTextImage != null) {
        screen.trackTextRenderer.drawTrackTextImage(g, renderedTextImage, screen.textX, yPosition, isLargeScreen);
      }
    }
  }

  private void paintTrackName(
      Graphics g, int clipY, int clipHeight, Track currentTrack, boolean isLargeScreen) {
    Font font = isLargeScreen ? screen.titleFont : screen.defaultFont;
    int fontHeight = isLargeScreen ? screen.titleFont.getHeight() : screen.textHeight;
    int fontSize = fontHeight;

    paintTrackInfoText(
        g,
        clipY,
        clipHeight,
        currentTrack.getName(),
        font,
        fontHeight,
        fontSize,
        screen.titleY,
        Theme.getOnBackgroundColor(),
        isLargeScreen,
        screen.trackTextRenderer.trackNameTextImage);
  }

  private void paintArtistName(
      Graphics g, int clipY, int clipHeight, Track currentTrack, boolean isLargeScreen) {
    Font font = isLargeScreen ? screen.artistFont : screen.defaultFont;
    int fontHeight = isLargeScreen ? screen.artistFont.getHeight() : screen.textHeight;
    int titleFontSize = isLargeScreen ? screen.titleFont.getHeight() : screen.textHeight;
    int fontSize = fontHeight - 2;
    if (fontSize >= titleFontSize) {
      fontSize = titleFontSize - 1;
    }
    if (fontSize < 8) {
      fontSize = 8;
    }

    paintTrackInfoText(
        g,
        clipY,
        clipHeight,
        currentTrack.getArtist(),
        font,
        fontHeight,
        fontSize,
        screen.artistY,
        getFadedArtistColor(),
        isLargeScreen,
        screen.trackTextRenderer.artistTextImage);
  }

  private int getFadedArtistColor() {
    int fg = Theme.getOnSurfaceVariantColor();
    int bg = Theme.getBackgroundColor();
    if (fg != fadedArtistFgKey || bg != fadedArtistBgKey) {
      fadedArtistFgKey = fg;
      fadedArtistBgKey = bg;
      fadedArtistColor = blendColorWithBackground(fg, bg, 15);
    }
    return fadedArtistColor;
  }

  private int blendColorWithBackground(
      int color, int backgroundColor, int backgroundWeightPercent) {
    if (backgroundWeightPercent < 0) {
      backgroundWeightPercent = 0;
    } else if (backgroundWeightPercent > 100) {
      backgroundWeightPercent = 100;
    }

    int colorWeight = 100 - backgroundWeightPercent;
    int r =
        ((((color >> 16) & 0xFF) * colorWeight)
                + (((backgroundColor >> 16) & 0xFF) * backgroundWeightPercent))
            / 100;
    int g =
        ((((color >> 8) & 0xFF) * colorWeight)
                + (((backgroundColor >> 8) & 0xFF) * backgroundWeightPercent))
            / 100;
    int b =
        (((color & 0xFF) * colorWeight) + ((backgroundColor & 0xFF) * backgroundWeightPercent))
            / 100;
    return (r << 16) | (g << 8) | b;
  }

  private int calculateMaxTextWidth() {
    if (screen.isLandscape && screen.isLargeScreen) {
      return calculateLandscapeTextWidth();
    }

    int padding = 20;
    int textOffset = !screen.isLargeScreen ? screen.textX : 0;
    return screen.displayWidth - padding - textOffset;
  }

  private int calculateLandscapeTextWidth() {
    int leftPanelWidth = screen.displayWidth * LANDSCAPE_LEFT_PANEL_PCT / 100;
    int rightPanelLeft = leftPanelWidth + (screen.displayWidth / 40);
    int rightPanelWidth = screen.displayWidth - rightPanelLeft - (screen.displayWidth / 30);
    return rightPanelWidth - 20;
  }

  private void paintTimeSlider(Graphics g, int clipY, int clipHeight) {
    // Skip the (synchronized + native MMAPI) duration/current-time lookups
    // entirely when the repaint clip is outside the slider+time band. The
    // per-second tick repaints only this row; a status-bar-only or button-only
    // repaint must not pay for getDuration()/getCurrentTime() each time.
    int bandTop = screen.sliderTop;
    int bandBottom = screen.timeY + screen.textHeight;
    if (bandBottom > bandTop && !intersects(clipY, clipHeight, bandTop, bandBottom - bandTop)) {
      return;
    }

    long duration = screen.getPlayerGUI().getDuration();
    long current = screen.getPlayerGUI().getCurrentTime();

    String strDuration = getDurationText(duration);
    String strCurrent = getCurrentTimeText(current);

    calculateSliderValue(duration, current, screen.sliderWidth);

    if (screen.isLargeScreen) {
      paintLargeScreenTimeSlider(g, clipY, clipHeight, strCurrent, strDuration);
    } else {
      paintSmallScreenTimeSlider(g, clipY, clipHeight, strCurrent, strDuration);
    }
  }

  private void paintLargeScreenTimeSlider(
      Graphics g, int clipY, int clipHeight, String strCurrent, String strDuration) {
    int currentTimeX, durationTimeX;

    // sliderLeft/sliderWidth are computed once during layout (see
    // calculateLandscapeSlider / calculatePortraitPositions); paint must only
    // read them — recomputing here diverged from the layout and shifted the
    // bar relative to the thumb.
    currentTimeX = screen.sliderLeft;
    durationTimeX = screen.sliderLeft + screen.sliderWidth;

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
    paintTimeText(g, clipY, clipHeight, strCurrent, currentTimeX, ANCHOR_LEFT_TOP);
    paintTimeText(g, clipY, clipHeight, strDuration, durationTimeX, ANCHOR_RIGHT_TOP);
  }

  private void paintSmallScreenTimeSlider(
      Graphics g, int clipY, int clipHeight, String strCurrent, String strDuration) {
    paintCurrentTime(g, clipY, clipHeight, strCurrent);
    paintSliderBar(g);
    paintDurationTime(g, clipY, clipHeight, strDuration);
  }

  private void paintTimeText(
      Graphics g, int clipY, int clipHeight, String timeText, int x, int anchor) {
    if (intersects(clipY, clipHeight, screen.timeY, screen.textHeight)) {
      g.setColor(Theme.getOnSurfaceVariantColor());
      g.drawString(timeText, x, screen.timeY, anchor);
    }
  }

  private void paintCurrentTime(Graphics g, int clipY, int clipHeight, String strCurrent) {
    paintTimeText(g, clipY, clipHeight, strCurrent, 5, ANCHOR_LEFT_TOP);
  }

  private void paintDurationTime(Graphics g, int clipY, int clipHeight, String strDuration) {
    paintTimeText(g, clipY, clipHeight, strDuration, screen.displayWidth - 5, ANCHOR_RIGHT_TOP);
  }

  private void paintSliderBar(Graphics g) {
    g.setColor(Theme.getOutlineVariantColor());
    g.fillRect(screen.sliderLeft, screen.sliderTop, screen.sliderWidth, screen.sliderHeight);
    g.setColor(Theme.getPrimaryColor());
    g.fillRect(screen.sliderLeft, screen.sliderTop, screen.sliderValue, screen.sliderHeight);
  }

  private void paintControlButtons(Graphics g) {
    initButtonPositions();

    paintPlayPauseButton(g);
    paintNavigationButtons(g);
    paintModeButtons(g);
  }

  private void paintPlayPauseButton(Graphics g) {
    boolean isPlaying = screen.getPlayerGUI().isPlaying();
    Image playPauseImg = isPlaying ? Configuration.pauseIcon : Configuration.playIcon;
    if (playPauseImg != null) {
      g.drawImage(playPauseImg, screen.playX, screen.playY, 3);
    }
  }

  private void paintNavigationButtons(Graphics g) {
    // Both prev/next share the same dim state, so sample the (synchronized)
    // loading flag once instead of acquiring the monitor twice per paint.
    boolean loading = screen.isLoading();
    Image prevImg = loading ? Configuration.prevDimIcon : Configuration.prevIcon;
    if (prevImg != null) {
      g.drawImage(prevImg, screen.prevX, screen.prevY, 3);
    }

    Image nextImg = loading ? Configuration.nextDimIcon : Configuration.nextIcon;
    if (nextImg != null) {
      g.drawImage(nextImg, screen.nextX, screen.nextY, 3);
    }
  }

  private void paintModeButtons(Graphics g) {
    int repeatMode = screen.getPlayerGUI().getRepeatMode();
    Image repeatImg = getRepeatIcon(repeatMode);
    if (repeatImg != null) {
      g.drawImage(repeatImg, screen.repeatX, screen.repeatY, 3);
    }

    Image shuffleImg =
        screen.getPlayerGUI().isShuffleEnabled()
            ? Configuration.shuffleIcon
            : Configuration.shuffleOffIcon;
    if (shuffleImg != null) {
      g.drawImage(shuffleImg, screen.shuffleX, screen.shuffleY, 3);
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
    if (screen.playX != 0) {
      return;
    }

    int[] positions = screen.buttonPositionCache;
    positions[4] = screen.buttonWidth; // Pass buttonWidth for calculations
    calculateButtonPositions(screen.displayWidth, screen.displayHeight, screen.isLandscape, screen.isLargeScreen, positions);

    screen.playX = positions[0];
    screen.playY = screen.playTop;
    screen.prevX = positions[1];
    screen.prevY = screen.playTop;
    screen.nextX = positions[2];
    screen.nextY = screen.playTop;
    screen.repeatX = positions[3];
    screen.repeatY = screen.playTop;
    screen.shuffleX = positions[4];
    screen.shuffleY = screen.playTop;
  }

  private void paintVolumeAlert(Graphics g) {
    int alertWidth = screen.displayWidth - 2 * screen.VOLUME_ALERT_MARGIN;
    int alertHeight = screen.VOLUME_ALERT_HEIGHT;
    int alertX = screen.VOLUME_ALERT_MARGIN;
    int alertY = (screen.displayHeight - alertHeight) / 2;
    int barWidth = alertWidth - 2 * screen.VOLUME_BAR_INSET;
    int barX = alertX + screen.VOLUME_BAR_INSET;
    int barY = alertY + screen.VOLUME_BAR_TOP_OFFSET;
    int barHeight = screen.VOLUME_BAR_HEIGHT;

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

    int progressWidth = (barWidth * screen.getPlayerGUI().getVolumeLevel()) / Configuration.PLAYER_MAX_VOLUME;
    g.setColor(Theme.getPrimaryColor());
    g.fillRect(barX, barY, progressWidth, barHeight);

    g.setColor(Theme.getOutlineColor());
    g.drawRect(barX, barY, barWidth, barHeight);
  }

  void initializeFonts() {
    screen.defaultFont = Font.getDefaultFont();
    screen.titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    screen.artistFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
  }

  void ensureFontsInitialized() {
    if (screen.defaultFont == null || screen.titleFont == null || screen.artistFont == null) {
      initializeFonts();
    }
  }

  void updateFontsForLargeScreen() {
    screen.titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    screen.artistFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    screen.textHeight = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM).getHeight();
  }

  void detectOrientationChange(int w, int h) {
    if (w <= 0 || h <= 0) {
      return;
    }
    screen.isLargeScreen = isLargeScreen(w, h);
    screen.isLandscape = isLandscape(w, h);
  }

  private void calculateResponsiveSizes() {
    int[] sizes = new int[5];
    calculateResponsiveSizes(screen.displayWidth, screen.displayHeight, sizes);
    screen.buttonWidth = sizes[0];
    screen.buttonHeight = sizes[1];
    screen.playButtonWidth = sizes[2];
    screen.playButtonHeight = sizes[3];
    screen.sliderHeight = sizes[4];
  }

  void resetButtonPositions() {
    screen.playX = 0;
  }

  private void calculateSliderValue(long duration, long current, int sliderWidth) {
    if (duration <= 0 || sliderWidth <= 0) {
      screen.sliderValue = 0;
      return;
    }
    // Integer-only fixed ratio: avoids per-paint float boxing on low-end J2ME.
    // current/duration is computed in 64-bit (current is long) then clamped so
    // the fill never overshoots when current exceeds the metadata duration.
    long fill = (sliderWidth * current) / duration;
    if (fill > sliderWidth) {
      fill = sliderWidth;
    } else if (fill < 0) {
      fill = 0;
    }
    screen.sliderValue = (int) fill;
  }

  private String getDurationText(long duration) {
    if (screen.lastDuration != duration) {
      screen.lastDuration = duration;
      screen.durationText = Utils.formatTime(duration);
    }
    return screen.durationText;
  }

  // Current-time label cached the same way duration is: reformatted only when
  // the second changes, not on every paint. Otherwise the 5-way String concat
  // in Utils.formatTime allocates a StringBuffer + new String each frame.
  private String getCurrentTimeText(long current) {
    if (screen.lastCurrent != current) {
      screen.lastCurrent = current;
      screen.currentText = Utils.formatTime(current);
    }
    return screen.currentText;
  }

  void resetDurationText() {
    screen.lastDuration = -1;
    screen.durationText = "";
    screen.lastCurrent = -1;
    screen.currentText = "";
  }
}
