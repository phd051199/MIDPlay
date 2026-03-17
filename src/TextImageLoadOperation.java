import javax.microedition.lcdui.Image;

public class TextImageLoadOperation extends NetworkOperation implements Network.BinaryNetworkListener {
  private final String imageUrl;
  private final Listener listener;

  public TextImageLoadOperation(String imageUrl, Listener listener) {
    this.imageUrl = imageUrl;
    this.listener = listener;
  }

  protected void execute() {
    if (this.imageUrl == null || this.imageUrl.length() == 0) {
      this.listener.onImageLoadError(new Exception("Invalid image URL"));
      return;
    }
    this.network = new Network();
    this.network.startHttpGetBytes(this.imageUrl, this);
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
