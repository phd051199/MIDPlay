package midplay.ui;

import midplay.model.JsonListResult;
import midplay.model.Tracks;
import midplay.ui.screen.TrackListScreen;

// Fetches tracks on a worker thread and opens them in a TrackListScreen.
public class TracksListForwarder extends AbstractListForwarder {
  private final String title;
  private final boolean playSingleTrack;

  public TracksListForwarder(Navigator navigator, String title, String noDataKey) {
    this(navigator, title, noDataKey, false);
  }

  // playSingleTrack: when true (search results), tapping a row plays only that
  // one track instead of queuing the whole result set.
  public TracksListForwarder(
      Navigator navigator, String title, String noDataKey, boolean playSingleTrack) {
    super(navigator, noDataKey);
    this.title = title;
    this.playSingleTrack = playSingleTrack;
  }

  public void onDataReceived(JsonListResult result) {
    navigator.forward(
        new TrackListScreen(title, (Tracks) result, navigator, null, playSingleTrack));
  }
}
