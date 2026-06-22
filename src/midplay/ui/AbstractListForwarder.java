package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.net.JsonOperation;
import midplay.util.Lang;

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
