package midplay.ui.screen;

import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Playlist;
import midplay.model.Playlists;
import midplay.model.Track;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.ui.BaseList;
import midplay.ui.Navigator;
import midplay.util.Lang;

// Modal "pick a custom playlist to add this track to" screen. Split out of the
// former dual-mode FavoritesScreen: because it only ever lists custom
// playlists, the selected row maps 1:1 to the custom-playlist list, so no
// index remapping is needed.
public final class PlaylistPickerScreen extends BaseList {
  private final FavoritesManager favoritesManager;
  private final Track trackToAdd;
  private final Displayable previousScreen;
  private Playlist[] customPlaylists = new Playlist[0];

  public PlaylistPickerScreen(Navigator navigator, Track trackToAdd, Displayable previousScreen) {
    super(Lang.tr("playlist.select"), navigator);
    this.favoritesManager = FavoritesManager.getInstance();
    this.trackToAdd = trackToAdd;
    this.previousScreen = previousScreen;
    populateItems();
  }

  protected void populateItems() {
    Playlists favorites = favoritesManager.getPlaylists();
    Playlist[] all = favorites.getPlaylists();
    Vector custom = new Vector();
    if (all != null) {
      for (int i = 0; i < all.length; i++) {
        if (all[i].isCustom()) {
          custom.addElement(all[i]);
        }
      }
    }
    customPlaylists = new Playlist[custom.size()];
    custom.copyInto(customPlaylists);
    for (int i = 0; i < customPlaylists.length; i++) {
      this.append(customPlaylists[i].getDisplayTitle(), Configuration.folderIcon);
    }
    if (customPlaylists.length == 0) {
      this.append(Lang.tr("playlist.status.no_custom"), null);
    }
  }

  protected void handleSelection() {
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= customPlaylists.length) {
      return;
    }
    Playlist selectedPlaylist = customPlaylists[selectedIndex];
    int result = favoritesManager.addTrackToPlaylist(selectedPlaylist, trackToAdd);
    if (result == FavoritesManager.SUCCESS) {
      navigator.back();
      navigator.showAlert(
          Lang.tr("playlist.status.track_added"), AlertType.CONFIRMATION, previousScreen);
    } else if (result == FavoritesManager.ALREADY_EXISTS) {
      navigator.showAlert(Lang.tr("playlist.error.track_already_exists"), AlertType.INFO);
    } else {
      navigator.showAlert(Lang.tr("playlist.error.add_track_failed"), AlertType.ERROR);
    }
  }

  // No screen-specific commands; only the inherited back / now-playing actions,
  // which BaseList handles itself.
  protected void handleCommand(Command c, Displayable d) {}
}
