package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.model.JsonListResult;
import midplay.model.Playlists;
import midplay.net.JsonOperation;
import midplay.ui.screen.PlaylistListScreen;
import midplay.util.Lang;

// Standard handler for "fetch playlists on a worker thread, then open them in a
// PlaylistListScreen". Mirrors TracksListForwarder. The two call sites differ in
// whether the result is paginated (search: keyword + searchType enable "load
// more") or not (discover/hot), which is expressed by the constructor used.
public class PlaylistsListForwarder implements JsonOperation.JsonListListener {
  private final Navigator navigator;
  private final String title;
  private final String noDataKey;
  // Null for non-paginated results (discover/hot); non-null enables pagination.
  private final String keyword;
  private final String searchType;

  // Paginated result (search): "load more" is available.
  public PlaylistsListForwarder(
      Navigator navigator, String title, String noDataKey, String keyword, String searchType) {
    this.navigator = navigator;
    this.title = title;
    this.noDataKey = noDataKey;
    this.keyword = keyword;
    this.searchType = searchType;
  }

  // Non-paginated result (discover/hot): no "load more".
  public PlaylistsListForwarder(Navigator navigator, String title, String noDataKey) {
    this(navigator, title, noDataKey, null, null);
  }

  public void onDataReceived(JsonListResult result) {
    Playlists items = (Playlists) result;
    PlaylistListScreen screen;
    if (keyword != null) {
      screen = new PlaylistListScreen(title, items, navigator, keyword, searchType);
    } else {
      screen = new PlaylistListScreen(title, items, navigator);
    }
    navigator.forward(screen);
  }

  public void onNoData() {
    navigator.showAlert(Lang.tr(noDataKey), AlertType.INFO);
  }

  public void onError(Exception e) {
    navigator.showAlert(e.toString(), AlertType.ERROR);
  }
}
