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

  public static Command checkUpdate() {
    return get("settings.check_update", Command.SCREEN, 2);
  }

  private Commands() {}

  public static class Favorites {
    public static Command add() {
      return get("favorites.add", Command.ITEM, 1);
    }

    public static Command create() {
      return get("playlist.create", Command.SCREEN, 2);
    }

    public static Command remove() {
      return get("favorites.remove", Command.ITEM, 3);
    }

    public static Command rename() {
      return get("playlist.rename", Command.ITEM, 4);
    }

    private Favorites() {}
  }

  public static class Form {
    public static Command save() {
      return get("action.save", Command.SCREEN, 1);
    }

    public static Command cancel() {
      return get("action.cancel", Command.BACK, 2);
    }

    private Form() {}
  }

  public static class SleepTimer {
    public static Command setTimer() {
      return get("timer.set", Command.SCREEN, 1);
    }

    public static Command switchToCountdown() {
      return get("timer.mode.countdown", Command.SCREEN, 2);
    }

    public static Command switchToAbsolute() {
      return get("timer.mode.absolute", Command.SCREEN, 2);
    }

    private SleepTimer() {}
  }

  public static class Player {
    public static Command play() {
      return get("player.play", Command.OK, 1);
    }

    public static Command next() {
      return get("player.next", Command.SCREEN, 2);
    }

    public static Command previous() {
      return get("player.previous", Command.SCREEN, 3);
    }

    public static Command stop() {
      return get("player.stop", Command.SCREEN, 4);
    }

    public static Command volume() {
      return get("player.volume", Command.SCREEN, 5);
    }

    public static Command repeat() {
      return get("player.repeat", Command.SCREEN, 5);
    }

    public static Command shuffle() {
      return get("player.shuffle", Command.SCREEN, 7);
    }

    public static Command addToPlaylist() {
      return get("playlist.add_track", Command.SCREEN, 8);
    }

    public static Command showPlaylist() {
      return get("player.show_playlist", Command.SCREEN, 9);
    }

    public static Command sleepTimer() {
      return get("timer.sleep_timer", Command.SCREEN, 10);
    }

    public static Command cancelTimer() {
      return get("timer.cancel", Command.SCREEN, 11);
    }

    public static Command nowPlaying() {
      return get("menu.now_playing", Command.SCREEN, 12);
    }

    private Player() {}
  }

  public static class Menu {
    public static Command sort() {
      return get("menu.reorder", Command.SCREEN, 1);
    }

    public static Command visibility() {
      return get("menu.visibility", Command.SCREEN, 2);
    }

    private Menu() {}
  }
}
