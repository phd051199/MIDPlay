import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Playlist;
import model.Track;
import model.Tracks;

public final class TrackListScreen extends BaseList {
  private Tracks items;
  private final String title;
  private final Playlist playlist;

  public TrackListScreen(String title, Tracks items, Navigator navigator) {
    this(title, items, navigator, null);
  }

  public TrackListScreen(String title, Tracks items, Navigator navigator, Playlist playlist) {
    super(title, navigator);
    this.items = items;
    this.title = title;
    this.playlist = playlist;
    addCommand(Commands.playerAddToPlaylist());
    if (playlist != null && playlist.isCustom()) {
      addCommand(Commands.playlistRemove());
    }
    populateItems();
  }

  protected void populateItems() {
    for (int i = 0; i < items.getTracks().length; i++) {
      this.append(items.getTracks()[i].getName(), Configuration.musicIcon);
    }
  }

  private void refresh() {
    if (playlist == null || !playlist.isCustom()) {
      return;
    }
    int index = getSelectedIndex();
    this.deleteAll();
    FavoritesManager favoritesManager = FavoritesManager.getInstance();
    items = favoritesManager.getCustomPlaylistTracks(playlist);
    if (items != null) {
      populateItems();
    }
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

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (index >= 0 && index < items.getTracks().length) {
      PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
      if (playerScreen != null) {
        playerScreen.change(title, items, index, navigator);
        Displayable previous = navigator.getPrevious();
        if (previous instanceof PlayerScreen) {
          navigator.back();
        } else {
          navigator.forward(playerScreen);
        }
      } else {
        playerScreen = new PlayerScreen(title, items, index, navigator);
        MIDPlay.setPlayerScreen(playerScreen);
        navigator.forward(playerScreen);
      }
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playerAddToPlaylist()) {
      addToPlaylist();
    } else if (c == Commands.playlistRemove()) {
      removeFromPlaylist();
    }
  }

  private void addToPlaylist() {
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= items.getTracks().length) {
      return;
    }
    Track selectedTrack = items.getTracks()[selectedIndex];
    FavoritesScreen selectionScreen = new FavoritesScreen(navigator, selectedTrack, this);
    navigator.forward(selectionScreen);
  }

  private void removeFromPlaylist() {
    if (playlist == null || !playlist.isCustom()) {
      return;
    }
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= items.getTracks().length) {
      return;
    }
    Track selectedTrack = items.getTracks()[selectedIndex];
    FavoritesManager favoritesManager = FavoritesManager.getInstance();
    if (favoritesManager.removeTrackFromPlaylist(playlist, selectedTrack)) {
      navigator.showAlert(Lang.tr("playlist.status.track_removed"), AlertType.CONFIRMATION);
      refresh();
    } else {
      navigator.showAlert(Lang.tr("playlist.error.remove_track_failed"), AlertType.ERROR);
    }
  }
}
