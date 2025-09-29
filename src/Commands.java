import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Command;

public class Commands {
  private static final Hashtable cache = new Hashtable();
  private static final Vector keys = new Vector();

  private static Command get(String key, int type, int priority) {
    if (cache.containsKey(key)) {
      return (Command) cache.get(key);
    }
    Command cmd = new Command(Lang.tr(key), type, priority);
    cache.put(key, cmd);
    keys.addElement(key);
    return cmd;
  }

  public static void refresh() {
    cache.clear();
  }

  public static Command ok() {
    return get("action.ok", Command.OK, 0);
  }

  public static Command cancel() {
    return get("action.cancel", Command.CANCEL, 1);
  }

  public static Command back() {
    return get("action.back", Command.BACK, 1);
  }

  public static Command exit() {
    return get("action.exit", Command.EXIT, 1);
  }

  public static Command input() {
    return get("chat.input", Command.SCREEN, 0);
  }

  public static Command checkUpdate() {
    return get("settings.check_update", Command.SCREEN, 3);
  }

  public static Command playlistAdd() {
    return get("favorites.add", Command.ITEM, 1);
  }

  public static Command playlistCreate() {
    return get("playlist.create", Command.SCREEN, 2);
  }

  public static Command playlistRemove() {
    return get("favorites.remove", Command.ITEM, 3);
  }

  public static Command playlistRename() {
    return get("playlist.rename", Command.ITEM, 4);
  }

  public static Command formSave() {
    return get("action.save", Command.SCREEN, 1);
  }

  public static Command formCancel() {
    return get("action.cancel", Command.BACK, 2);
  }

  public static Command timerSet() {
    return get("timer.set", Command.SCREEN, 1);
  }

  public static Command playerPlay() {
    return get("player.play", Command.OK, 1);
  }

  public static Command playerNext() {
    return get("player.next", Command.SCREEN, 2);
  }

  public static Command playerPrevious() {
    return get("player.previous", Command.SCREEN, 3);
  }

  public static Command playerStop() {
    return get("player.stop", Command.SCREEN, 4);
  }

  public static Command playerVolume() {
    return get("player.volume", Command.SCREEN, 5);
  }

  public static Command playerRepeat() {
    return get("player.repeat", Command.SCREEN, 5);
  }

  public static Command playerShuffle() {
    return get("player.shuffle", Command.SCREEN, 7);
  }

  public static Command playerAddToPlaylist() {
    return get("playlist.add_track", Command.SCREEN, 8);
  }

  public static Command playerShowPlaylist() {
    return get("player.show_playlist", Command.SCREEN, 9);
  }

  public static Command playerSleepTimer() {
    return get("timer.sleep_timer", Command.SCREEN, 10);
  }

  public static Command playerCancelTimer() {
    return get("timer.cancel", Command.SCREEN, 11);
  }

  public static Command playerNowPlaying() {
    return get("menu.now_playing", Command.SCREEN, 12);
  }

  public static Command menuSort() {
    return get("menu.reorder", Command.SCREEN, 1);
  }

  public static Command menuVisibility() {
    return get("menu.visibility", Command.SCREEN, 2);
  }

  public static Command playlistViewTracks() {
    return get("playlist.view_tracks", Command.SCREEN, 2);
  }

  public static Command details() {
    return get("details.title", Command.ITEM, 2);
  }

  private Commands() {}
}
