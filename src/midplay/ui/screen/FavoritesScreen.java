package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import midplay.MIDPlay;
import midplay.model.Playlist;
import midplay.model.Playlists;
import midplay.model.Tracks;
import midplay.net.JsonOperation;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.TracksListForwarder;
import midplay.util.Lang;

public final class FavoritesScreen extends BaseList {
  private final FavoritesManager favoritesManager;
  private Playlists favorites;

  public FavoritesScreen(Navigator navigator) {
    super(Lang.tr("menu.favorites"), navigator);
    this.favoritesManager = FavoritesManager.getInstance();
    addCommand(Commands.playlistCreate());
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
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    if (selected.isCustom()) {
      loadCustomPlaylistTracks(selected);
    } else {
      loadRemotePlaylistTracks(selected);
    }
  }

  private void loadCustomPlaylistTracks(final Playlist playlist) {
    Tracks tracks = favoritesManager.getCustomPlaylistTracks(playlist);
    if (tracks == null || tracks.getTracks().length == 0) {
      navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
    } else {
      TrackListScreen trackListScreen =
          new TrackListScreen(playlist.getName(), tracks, navigator, playlist);
      navigator.forward(trackListScreen);
    }
  }

  private void loadRemotePlaylistTracks(final Playlist playlist) {
    MIDPlay.startOperation(
        JsonOperation.getTracks(
            playlist.getKey(),
            new TracksListForwarder(navigator, playlist.getName(), "status.no_data")));
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playlistRemove()) {
      removeFromFavorites();
    } else if (c == Commands.playlistCreate()) {
      showCreatePlaylistForm();
    } else if (c == Commands.playlistRename()) {
      showRenamePlaylistForm();
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
    final Form f = new Form(formTitle);
    final TextField nameField =
        new TextField(
            Lang.tr("playlist.name"),
            existingPlaylist != null ? existingPlaylist.getName() : "",
            50,
            TextField.ANY);
    f.append(nameField);
    f.addCommand(Commands.ok());
    f.addCommand(Commands.cancel());
    f.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              String name = nameField.getString().trim();
              if (name.length() > 0) {
                if (existingPlaylist != null) {
                  renamePlaylist(existingPlaylist, name);
                } else {
                  createNewPlaylist(name);
                }
              } else {
                navigator.showAlert(Lang.tr("playlist.error.empty_name"), AlertType.ERROR);
              }
            } else if (c == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(f);
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
