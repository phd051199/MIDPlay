package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.DownloadManager;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class LinkTrackPickerScreen extends TrackListScreen {
  private final DownloadManager downloadManager = DownloadManager.getInstance();
  private final String path;
  private final Displayable returnScreen;

  public LinkTrackPickerScreen(
      String title, Tracks items, Navigator navigator, String path, Displayable returnScreen) {
    super(title, items, navigator, null);
    this.path = path;
    this.returnScreen = returnScreen;
    removeCommand(Commands.download());
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, tracks.length)) {
      return;
    }
    Track track = tracks[index];
    if (downloadManager.linkLocalFile(
        path, track.getUrl(), track.getImageUrl(), track.getName(), track.getArtist())) {
      if (returnScreen instanceof LocalFileLinkScreen) {
        ((LocalFileLinkScreen) returnScreen).onFileLinked();
      }
      navigator.popTo(returnScreen);
      navigator.showAlert(Lang.tr("download.link_status"), AlertType.CONFIRMATION, returnScreen);
    } else {
      navigator.showAlert(Lang.tr("download.link_error"), AlertType.ERROR);
    }
  }
}
