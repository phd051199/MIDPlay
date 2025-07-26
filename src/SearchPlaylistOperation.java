import model.Playlists;

public class SearchPlaylistOperation extends NetworkOperation {
  private final Listener listener;
  private final String keyword;
  private final String type;
  private final int currentPage;

  public SearchPlaylistOperation(String keyword, String type, int currentPage, Listener listener) {
    this.keyword = keyword;
    this.type = type;
    this.currentPage = currentPage;
    this.listener = listener;
  }

  public SearchPlaylistOperation(String keyword, String type, Listener listener) {
    this(keyword, type, 1, listener);
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(
        URLProvider.searchPlaylists(this.keyword, this.type, this.currentPage));
  }

  protected void processResponse(String response) {
    final Playlists playlistList = new Playlists().fromJSON(response);
    if (playlistList == null || playlistList.getPlaylists().length == 0) {
      this.listener.onNoDataReceived();
    } else {
      this.listener.onDataReceived(playlistList);
    }
  }

  protected void handleNoData() {
    this.listener.onNoDataReceived();
  }

  protected void handleError(Exception e) {
    this.listener.onError(e);
  }

  public interface Listener {
    void onDataReceived(Playlists p);

    void onNoDataReceived();

    void onError(Exception e);
  }
}
