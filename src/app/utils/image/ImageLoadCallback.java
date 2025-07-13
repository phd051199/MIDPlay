package app.utils.image;

import javax.microedition.lcdui.Image;

public interface ImageLoadCallback {
  void onImageLoaded(int index, Image image, String requestId);

  void onImageLoadFailed(int index, String url, String error, String requestId);

  boolean shouldContinueLoading();
}
