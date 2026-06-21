package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.net.JsonOperation;
import midplay.util.Lang;

// Shared base for the small "fetch a JSON list on a worker thread, then act"
// adapters (TracksListForwarder, PlaylistsListForwarder, QueueAppender). Each
// subclass implements only onDataReceived; the no-data and error handling
// (show a translated alert) is identical across all of them.
public abstract class AbstractListForwarder implements JsonOperation.JsonListListener {
  protected final Navigator navigator;
  private final String noDataKey;

  protected AbstractListForwarder(Navigator navigator, String noDataKey) {
    this.navigator = navigator;
    this.noDataKey = noDataKey;
  }

  public void onNoData() {
    navigator.showAlert(Lang.tr(noDataKey), AlertType.INFO);
  }

  public void onError(Exception e) {
    navigator.showAlert(e.toString(), AlertType.ERROR);
  }
}
