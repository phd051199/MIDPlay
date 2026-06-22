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

  void freeImageHeap() {
    stopCurrentImageLoad();
    clearImageData();
  }

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

  private Image normalizeSmallScreenArt(Image image) {
    if (screen.isLargeScreen || image == null || screen.albumSize <= 0) {
      return image;
    }
    if (image.getWidth() == screen.albumSize && image.getHeight() == screen.albumSize) {
      return image;
    }
    Image resized = Utils.resizeImageToFit(image, screen.albumSize, screen.albumSize);
    return resized != null ? resized : image;
  }

  void clearImageData() {
    albumArt = null;
    scaledAlbumArt = null;
    lastScaledSize = -1;
    loadingAlbumArt = albumArtLoaded = false;
    artLoadKey++;
  }

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

    public void onImageLoaded(final Image image) {
      screen.navigator.callSerially(
          new Runnable() {
            public void run() {
              if (key != artLoadKey) {
                return;
              }
              albumArt = normalizeSmallScreenArt(image);
              finishImageLoad();
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
                return;
              }
              albumArt = null;
              finishImageLoad();
            }
          });
    }
  }
}
