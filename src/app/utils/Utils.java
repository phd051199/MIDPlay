package app.utils;

import app.model.Song;
import app.ui.MainList;
import app.utils.Utils.BreadCrumbTrail;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.media.MediaException;

public class Utils {

  public static boolean DEBUG = false;

  private static final String[] MAIN_MENU_ICONS = {
    "/images/Magnifier.png",
    "/images/MusicPlaylist.png",
    "/images/Heart.png",
    "/images/MusicNoteBlue.png",
    "/images/MusicNote.png",
    "/images/Album.png",
    "/images/Setting.png",
    "/images/Information.png"
  };

  private static final String[] MAIN_MENU_ITEMS = {
    "search_title",
    "billboard",
    "favorites",
    "new_playlists",
    "hot_playlists",
    "genres",
    "settings",
    "app_info"
  };

  private Utils() {}

  public static String convertString(String source) {
    while (true) {
      try {
        if (source.indexOf("&#") > 0) {
          int indexBegin = source.indexOf("&#");
          int indexEnd = source.indexOf(";");
          String sChar = source.substring(indexBegin + 2, indexEnd - 1);
          int ichar = Integer.parseInt(sChar);
          String replace = String.valueOf((char) ichar);
          source = source.substring(0, indexBegin - 1) + replace + source.substring(indexEnd + 1);
          continue;
        }
      } catch (Throwable var6) {
        var6.printStackTrace();
      }

      return source;
    }
  }

  public static void debugOut(String s) {
    if (DEBUG) {
      System.out.println(s);
    }
  }

  public static void debugOut(Throwable t) {
    if (DEBUG) {
      System.out.println(t.toString());
    }

    if (DEBUG) {
      t.printStackTrace();
    }
  }

  public static void error(Throwable t, Utils.BreadCrumbTrail bct) {
    if (DEBUG) {
      t.printStackTrace();
    }

    error(friendlyException(t), bct);
  }

  public static void error(String s, Utils.BreadCrumbTrail bct) {
    Alert alert = new Alert(I18N.tr("error"), s, (Image) null, AlertType.ERROR);
    alert.setTimeout(-2);
    bct.replaceCurrent(alert);
  }

  public static String friendlyException(Throwable t) {
    if (t instanceof MediaException && t.getMessage().indexOf(" ") > 5) {
      return t.getMessage();
    } else {
      String s = t.toString();

      while (true) {
        int dot = s.indexOf(".");
        int space = s.indexOf(" ");
        if (space < 0) {
          space = s.length();
        }

        int colon = s.indexOf(":");
        if (colon < 0) {
          colon = s.length();
        }

        if (dot < 0 || dot >= space || dot >= colon) {
          return s;
        }

        s = s.substring(dot + 1);
      }
    }
  }

  public static Image[] loadMainMenuIcons() {
    Image[] images = new Image[MAIN_MENU_ICONS.length];
    for (int i = 0; i < MAIN_MENU_ICONS.length; i++) {
      try {
        images[i] = Image.createImage(MAIN_MENU_ICONS[i]);
      } catch (Exception e) {
        e.printStackTrace();
        images[i] = null;
      }
    }
    return images;
  }

  public static String[] getMainMenuItemLabels() {
    String[] labels = new String[MAIN_MENU_ITEMS.length];
    for (int i = 0; i < MAIN_MENU_ITEMS.length; i++) {
      labels[i] = I18N.tr(MAIN_MENU_ITEMS[i]);
    }
    return labels;
  }

  public static MainList createMainMenu(BreadCrumbTrail observer) {
    Image[] images = loadMainMenuIcons();
    String[] labels = getMainMenuItemLabels();
    MainList mainMenu = new MainList(I18N.tr("app_name"), labels, images);
    mainMenu.setObserver(observer);
    return mainMenu;
  }

  public interface Interruptable {

    void pauseApp();

    void resumeApp();
  }

  interface QueryListener {

    void queryOK(String var1);

    void queryCancelled();
  }

  public interface ContentHandler {

    void close();

    boolean canHandle(String var1);

    void handle(Song var1);
  }

  public interface BreadCrumbTrail {

    Displayable go(Displayable var1);

    Displayable goBack();

    void handle(Song var1);

    Displayable replaceCurrent(Displayable var1);

    Displayable getCurrentDisplayable();
  }

  public static class QueryTask implements CommandListener, Runnable {

    private static Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);
    private static Command OKCommand = new Command("OK", Command.OK, 1);
    private Utils.QueryListener queryListener;
    private Utils.BreadCrumbTrail queryBCT;
    private static String queryText = "";

    private QueryTask(Utils.QueryListener listener, Utils.BreadCrumbTrail bct) {
      this.queryListener = listener;
      this.queryBCT = bct;
    }

    public void commandAction(Command c, Displayable s) {
      if (this.queryBCT != null) {
        Utils.debugOut("Utils.commandAction: goBack()");
      }

      if (c == cancelCommand) {
        Utils.debugOut("Command: cancel");
        if (this.queryListener != null) {
          this.queryListener.queryCancelled();
        }
      } else if (c == OKCommand) {
        Utils.debugOut("Command: OK");
        if (this.queryListener != null) {
          queryText = "";
          if (s instanceof TextBox) {
            queryText = ((TextBox) s).getString();
          }

          (new Thread(this)).start();
        }
      }
    }

    public void run() {
      this.sendListenerEvent();
    }

    private void sendListenerEvent() {
      if (this.queryListener != null) {
        this.queryListener.queryOK(queryText);
      }
    }

    QueryTask(Utils.QueryListener x0, Utils.BreadCrumbTrail x1, Object x2) {
      this(x0, x1);
    }
  }
}
