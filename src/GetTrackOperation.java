import model.Tracks;

public class GetTrackOperation extends NetworkOperation {
  private final Listener listener;
  private final String listKey;

  public GetTrackOperation(String listKey, Listener listener) {
    this.listKey = listKey;
    this.listener = listener;
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(URLProvider.getTracks(this.listKey));
  }

  protected void processResponse(String response) {
    final Tracks trackList = new Tracks().fromJSON(response);
    if (trackList == null || trackList.getTracks().length == 0) {
      this.listener.onNoDataReceived();
    } else {
      this.listener.onDataReceived(trackList);
    }
  }

  protected void handleNoData() {
    this.listener.onNoDataReceived();
  }

  protected void handleError(Exception e) {
    this.listener.onError(e);
  }

  public interface Listener {
    void onDataReceived(Tracks t);

    void onNoDataReceived();

    void onError(Exception e);
  }
}
