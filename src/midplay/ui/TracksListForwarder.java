package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.model.JsonListResult;
import midplay.model.Tracks;
import midplay.net.JsonOperation;
import midplay.ui.screen.TrackListScreen;
import midplay.util.Lang;

// Fetches tracks on a worker thread and opens them in a TrackListScreen.
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
