package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.MIDPlay;
import midplay.model.JsonListResult;
import midplay.model.Playlist;
import midplay.model.Playlists;
import midplay.net.JsonOperation;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.store.RecentManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.FavoritesActions;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;
import midplay.util.Utils;

public final class PlaylistListScreen extends BaseList {
  private Playlists items;
  private final FavoritesManager favoritesManager;
  private int currentPage = 1;
  private final String keyword;
  private final String searchType;
  private int loadMoreIndex = -1;

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
    addCommand(Commands.addAllToQueue());
    addCommand(Commands.details());
    populateItems();
  }

  protected void populateItems() {
    loadMoreIndex = -1;
    for (int i = 0; i < items.getPlaylists().length; i++) {
      this.append(items.getPlaylists()[i].getDisplayTitle(), Configuration.folderIcon);
    }
    addLoadMoreIfNeeded();
  }

  protected void handleSelection() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)) {
      loadMore();
      return;
    }
    if (!isValidSelection(selectedIndex, items.getPlaylists().length)) {
      return;
    }
    final Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    RecentManager.getInstance().recordFolder(selectedPlaylist);
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    PlayerNavHelper.loadRemotePlaylistTracks(
        navigator, selectedPlaylist.getKey(), selectedPlaylist.getName());
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playlistAdd()) {
      addToFavorites();
    } else if (c == Commands.addAllToQueue()) {
      addAllToQueue();
    } else if (c == Commands.details()) {
      showPlaylistDetails();
    }
  }

  private void addAllToQueue() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)
        || !isValidSelection(selectedIndex, items.getPlaylists().length)) {
      return;
    }
    final Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    PlayerNavHelper.loadRemotePlaylistToQueue(
        navigator, selectedPlaylist.getKey(), selectedPlaylist.getName());
  }

  private void loadMore() {
    final int nextPage = currentPage + 1;
    final boolean shouldRestoreLoadMore = items.hasMore();
    removeLoadMoreItem();
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    MIDPlay.startOperation(
        createLoadMoreOperation(
            nextPage,
            shouldRestoreLoadMore,
            new JsonOperation.JsonListListener() {
              public void onDataReceived(JsonListResult result) {
                Playlists newItems = (Playlists) result;
                currentPage = nextPage;
                onLoadMoreSuccess(newItems);
                if (!Utils.isJ2MELoader()) {
                  navigator.dismissAlert();
                }
              }

              public void onNoData() {
                restoreLoadMoreItem(shouldRestoreLoadMore);
                navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
              }

              public void onError(Exception e) {
                restoreLoadMoreItem(shouldRestoreLoadMore);
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            }));
  }

  private JsonOperation createLoadMoreOperation(
      int nextPage, boolean shouldRestoreLoadMore, JsonOperation.JsonListListener listener) {
    if (keyword != null && searchType != null) {
      return JsonOperation.searchPlaylists(keyword, searchType, nextPage, listener);
    }
    return JsonOperation.getHotPlaylists(nextPage, listener);
  }

  private void onLoadMoreSuccess(Playlists newItems) {
    items.add(newItems);
    addNewItems(newItems.getPlaylists());
    addLoadMoreIfNeeded();
  }

  private void restoreLoadMoreItem(boolean shouldRestore) {
    if (shouldRestore && loadMoreIndex < 0) {
      appendLoadMoreItem();
    }
  }

  private void appendLoadMoreItem() {
    this.append(Lang.tr("status.load_more"), Configuration.folderIcon);
    loadMoreIndex = this.size() - 1;
  }

  private void addToFavorites() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)
        || !isValidSelection(selectedIndex, items.getPlaylists().length)) {
      return;
    }
    Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    FavoritesActions.showAddPlaylistResult(
        navigator, favoritesManager.addPlaylist(selectedPlaylist));
  }

  private void showPlaylistDetails() {
    int selectedIndex = getSelectedIndex();
    if (isLoadMoreItem(selectedIndex)
        || !isValidSelection(selectedIndex, items.getPlaylists().length)) {
      return;
    }
    Playlist selectedPlaylist = items.getPlaylists()[selectedIndex];
    PlaylistDetailScreen detailScreen = new PlaylistDetailScreen(selectedPlaylist, navigator);
    navigator.forward(detailScreen);
  }

  private boolean isLoadMoreItem(int selectedIndex) {
    return loadMoreIndex >= 0 && selectedIndex == loadMoreIndex;
  }

  private void removeLoadMoreItem() {
    if (loadMoreIndex >= 0) {
      this.delete(loadMoreIndex);
      loadMoreIndex = -1;
    }
  }

  private void addNewItems(Playlist[] newPlaylists) {
    for (int i = 0; i < newPlaylists.length; i++) {
      this.append(newPlaylists[i].getDisplayTitle(), Configuration.folderIcon);
    }
  }

  private void addLoadMoreIfNeeded() {
    if (items.hasMore()) {
      appendLoadMoreItem();
    }
  }
}
