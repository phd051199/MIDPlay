package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Playlist;
import midplay.model.Playlists;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.store.RecentManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.FormHelpers;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;

public final class FavoritesScreen extends BaseList {
  private final FavoritesManager favoritesManager;
  private Playlists favorites;

  public FavoritesScreen(Navigator navigator) {
    super(Lang.tr("menu.favorites"), navigator);
    this.favoritesManager = FavoritesManager.getInstance();
    addCommand(Commands.playlistCreate());
    addCommand(Commands.addAllToQueue());
    addCommand(Commands.playlistRemove());
    addCommand(Commands.playlistRename());
    populateItems();
  }

  protected void populateItems() {
    favorites = favoritesManager.getPlaylists();
    Playlist[] playlists = favorites.getPlaylists();
    if (playlists != null && playlists.length > 0) {
      for (int i = 0; i < playlists.length; i++) {
        this.append(playlists[i].getDisplayTitle(), Configuration.folderIcon);
      }
    }
  }

  protected void handleSelection() {
    final Playlist selected = getSelectedPlaylist();
    if (selected == null) {
      return;
    }
    RecentManager.getInstance().recordFolder(selected);
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    if (selected.isCustom()) {
      loadCustomPlaylistTracks(selected);
    } else {
      PlayerNavHelper.loadRemotePlaylistTracks(navigator, selected.getKey(), selected.getName());
    }
  }

  private void loadCustomPlaylistTracks(final Playlist playlist) {
    Tracks tracks = favoritesManager.getCustomPlaylistTracks(playlist);
    if (Tracks.isEmpty(tracks)) {
      navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
    } else {
      TrackListScreen trackListScreen =
          new TrackListScreen(playlist.getName(), tracks, navigator, playlist);
      navigator.forward(trackListScreen);
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playlistRemove()) {
      removeFromFavorites();
    } else if (c == Commands.playlistCreate()) {
      showCreatePlaylistForm();
    } else if (c == Commands.playlistRename()) {
      showRenamePlaylistForm();
    } else if (c == Commands.addAllToQueue()) {
      addAllToQueue();
    }
  }

  private void addAllToQueue() {
    final Playlist selected = getSelectedPlaylist();
    if (selected == null) {
      return;
    }
    if (selected.isCustom()) {
      Tracks tracks = favoritesManager.getCustomPlaylistTracks(selected);
      if (Tracks.isEmpty(tracks)) {
        navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
        return;
      }
      PlayerNavHelper.addToQueue(tracks.getTracks(), selected.getName(), navigator);
    } else {
      navigator.showLoadingAlert(Lang.tr("status.loading"));
      PlayerNavHelper.loadRemotePlaylistToQueue(navigator, selected.getKey(), selected.getName());
    }
  }

  private Playlist getSelectedPlaylist() {
    Playlist[] playlists = favorites.getPlaylists();
    int selectedIndex = getSelectedIndex();
    if (playlists == null || playlists.length == 0) {
      return null;
    }
    if (selectedIndex < 0 || selectedIndex >= playlists.length) {
      return null;
    }
    return playlists[selectedIndex];
  }

  private void removeFromFavorites() {
    final Playlist selected = getSelectedPlaylist();
    if (selected == null) {
      return;
    }

    if (selected.isCustom() && favoritesManager.getCustomPlaylistTracksCount(selected) > 0) {
      navigator.showConfirmationAlert(
          Lang.tr("playlist.confirm.delete_with_tracks"),
          new Runnable() {
            public void run() {
              performRemovePlaylist(selected);
            }
          },
          AlertType.WARNING);
    } else {
      performRemovePlaylist(selected);
    }
  }

  private void performRemovePlaylist(Playlist playlist) {
    if (favoritesManager.removePlaylistAndTracks(playlist)) {
      navigator.showAlert(Lang.tr("favorites.status.removed"), AlertType.CONFIRMATION);
      refresh();
    } else {
      navigator.showAlert(Lang.tr("favorites.error.remove_failed"), AlertType.ERROR);
    }
  }

  private void showCreatePlaylistForm() {
    showPlaylistForm(null, Lang.tr("playlist.create"));
  }

  private void showRenamePlaylistForm() {
    Playlist selected = getSelectedPlaylist();
    if (selected == null) {
      return;
    }
    if (selected.isCustom()) {
      showPlaylistForm(selected, Lang.tr("playlist.rename"));
    } else {
      navigator.showAlert(Lang.tr("playlist.error.cannot_rename_system"), AlertType.INFO);
    }
  }

  private void showPlaylistForm(final Playlist existingPlaylist, String formTitle) {
    String initialName = existingPlaylist != null ? existingPlaylist.getName() : "";
    FormHelpers.promptName(
        navigator,
        formTitle,
        initialName,
        new FormHelpers.NameSubmitHandler() {
          public void onSubmit(String name) {
            if (existingPlaylist != null) {
              renamePlaylist(existingPlaylist, name);
            } else {
              createNewPlaylist(name);
            }
          }
        });
  }

  private void createNewPlaylist(String name) {
    String key = "custom_" + System.currentTimeMillis();
    Playlist customPlaylist = new Playlist(key, name, "");
    int result = favoritesManager.addPlaylist(customPlaylist);
    if (result == FavoritesManager.SUCCESS) {
      navigator.back();
      refresh();
      navigator.showAlert(Lang.tr("playlist.status.created"), AlertType.CONFIRMATION, this);
    } else {
      navigator.showAlert(Lang.tr("playlist.error.create_failed"), AlertType.ERROR);
    }
  }

  private void renamePlaylist(Playlist oldPlaylist, String newName) {
    Playlist renamedPlaylist =
        new Playlist(
            oldPlaylist.getKey(), newName, oldPlaylist.getImageUrl(), oldPlaylist.getArtist());
    renamedPlaylist.setId(oldPlaylist.getId());
    if (favoritesManager.updatePlaylist(renamedPlaylist)) {
      navigator.back();
      refresh();
      navigator.showAlert(Lang.tr("playlist.status.renamed"), AlertType.CONFIRMATION, this);
    } else {
      navigator.showAlert(Lang.tr("playlist.error.rename_failed"), AlertType.ERROR);
    }
  }
}
