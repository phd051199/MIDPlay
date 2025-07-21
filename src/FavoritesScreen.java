import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import model.Playlist;
import model.Playlists;
import model.Track;
import model.Tracks;

public final class FavoritesScreen extends BaseList {
  private final FavoritesManager favoritesManager;
  private Playlists favorites;
  private final Track trackToAdd;
  private final Displayable previousScreen;
  private final boolean isSelectionMode;

  public FavoritesScreen(Navigator navigator) {
    this(navigator, null, null);
  }

  public FavoritesScreen(Navigator navigator, Track trackToAdd, Displayable previousScreen) {
    super(trackToAdd != null ? Lang.tr("playlist.select") : Lang.tr("menu.favorites"), navigator);
    this.favoritesManager = FavoritesManager.getInstance();
    this.trackToAdd = trackToAdd;
    this.previousScreen = previousScreen;
    this.isSelectionMode = (trackToAdd != null);
    if (!isSelectionMode) {
      addCommand(Commands.playlistCreate());
      addCommand(Commands.playlistRemove());
      addCommand(Commands.playlistRename());
    }
    populateItems();
  }

  protected void populateItems() {
    favorites = favoritesManager.getPlaylists();
    Playlist[] playlists = favorites.getPlaylists();
    if (playlists != null && playlists.length > 0) {
      for (int i = 0; i < playlists.length; i++) {
        if (!isSelectionMode || playlists[i].isCustom()) {
          this.append(playlists[i].getName(), Configuration.folderIcon);
        }
      }
    }
    if (isSelectionMode && this.size() == 0) {
      this.append(Lang.tr("playlist.status.no_custom"), null);
    }
  }

  protected void handleSelection() {
    if (isSelectionMode) {
      handleSelectionModeSelection();
    } else {
      handleNormalModeSelection();
    }
  }

  private void handleSelectionModeSelection() {
    if (this.size() == 0
        || this.getString(getSelectedIndex()).equals(Lang.tr("playlist.status.no_custom"))) {
      return;
    }
    Playlist selectedPlaylist = getSelectedCustomPlaylist();
    if (selectedPlaylist == null) {
      return;
    }
    int result = favoritesManager.addTrackToCustomPlaylist(selectedPlaylist, trackToAdd);
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

  private void handleNormalModeSelection() {
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
        TracksOperation.getTracks(
            playlist.getKey(),
            new TracksOperation.TracksListener() {
              public void onDataReceived(Tracks items) {
                TrackListScreen trackListScreen =
                    new TrackListScreen(playlist.getName(), items, navigator);
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
    if (!isSelectionMode) {
      if (c == Commands.playlistRemove()) {
        removeFromFavorites();
      } else if (c == Commands.playlistCreate()) {
        showCreatePlaylistForm();
      } else if (c == Commands.playlistRename()) {
        showRenamePlaylistForm();
      }
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

  private Playlist getSelectedCustomPlaylist() {
    Playlist[] playlists = favorites.getPlaylists();
    int selectedIndex = getSelectedIndex();
    int customPlaylistIndex = 0;
    if (playlists == null || playlists.length == 0) {
      return null;
    }
    for (int i = 0; i < playlists.length; i++) {
      if (playlists[i].isCustom()) {
        if (customPlaylistIndex == selectedIndex) {
          return playlists[i];
        }
        customPlaylistIndex++;
      }
    }
    return null;
  }

  private void refresh() {
    int index = getSelectedIndex();
    this.deleteAll();
    populateItems();
    int newSize = this.size();
    if (newSize > 0) {
      if (index < 0) {
        index = 0;
      }
      if (index >= newSize) {
        index = newSize - 1;
      }
      this.setSelectedIndex(index, true);
    }
  }

  private void removeFromFavorites() {
    final Playlist selected = getSelectedPlaylist();
    if (selected == null) {
      return;
    }

    if (selected.isCustom() && favoritesManager.getCustomPlaylistTracksCount(selected) > 0) {
      navigator.showConfirmationAlert(
          Lang.tr("playlist.confirm.delete_with_tracks"),
          new CommandListener() {
            public void commandAction(Command c, Displayable d) {
              if (c == Commands.ok()) {
                performRemovePlaylist(selected);
              } else if (c == Commands.cancel()) {
                navigator.dismissAlert();
              }
            }
          },
          AlertType.WARNING);
    } else {
      performRemovePlaylist(selected);
    }
  }

  private void performRemovePlaylist(Playlist playlist) {
    if (favoritesManager.removePlaylistWithTracks(playlist)) {
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
        new Playlist(oldPlaylist.getKey(), newName, oldPlaylist.getImageUrl());
    renamedPlaylist.setId(oldPlaylist.getId());
    if (favoritesManager.removePlaylist(oldPlaylist)
        && favoritesManager.addPlaylist(renamedPlaylist) == FavoritesManager.SUCCESS) {
      navigator.back();
      refresh();
      navigator.showAlert(Lang.tr("playlist.status.renamed"), AlertType.CONFIRMATION, this);
    } else {
      navigator.showAlert(Lang.tr("playlist.error.rename_failed"), AlertType.ERROR);
    }
  }
}
