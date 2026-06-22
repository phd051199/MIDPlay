package midplay.ui;

import midplay.model.JsonListResult;
import midplay.model.Tracks;
import midplay.ui.screen.TrackListScreen;

public class TracksListForwarder extends AbstractListForwarder {
  private final String title;

  public TracksListForwarder(Navigator navigator, String title, String noDataKey) {
    super(navigator, noDataKey);
    this.title = title;
  }

  public void onDataReceived(JsonListResult result) {
    navigator.forward(new TrackListScreen(title, (Tracks) result, navigator, null));
  }
}
