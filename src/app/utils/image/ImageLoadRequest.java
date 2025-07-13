package app.utils.image;

public class ImageLoadRequest {
  private final String url;
  private final int size;
  private final int index;
  private final ImageLoadCallback callback;
  private final String requestId;
  private volatile boolean cancelled;

  public ImageLoadRequest(String url, int size, int index, ImageLoadCallback callback) {
    this.url = url;
    this.size = size;
    this.index = index;
    this.callback = callback;
    this.requestId = generateRequestId();
    this.cancelled = false;
  }

  public String getUrl() {
    return url;
  }

  public int getSize() {
    return size;
  }

  public int getIndex() {
    return index;
  }

  public ImageLoadCallback getCallback() {
    return callback;
  }

  public String getRequestId() {
    return requestId;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void cancel() {
    this.cancelled = true;
  }

  private String generateRequestId() {
    return "req_" + System.currentTimeMillis() + "_" + index;
  }
}
