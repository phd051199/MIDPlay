package midplay.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import midplay.MIDPlay;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.net.JsonOperation;
import midplay.player.PlayerScreen;
import midplay.store.Configuration;
import midplay.store.RecentManager;
import midplay.util.Lang;

public final class PlayerNavHelper {
  private PlayerNavHelper() {}

  public static void loadRemotePlaylistTracks(Navigator navigator, String key, String name) {
    MIDPlay.startOperation(
        JsonOperation.getTracks(key, new TracksListForwarder(navigator, name, "status.no_data")));
  }

  public static void loadRemotePlaylistToQueue(Navigator navigator, String key, String name) {
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    int playlistGeneration =
        (playerScreen != null) ? playerScreen.getPlayerGUI().playlistGeneration() : -1;
    MIDPlay.startOperation(
        JsonOperation.getTracks(key, new QueueAppender(navigator, name, playlistGeneration)));
  }

  public static void playTrackFromList(
      String title, Tracks tracks, int index, long positionMicros, Navigator navigator) {
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      playerScreen.change(title, tracks, index, positionMicros, navigator);
      Displayable previous = navigator.getPrevious();
      if (previous instanceof PlayerScreen) {
        navigator.back();
      } else {
        navigator.forward(playerScreen);
      }
    } else {
      playerScreen = new PlayerScreen(title, tracks, index, positionMicros, navigator);
      MIDPlay.setPlayerScreen(playerScreen);
      navigator.forward(playerScreen);
    }
  }

  public static void playSingleTrack(String title, Track track, Navigator navigator) {
    RecentManager.getInstance().recordTrack(track);
    Tracks single = new Tracks();
    single.setTracks(new Track[] {track});
    playTrackFromList(title, single, 0, 0L, navigator);
  }

  public static void addToQueue(Track[] toAdd, String title, Navigator navigator) {
    if (toAdd == null || toAdd.length == 0) {
      return;
    }
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen == null) {
      Tracks seeds = new Tracks();
      seeds.setTracks(toAdd);
      playTrackFromList(title, seeds, 0, 0L, navigator);
      return;
    }
    playerScreen.getPlayerGUI().addToQueue(toAdd);
    navigator.showAlert(Lang.tr("queue.added"), AlertType.CONFIRMATION);
  }

  public static void showNowPlaying(Navigator navigator) {
    Displayable previous = navigator.getPrevious();
    if (previous instanceof PlayerScreen) {
      navigator.back();
      return;
    }
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      navigator.forward(playerScreen);
    } else {
      navigator.showAlert(Lang.tr(Configuration.PLAYER_STATUS_STOPPED), AlertType.INFO);
    }
  }
}
