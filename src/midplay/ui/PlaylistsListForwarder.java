package midplay.ui;

import midplay.model.JsonListResult;
import midplay.model.Playlists;
import midplay.ui.screen.PlaylistListScreen;

// Standard handler for "fetch playlists on a worker thread, then open them in a
// PlaylistListScreen". The two call sites differ in whether the result is
// paginated (search: keyword + searchType enable "load more") or not
// (discover/hot), which is expressed by the constructor used.
public class PlaylistsListForwarder extends AbstractListForwarder {
  private final String title;
  // Null for non-paginated results (discover/hot); non-null enables pagination.
  private final String keyword;
  private final String searchType;

  // Paginated result (search): "load more" is available.
  public PlaylistsListForwarder(
      Navigator navigator, String title, String noDataKey, String keyword, String searchType) {
    super(navigator, noDataKey);
    this.title = title;
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
}
