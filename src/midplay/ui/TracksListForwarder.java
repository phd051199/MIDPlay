package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.model.JsonListResult;
import midplay.model.Tracks;
import midplay.net.JsonOperation;
import midplay.ui.screen.TrackListScreen;
import midplay.util.Lang;

// Standard handler for "fetch tracks on a worker thread, then open them in a
// TrackListScreen". Replaces the near-identical anonymous listener instances
// that were inlined into every track-opening screen. PlaylistDetailScreen is
// not a fit (it updates an in-place count instead of navigating), so it keeps
// its own listener.
public class TracksListForwarder implements JsonOperation.JsonListListener {
  private final Navigator navigator;
  private final String title;
  private final String noDataKey;

  public TracksListForwarder(Navigator navigator, String title, String noDataKey) {
    this.navigator = navigator;
    this.title = title;
    this.noDataKey = noDataKey;
  }

  public void onDataReceived(JsonListResult result) {
    navigator.forward(new TrackListScreen(title, (Tracks) result, navigator));
  }

  public void onNoData() {
    navigator.showAlert(Lang.tr(noDataKey), AlertType.INFO);
  }

  public void onError(Exception e) {
    navigator.showAlert(e.toString(), AlertType.ERROR);
  }
}
