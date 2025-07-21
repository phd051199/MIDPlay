import model.Tracks;

public class TracksOperation extends NetworkOperation {

  public static TracksOperation searchTracks(String keyword, int page, TracksListener listener) {
    return new TracksOperation(URLProvider.searchTracks(keyword, page), listener);
  }

  public static TracksOperation searchTracks(String keyword, TracksListener listener) {
    return searchTracks(keyword, 1, listener);
  }

  public static TracksOperation getTracks(String listKey, TracksListener listener) {
    return new TracksOperation(URLProvider.getTracks(listKey), listener);
  }

  private final String url;
  private final TracksListener listener;

  public TracksOperation(String url, TracksListener listener) {
    this.url = url;
    this.listener = listener;
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(this.url);
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

  public interface TracksListener {
    void onDataReceived(Tracks tracks);

    void onNoDataReceived();

    void onError(Exception e);
  }
}
