package midplay.player;

import midplay.MIDPlay;
import midplay.store.Configuration;
import midplay.ui.BaseList;
import midplay.ui.Navigator;
import midplay.util.Lang;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import midplay.model.Tracks;

public final class PlayerNavHelper {
  private PlayerNavHelper() {}

  public static void playTrackFromList(String title, Tracks tracks, int index, Navigator navigator) {
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      playerScreen.change(title, tracks, index, navigator);
      Displayable previous = navigator.getPrevious();
      if (previous instanceof PlayerScreen) {
        navigator.back();
      } else {
        navigator.forward(playerScreen);
      }
    } else {
      playerScreen = new PlayerScreen(title, tracks, index, navigator);
      MIDPlay.setPlayerScreen(playerScreen);
      navigator.forward(playerScreen);
    }
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
