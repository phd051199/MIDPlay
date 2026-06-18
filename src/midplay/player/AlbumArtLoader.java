package midplay.player;

import javax.microedition.lcdui.Image;
import midplay.net.BinaryImageLoadOperation;
import midplay.util.Utils;

public final class AlbumArtLoader {

  private final PlayerScreen screen;

  Image albumArt;
  Image scaledAlbumArt;
  int lastScaledSize = -1;
  String albumArtUrl;
  boolean loadingAlbumArt, albumArtLoaded;
  BinaryImageLoadOperation currentImageLoadOperation;
  // Monotonic token bumped whenever an album-art load is superseded or
  // cancelled; in-flight callbacks compare their captured token to detect
  // staleness (mirrors TextImageSlot.requestKey for the text-image path).
  int artLoadKey = 0;

  AlbumArtLoader(PlayerScreen screen) {
    this.screen = screen;
  }

  void setAlbumArtUrl(String url) {
    if (url != null && !url.equals(albumArtUrl)) {
      stopCurrentImageLoad();
      resetImageState(url);
      if (screen.displayWidth > 0 && !screen.isLargeScreen) {
        loadAlbumArt();
      }
    }
  }

  void loadAlbumArt() {
    if (!canLoadAlbumArt()) {
      return;
    }

    stopCurrentImageLoad();
    loadingAlbumArt = true;
    final String imageUrl = albumArtUrl;
    int targetSize =
        calculateAlbumArtSize(
            screen.displayWidth, screen.displayHeight, screen.isLandscape, screen.isLargeScreen);
    final int key = ++artLoadKey;

    currentImageLoadOperation =
        new BinaryImageLoadOperation(imageUrl, targetSize, new ImageLoadCallback(key));
    currentImageLoadOperation.start();
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

  boolean canLoadAlbumArt() {
    return albumArtUrl != null && !loadingAlbumArt && !albumArtLoaded;
  }

  void resetImage() {
    freeImageHeap();
    albumArtUrl = null;
  }

  // Drop decoded album-art bitmaps so the heap is free before a memory-heavy
  // operation (creating a new media Player on low-heap devices). Keeps the URL
  // and load state, so images are re-decoded lazily on the next paint.
  void freeImageHeap() {
    stopCurrentImageLoad();
    clearImageData();
  }

  // Scale the full-size album art down to the large-screen art box. Resize
  // allocates several hundred KB of int[] + a second RGB image transiently;
  // doing that inside paint() the first time art appears causes a mid-frame GC
  // pause (or OOM on the weakest devices). Preparing it here — called from the
  // image-load callback (a separate serial event, between paints) — keeps the
  // allocation off the synchronous paint path. No-op once prepared for the
  // current size, so it is cheap to call from paint as a size-change fallback.
  void prepareScaledAlbumArt() {
    if (albumArt == null || !screen.isLargeScreen || screen.displayWidth <= 0) {
      return;
    }
    int innerSize = screen.albumSize - 1;
    if (innerSize <= 0) {
      return;
    }
    if (scaledAlbumArt == null || lastScaledSize != innerSize) {
      scaledAlbumArt = Utils.resizeImageToFit(albumArt, innerSize, innerSize);
      lastScaledSize = innerSize;
    }
  }

  void clearImageData() {
    albumArt = null;
    scaledAlbumArt = null;
    lastScaledSize = -1;
    loadingAlbumArt = albumArtLoaded = false;
    artLoadKey++;
  }

  // Cancel in-flight album-art + text-image network loads without clearing
  // already-decoded images. Used by hideNotify so a hidden canvas stops
  // spending CPU/network; loads re-trigger on the next paint after showNotify.
  void cancelImageLoads() {
    stopCurrentImageLoad();
    loadingAlbumArt = false;
    screen.trackTextRenderer.trackNameTextImage.stop();
    screen.trackTextRenderer.artistTextImage.stop();
  }

  void stopCurrentImageLoad() {
    if (currentImageLoadOperation != null) {
      currentImageLoadOperation.stop();
      currentImageLoadOperation = null;
    }
  }

  private void resetImageState(String url) {
    albumArtUrl = url;
    clearImageData();
  }

  private class ImageLoadCallback implements BinaryImageLoadOperation.Listener {
    private final int key;

    ImageLoadCallback(int key) {
      this.key = key;
    }

    private void finishImageLoad() {
      scaledAlbumArt = null;
      lastScaledSize = -1;
      albumArtLoaded = true;
      loadingAlbumArt = false;
      currentImageLoadOperation = null;
    }

    // These fire on the image-load worker thread; marshal field mutation +
    // repaint onto the event thread (LCDUI is single-threaded).
    public void onImageLoaded(final Image image) {
      screen.navigator.callSerially(
          new Runnable() {
            public void run() {
              if (key != artLoadKey) {
                return; // stale: a newer load started or art was reset
              }
              albumArt = image;
              finishImageLoad();
              // Pre-scale off the synchronous paint path (see prepareScaledAlbumArt).
              prepareScaledAlbumArt();
              if (albumArt != null) {
                screen.updateDisplayAsync();
              }
            }
          });
    }

    public void onImageLoadError(final Exception e) {
      screen.navigator.callSerially(
          new Runnable() {
            public void run() {
              if (key != artLoadKey) {
                return; // stale
              }
              albumArt = null;
              finishImageLoad();
            }
          });
    }
  }
}
