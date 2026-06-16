package midplay.player;

import midplay.MIDPlay;
import midplay.model.Track;

// Read-only view onto the singleton PlayerScreen's current track, so list screens
// can mark the row that is cued/playing without each one re-deriving the lookup.
public final class NowPlaying {
  private NowPlaying() {}

  public static Track currentTrack() {
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen == null) {
      return null;
    }
    try {
      return playerScreen.getPlayerGUI().getCurrentTrack();
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isCurrent(Track track) {
    Track current = currentTrack();
    return current != null && current.isSame(track);
  }
}
