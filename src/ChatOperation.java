import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;

public class ChatOperation extends NetworkOperation {

  public static ChatOperation sendMessage(String message, String sessionId, ChatListener listener) {
    return new ChatOperation(URLProvider.getChatEndpoint(message, sessionId), listener);
  }

  private final String url;
  private final ChatListener listener;

  public ChatOperation(String url, ChatListener listener) {
    this.url = url;
    this.listener = listener;
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(this.url);
  }

  protected void processResponse(String response) {
    if (response != null && response.trim().length() > 0) {
      try {
        JSONObject json = JSON.getObject(response.trim());
        String message = json.getString("message");
        if (message.length() > 0) {
          this.listener.onDataReceived(message);
        } else {
          this.listener.onNoDataReceived();
        }
      } catch (Exception e) {
        this.listener.onError(e);
      }
    } else {
      this.listener.onNoDataReceived();
    }
  }

  protected void handleNoData() {
    this.listener.onNoDataReceived();
  }

  protected void handleError(Exception e) {
    this.listener.onError(e);
  }

  public interface ChatListener {
    void onDataReceived(String response);

    void onNoDataReceived();

    void onError(Exception e);
  }
}
