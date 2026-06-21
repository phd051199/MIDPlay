package midplay.ui.screen;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import midplay.model.Track;
import midplay.player.PlayerNavHelper;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;

// Track detail screen: shows name/artist/duration/url and lets the user play the
// track or add it to a custom playlist.
public final class TrackDetailScreen extends BaseForm {
  private final Track track;

  private StringItem nameItem;
  private StringItem artistItem;
  private StringItem durationItem;
  private TextField urlField;

  public TrackDetailScreen(Track track, Navigator navigator) {
    super(Lang.tr("details.title"), navigator);
    this.track = track;

    addCommand(Commands.playerPlay());
    addCommand(Commands.playerAddToPlaylist());

    initializeItems();
  }

  private void initializeItems() {
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

  private String formatDuration(int seconds) {
    if (seconds <= 0) {
      return Lang.tr("details.unknown_duration");
    }
    int minutes = seconds / 60;
    int remainingSeconds = seconds % 60;
    return minutes + ":" + (remainingSeconds < 10 ? "0" : "") + remainingSeconds;
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.playerPlay()) {
      playTrack();
    } else if (c == Commands.playerAddToPlaylist()) {
      addTrackToPlaylist();
    }
  }

  private void playTrack() {
    PlayerNavHelper.playSingleTrack(track.getName(), track, navigator);
  }

  private void addTrackToPlaylist() {
    PlaylistPickerScreen selectionScreen = new PlaylistPickerScreen(navigator, track, this);
    navigator.forward(selectionScreen);
  }
}
