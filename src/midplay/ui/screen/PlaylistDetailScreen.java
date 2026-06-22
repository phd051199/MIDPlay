package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import midplay.MIDPlay;
import midplay.model.JsonListResult;
import midplay.model.Playlist;
import midplay.model.Tracks;
import midplay.net.JsonOperation;
import midplay.store.FavoritesManager;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.FavoritesActions;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class PlaylistDetailScreen extends BaseForm {
  private final Playlist playlist;
  private Tracks tracks;

  private StringItem nameItem;
  private StringItem typeItem;
  private StringItem trackCountItem;

  public PlaylistDetailScreen(Playlist playlist, Navigator navigator) {
    super(Lang.tr("details.title"), navigator);
    this.playlist = playlist;

    addCommand(Commands.playlistViewTracks());
    addCommand(Commands.playlistAdd());

    initializeItems();
    loadPlaylistTracks();
  }

  private void initializeItems() {
    nameItem = new StringItem(Lang.tr("details.name") + ": ", playlist.getName());
    typeItem =
        new StringItem(
            Lang.tr("details.type") + ": ",
            playlist.isCustom() ? Lang.tr("details.custom") : Lang.tr("details.system"));
    trackCountItem =
        new StringItem(Lang.tr("details.track_count") + ": ", Lang.tr("status.loading"));

    append(nameItem);
    append(typeItem);
    append(trackCountItem);
  }

  private void loadPlaylistTracks() {
    if (playlist.isCustom()) {
      tracks = FavoritesManager.getInstance().getCustomPlaylistTracks(playlist);
      updateTrackCount();
      return;
    }

    MIDPlay.startOperation(
        JsonOperation.getTracks(
            playlist.getKey(),
            new JsonOperation.JsonListListener() {
              public void onDataReceived(JsonListResult result) {
                tracks = (Tracks) result;
                updateTrackCount();
              }

              public void onNoData() {
                trackCountItem.setText("0");
              }

              public void onError(Exception e) {
                trackCountItem.setText(Lang.tr("status.error"));
              }
            }));
  }

  private void updateTrackCount() {
    if (tracks != null && tracks.getTracks() != null) {
      trackCountItem.setText(String.valueOf(tracks.getTracks().length));
    } else {
      trackCountItem.setText("0");
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playlistViewTracks()) {
      viewTracks();
    } else if (c == Commands.playlistAdd()) {
      addPlaylistToFavorites();
    }
  }

  private void viewTracks() {
    if (tracks != null) {
      TrackListScreen trackListScreen =
          new TrackListScreen(playlist.getName(), tracks, navigator, playlist);
      navigator.forward(trackListScreen);
    } else {
      navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
    }
  }

  private void addPlaylistToFavorites() {
    FavoritesActions.showAddPlaylistResult(
        navigator, FavoritesManager.getInstance().addPlaylist(playlist));
  }
}
