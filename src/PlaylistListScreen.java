import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Playlist;
import model.Playlists;
import model.Tracks;

public final class PlaylistListScreen extends BaseList {
  private Playlists items;
  private final FavoritesManager favoritesManager;
  private int currentPage = 1;
  private final String keyword;
  private final String searchType;

  public PlaylistListScreen(String title, Playlists items, Navigator navigator) {
    this(title, items, navigator, null, null);
  }

  public PlaylistListScreen(
      String title, Playlists items, Navigator navigator, String keyword, String searchType) {
    super(title, navigator);
    this.items = items;
    this.favoritesManager = FavoritesManager.getInstance();
    this.keyword = keyword;
    this.searchType = searchType;
    addCommand(Commands.playlistAdd());
    addCommand(Commands.details());
    populateItems();
  }

  protected void populateItems() {
    for (int i = 0; i < items.getPlaylists().length; i++) {
      this.append(items.getPlaylists()[i].getName(), Configuration.folderIcon);
    }
    if (items.hasMore()) {
      this.append(Lang.tr("status.load_more"), Configuration.folderIcon);
    }
  }

  protected void handleSelection() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)) {
      loadMore();
      return;
    }
    if (selectedIndex < 0 || selectedIndex >= items.getPlaylists().length) {
      return;
    }
    final Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    MIDPlay.startOperation(
        TracksOperation.getTracks(
            selectedPlaylist.getKey(),
            new TracksOperation.TracksListener() {
              public void onDataReceived(Tracks items) {
                TrackListScreen trackListScreen =
                    new TrackListScreen(selectedPlaylist.getName(), items, navigator);
                navigator.forward(trackListScreen);
              }

              public void onNoDataReceived() {
                navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
              }

              public void onError(Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            }));
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playlistAdd()) {
      addToFavorites();
    } else if (c == Commands.details()) {
      showPlaylistDetails();
    }
  }

  private void loadMore() {
    currentPage++;
    removeLoadMoreItem();
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    MIDPlay.startOperation(
        PlaylistsOperation.searchPlaylists(
            keyword,
            searchType,
            currentPage,
            new PlaylistsOperation.PlaylistsListener() {
              public void onDataReceived(Playlists newItems) {
                onLoadMoreSuccess(newItems);
                if (!Utils.isJ2MELoader()) {
                  navigator.dismissAlert();
                }
              }

              public void onNoDataReceived() {
                navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
              }

              public void onError(Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            }));
  }

  private void onLoadMoreSuccess(Playlists newItems) {
    items.add(newItems);
    addNewItems(newItems.getPlaylists());
    addLoadMoreIfNeeded();
  }

  private void addToFavorites() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)
        || selectedIndex < 0
        || selectedIndex >= items.getPlaylists().length) {
      return;
    }
    Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    int result = favoritesManager.addPlaylist(selectedPlaylist);
    if (result == FavoritesManager.SUCCESS) {
      navigator.showAlert(Lang.tr("favorites.status.added"), AlertType.CONFIRMATION);
    } else if (result == FavoritesManager.ALREADY_EXISTS) {
      navigator.showAlert(Lang.tr("favorites.status.already_exists"), AlertType.INFO);
    } else {
      navigator.showAlert(Lang.tr("favorites.error.save_failed"), AlertType.ERROR);
    }
  }

  private void showPlaylistDetails() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)
        || selectedIndex < 0
        || selectedIndex >= items.getPlaylists().length) {
      return;
    }
    Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    DetailScreen detailScreen = new DetailScreen(selectedPlaylist, navigator);
    navigator.forward(detailScreen);
  }

  private boolean isLoadMoreItem(int selectedIndex) {
    return selectedIndex == this.size() - 1
        && items.hasMore()
        && this.getString(selectedIndex).equals(Lang.tr("status.load_more"));
  }

  private void removeLoadMoreItem() {
    if (this.size() > 0) {
      int lastIndex = this.size() - 1;
      if (this.getString(lastIndex).equals(Lang.tr("status.load_more"))) {
        this.delete(lastIndex);
      }
    }
  }

  private void addNewItems(Playlist[] newPlaylists) {
    for (int i = 0; i < newPlaylists.length; i++) {
      this.append(newPlaylists[i].getName(), Configuration.folderIcon);
    }
  }

  private void addLoadMoreIfNeeded() {
    if (items.hasMore()) {
      this.append(Lang.tr("status.load_more"), Configuration.folderIcon);
    }
  }
}
