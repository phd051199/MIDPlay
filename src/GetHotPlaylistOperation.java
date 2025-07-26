import model.Playlists;

public class GetHotPlaylistOperation extends NetworkOperation {
  private final Listener listener;
  private int currentPage = 1;

  public GetHotPlaylistOperation(int currentPage, Listener listener) {
    this.listener = listener;
    this.currentPage = currentPage;
  }

  public GetHotPlaylistOperation(Listener listener) {
    this(1, listener);
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(URLProvider.getHotPlaylists(this.currentPage));
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
