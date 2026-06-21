package midplay.player;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import midplay.MIDPlay;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.store.RecentManager;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class PlayerNavHelper {
  private PlayerNavHelper() {}

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

  // Play exactly one track in its own single-item list and record it to Recent.
  // Centralises the record + wrap + playTrackFromList idiom that list/detail
  // screens used to copy verbatim.
  public static void playSingleTrack(String title, Track track, Navigator navigator) {
    RecentManager.getInstance().recordTrack(track);
    Tracks single = new Tracks();
    single.setTracks(new Track[] {track});
    playTrackFromList(title, single, 0, 0L, navigator);
  }

  // Add tracks to the current queue. If nothing is playing yet (no player
  // screen has ever been created), start playback instead so "Add to queue" from
  // a cold state is visible rather than silently enqueued into nothing.
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

  // "Now Playing" entry from a list or menu: reuse the singleton PlayerScreen
  // (back to it if it is directly beneath us, otherwise forward it), or show a
  // "stopped" alert when playback was never started. Centralises the decision
  // that BaseList and the main menu used to duplicate.
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
