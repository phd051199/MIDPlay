package midplay.ui;

import midplay.MIDPlay;
import midplay.model.JsonListResult;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.player.PlayerScreen;

public class QueueAppender extends AbstractListForwarder {
  private final String title;
  private final int playlistGeneration;

  public QueueAppender(Navigator navigator, String title, int playlistGeneration) {
    super(navigator, "status.no_data");
    this.title = title;
    this.playlistGeneration = playlistGeneration;
  }

  public void onDataReceived(JsonListResult result) {
    Track[] tracks = ((Tracks) result).getTracks();
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playlistGeneration >= 0
        && (playerScreen == null
            || playerScreen.getPlayerGUI().playlistGeneration() != playlistGeneration)) {
      return;
    }
    PlayerNavHelper.addToQueue(tracks, title, navigator);
  }
}
