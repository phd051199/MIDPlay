package midplay.ui;

import midplay.model.JsonListResult;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.player.PlayerNavHelper;

// Loads a folder's tracks on a worker thread and appends them to the current
// play queue ("Add All to Queue"), mirroring TracksListForwarder which instead
// opens them in a TrackListScreen.
public class QueueAppender extends AbstractListForwarder {
  private final String title;

  public QueueAppender(Navigator navigator, String title) {
    super(navigator, "status.no_data");
    this.title = title;
  }

  public void onDataReceived(JsonListResult result) {
    Track[] tracks = ((Tracks) result).getTracks();
    PlayerNavHelper.addToQueue(tracks, title, navigator);
  }
}
