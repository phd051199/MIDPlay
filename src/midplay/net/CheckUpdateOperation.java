package midplay.net;

public class CheckUpdateOperation extends NetworkOperation {
  private final Listener listener;

  public CheckUpdateOperation(Listener listener) {
    this.listener = listener;
  }

  protected void execute() {
    fetchText(URLProvider.checkForUpdate());
  }

  protected void processResponse(String response) {
    if (response == null || response.trim().length() == 0) {
      this.listener.onNoUpdateAvailable();
    } else {
      this.listener.onUpdateAvailable(response.trim());
    }
  }

  protected void handleNoData() {
    this.listener.onNoUpdateAvailable();
  }

  protected void handleError(Exception e) {
    this.listener.onError(e);
  }

  public interface Listener {
    void onUpdateAvailable(String updateUrl);

    void onNoUpdateAvailable();

    void onError(Exception e);
  }
}
