package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Playlist;
import midplay.model.RecentItem;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.store.RecentManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;
import midplay.util.Utils;

public final class RecentListScreen extends BaseList {
  private RecentItem[] recentItems;

  public RecentListScreen(Navigator navigator) {
    super(Lang.tr("menu.recent"), navigator);
    addCommand(Commands.recentClear());
    populateItems();
  }

  protected void populateItems() {
    recentItems = RecentManager.getInstance().getItems();
    for (int i = 0; i < recentItems.length; i++) {
      RecentItem item = recentItems[i];
      append(
          item.getName(),
          item.getType() == RecentItem.TRACK ? Configuration.musicIcon : Configuration.folderIcon);
    }
    String[] artUrls = new String[recentItems.length];
    for (int i = 0; i < recentItems.length; i++) {
      RecentItem item = recentItems[i];
      if (item.getType() == RecentItem.TRACK) {
        Track track = item.getTrack();
        artUrls[i] = Utils.withArtType(track != null ? track.getImageUrl() : null, 1);
      } else {
        Playlist folder = item.getFolder();
        artUrls[i] = Utils.withArtType(folder != null ? folder.getImageUrl() : null, 0);
      }
    }
    loadArt(artUrls);
  }

  protected int badgeAt(int row) {
    if (row < 0 || row >= recentItems.length) {
      return BADGE_NONE;
    }
    return recentItems[row].getType() == RecentItem.TRACK ? BADGE_MUSIC : BADGE_FOLDER;
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, recentItems.length)) {
      return;
    }
    RecentItem item = recentItems[index];
    if (item.getType() == RecentItem.TRACK) {
      playTrack(item);
    } else {
      openFolder(item);
    }
  }

  private void playTrack(RecentItem item) {
    Track track = item.getTrack();
    if (track == null) {
      return;
    }
    PlayerNavHelper.playSingleTrack(track.getName(), track, navigator);
  }

  private void openFolder(RecentItem item) {
    final Playlist folder = item.getFolder();
    if (folder == null) {
      return;
    }
    RecentManager.getInstance().recordFolder(folder);
    if (folder.isCustom()) {
      Tracks tracks = FavoritesManager.getInstance().getCustomPlaylistTracks(folder);
      if (Tracks.isEmpty(tracks)) {
        navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
        return;
      }
      navigator.forward(new TrackListScreen(folder.getName(), tracks, navigator, folder));
    } else {
      navigator.showLoadingAlert(Lang.tr("status.loading"));
      PlayerNavHelper.loadRemotePlaylistTracks(navigator, folder.getKey(), folder.getName());
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.recentClear()) {
      clearHistory();
    }
  }

  private void clearHistory() {
    if (recentItems.length == 0) {
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("recent.confirm.clear"),
        new Runnable() {
          public void run() {
            RecentManager.getInstance().clear();
            navigator.showAlert(Lang.tr("recent.status.cleared"), AlertType.CONFIRMATION);
            refresh();
          }
        },
        AlertType.WARNING);
  }
}
