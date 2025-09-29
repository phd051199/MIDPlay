import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import model.Playlist;
import model.Track;
import model.Tracks;

public final class DetailScreen extends BaseForm {
  private final Navigator navigator;
  private final Playlist playlist;
  private final Track track;
  private Tracks tracks;

  private StringItem nameItem;
  private StringItem typeItem;
  private StringItem trackCountItem;
  private StringItem artistItem;
  private StringItem durationItem;
  private TextField urlField;

  public DetailScreen(Playlist playlist, Navigator navigator) {
    super(Lang.tr("details.title"), navigator);
    this.playlist = playlist;
    this.track = null;
    this.navigator = navigator;

    addCommand(Commands.playlistViewTracks());
    addCommand(Commands.playlistAdd());

    initializePlaylistItems();
    loadPlaylistTracks();
  }

  public DetailScreen(Track track, Navigator navigator) {
    super(Lang.tr("details.title"), navigator);
    this.track = track;
    this.playlist = null;
    this.navigator = navigator;

    addCommand(Commands.playerPlay());
    addCommand(Commands.playerAddToPlaylist());

    initializeTrackItems();
  }

  private void initializePlaylistItems() {
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

  private void initializeTrackItems() {
    nameItem = new StringItem(Lang.tr("details.name") + ": ", track.getName());
    artistItem =
        new StringItem(
            Lang.tr("details.artist") + ": ",
            track.getArtist() != null ? track.getArtist() : Lang.tr("details.unknown_artist"));
    durationItem =
        new StringItem(Lang.tr("details.duration") + ": ", formatDuration(track.getDuration()));

    if (track.getUrl() != null && track.getUrl().length() > 0) {
      urlField =
          new TextField(
              Lang.tr("details.url"), track.getUrl(), track.getUrl().length() + 50, TextField.URL);
      urlField.setConstraints(TextField.UNEDITABLE);
    } else {
      urlField =
          new TextField(Lang.tr("details.url"), Lang.tr("details.no_url"), 50, TextField.ANY);
      urlField.setConstraints(TextField.UNEDITABLE);
    }

    append(nameItem);
    append(artistItem);
    append(durationItem);
    append(urlField);
  }

  private void loadPlaylistTracks() {
    if (playlist == null) return;

    MIDPlay.startOperation(
        TracksOperation.getTracks(
            playlist.getKey(),
            new TracksOperation.TracksListener() {
              public void onDataReceived(Tracks items) {
                tracks = items;
                updateTrackCount();
              }

              public void onNoDataReceived() {
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

  private String formatDuration(int seconds) {
    if (seconds <= 0) {
      return Lang.tr("details.unknown_duration");
    }
    int minutes = seconds / 60;
    int remainingSeconds = seconds % 60;
    return minutes + ":" + (remainingSeconds < 10 ? "0" : "") + remainingSeconds;
  }

  protected void handleCommand(Command c, Displayable d) {
    if (playlist != null) {
      if (c == Commands.playlistViewTracks()) {
        viewTracks();
      } else if (c == Commands.playlistAdd()) {
        addPlaylistToFavorites();
      }
    } else if (track != null) {
      if (c == Commands.playerPlay()) {
        playTrack();
      } else if (c == Commands.playerAddToPlaylist()) {
        addTrackToPlaylist();
      }
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
    FavoritesManager favoritesManager = FavoritesManager.getInstance();
    int result = favoritesManager.addPlaylist(playlist);
    if (result == FavoritesManager.SUCCESS) {
      navigator.showAlert(Lang.tr("favorites.status.added"), AlertType.CONFIRMATION);
    } else if (result == FavoritesManager.ALREADY_EXISTS) {
      navigator.showAlert(Lang.tr("favorites.status.already_exists"), AlertType.INFO);
    } else {
      navigator.showAlert(Lang.tr("favorites.error.save_failed"), AlertType.ERROR);
    }
  }

  private void playTrack() {
    Tracks singleTrackPlaylist = new Tracks();
    singleTrackPlaylist.setTracks(new Track[] {track});

    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      playerScreen.change(track.getName(), singleTrackPlaylist, 0, navigator);
      Displayable previous = navigator.getPrevious();
      if (previous instanceof PlayerScreen) {
        navigator.back();
      } else {
        navigator.forward(playerScreen);
      }
    } else {
      playerScreen = new PlayerScreen(track.getName(), singleTrackPlaylist, 0, navigator);
      MIDPlay.setPlayerScreen(playerScreen);
      navigator.forward(playerScreen);
    }
  }

  private void addTrackToPlaylist() {
    FavoritesScreen selectionScreen = new FavoritesScreen(navigator, track, this);
    navigator.forward(selectionScreen);
  }
}
