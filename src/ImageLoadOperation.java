import javax.microedition.lcdui.Image;

public class ImageLoadOperation extends NetworkOperation implements Network.BinaryNetworkListener {
  private final Listener listener;
  private final String imageUrl;
  private final int targetSize;

  public ImageLoadOperation(String imageUrl, int targetSize, Listener listener) {
    this.imageUrl = imageUrl;
    this.targetSize = targetSize;
    this.listener = listener;
  }

  protected void execute() {
    if (this.imageUrl == null) {
      this.listener.onImageLoadError(new Exception("Invalid image URL"));
      return;
    }
    String sizedImageUrl = URLProvider.getSizedImage(this.imageUrl, this.targetSize);
    this.network = new Network();
    this.network.startHttpGetBytes(sizedImageUrl, this);
  }

  protected void processResponse(String response) {}

  protected void handleNoData() {
    this.listener.onImageLoadError(new Exception("No image data received"));
  }

  protected void handleError(Exception e) {
    this.listener.onImageLoadError(e);
  }

  public void onBinaryResponse(byte[] data) {
    if (this.stopped) {
      return;
    }
    try {
      Image image = Image.createImage(data, 0, data.length);
      this.listener.onImageLoaded(image);
    } catch (Exception e) {
      this.listener.onImageLoadError(e);
    }
  }

  public interface Listener {
    void onImageLoaded(Image image);

    void onImageLoadError(Exception e);
  }
}
