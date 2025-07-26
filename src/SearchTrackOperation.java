import model.Tracks;

public class SearchTrackOperation extends NetworkOperation {
  private final Listener listener;
  private final String keyword;
  private final int currentPage;

  public SearchTrackOperation(String keyword, int currentPage, Listener listener) {
    this.keyword = keyword;
    this.currentPage = currentPage;
    this.listener = listener;
  }

  public SearchTrackOperation(String keyword, Listener listener) {
    this(keyword, 1, listener);
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(URLProvider.searchTracks(this.keyword, this.currentPage));
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
