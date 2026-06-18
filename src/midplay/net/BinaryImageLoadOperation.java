package midplay.net;

import javax.microedition.lcdui.Image;

/**
 * Downloads a small binary image (album art or a server-rendered text image) off the UI thread and
 * decodes it into an {@link Image}. The two-argument constructor targets the text-image path (no
 * resizing); the three-argument constructor targets album art.
 */
public class BinaryImageLoadOperation extends NetworkOperation {

  private final Listener listener;
  private final String imageUrl;
  private final int targetSize; // <=0 means no resizing (text-image path)

  public BinaryImageLoadOperation(String imageUrl, int targetSize, Listener listener) {
    this.imageUrl = imageUrl;
    this.targetSize = targetSize;
    this.listener = listener;
  }

  public BinaryImageLoadOperation(String imageUrl, Listener listener) {
    this(imageUrl, 0, listener);
  }

  protected void execute() {
    if (imageUrl == null || imageUrl.length() == 0) {
      listener.onImageLoadError(new Exception("Invalid image URL"));
      return;
    }
    String url = targetSize > 0 ? URLProvider.getSizedImage(imageUrl, targetSize) : imageUrl;
    byte[] data = fetchBytes(url);
    if (data != null) {
      onBinaryResponse(data);
    }
  }

  protected void handleNoData() {
    listener.onImageLoadError(new Exception("No image data received"));
  }

  protected void handleError(Exception e) {
    listener.onImageLoadError(e);
  }

  public void onBinaryResponse(byte[] data) {
    if (stopped) {
      return;
    }
    try {
      Image image = Image.createImage(data, 0, data.length);
      listener.onImageLoaded(image);
    } catch (Exception e) {
      listener.onImageLoadError(e);
    }
  }

  public interface Listener {
    void onImageLoaded(Image image);

    void onImageLoadError(Exception e);
  }
}
