import model.Playlists;

public class PlaylistsOperation extends NetworkOperation {

  public static PlaylistsOperation searchPlaylists(
      String keyword, String type, int page, PlaylistsListener listener) {
    return new PlaylistsOperation(URLProvider.searchPlaylists(keyword, type, page), listener);
  }

  public static PlaylistsOperation searchPlaylists(
      String keyword, String type, PlaylistsListener listener) {
    return searchPlaylists(keyword, type, 1, listener);
  }

  public static PlaylistsOperation getHotPlaylists(int page, PlaylistsListener listener) {
    return new PlaylistsOperation(URLProvider.getHotPlaylists(page), listener);
  }

  public static PlaylistsOperation getHotPlaylists(PlaylistsListener listener) {
    return getHotPlaylists(1, listener);
  }

  private final String url;
  private final PlaylistsListener listener;

  public PlaylistsOperation(String url, PlaylistsListener listener) {
    this.url = url;
    this.listener = listener;
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(this.url);
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

  public interface PlaylistsListener {
    void onDataReceived(Playlists playlists);

    void onNoDataReceived();

    void onError(Exception e);
  }
}
