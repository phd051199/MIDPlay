package midplay.ui;

import midplay.model.JsonListResult;
import midplay.model.Playlists;
import midplay.ui.screen.PlaylistListScreen;

public class PlaylistsListForwarder extends AbstractListForwarder {
  private final String title;
  private final String keyword;
  private final String searchType;

  public PlaylistsListForwarder(
      Navigator navigator, String title, String noDataKey, String keyword, String searchType) {
    super(navigator, noDataKey);
    this.title = title;
    this.keyword = keyword;
    this.searchType = searchType;
  }

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
