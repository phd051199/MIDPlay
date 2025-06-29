package app.utils;

import app.model.Song;
import app.ui.MainList;
import app.utils.Utils.BreadCrumbTrail;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;

public class Utils {

  public static boolean DEBUG = false;

  private static final String[] MAIN_MENU_ICONS_NCT = {
    "/images/Magnifier.png",
    "/images/Heart.png",
    "/images/MessagingChat.png",
    "/images/Album.png",
    "/images/MusicNoteBlue.png",
    "/images/MusicNote.png",
    "/images/MusicPlaylist.png",
    "/images/Setting.png",
    "/images/Information.png",
  };

  private static final String[] MAIN_MENU_ITEMS_NCT = {
    "search_title",
    "favorites",
    "chat",
    "genres",
    "new_playlists",
    "hot_playlists",
    "billboard",
    "settings",
    "app_info",
  };

  private static final String[] MAIN_MENU_ICONS_SOUNDCLOUD = {
    "/images/Magnifier.png",
    "/images/Heart.png",
    "/images/MessagingChat.png",
    "/images/Album.png",
    "/images/Setting.png",
    "/images/Information.png",
  };

  private static final String[] MAIN_MENU_ITEMS_SOUNDCLOUD = {
    "search_title", "favorites", "chat", "discover_playlists", "settings", "app_info",
  };

  public static String convertString(String source) {
    while (true) {
      try {
        if (source.indexOf("&#") > 0) {
          int indexBegin = source.indexOf("&#");
          int indexEnd = source.indexOf(';');
          String sChar = source.substring(indexBegin + 2, indexEnd - 1);
          int ichar = Integer.parseInt(sChar);
          String replace = String.valueOf((char) ichar);
          source = source.substring(0, indexBegin - 1) + replace + source.substring(indexEnd + 1);
          continue;
        }
      } catch (Throwable var6) {
      }
      return source;
    }
  }

  public static Image[] loadMainMenuIcons(String service) {
    String[] icons = service.equals("nct") ? MAIN_MENU_ICONS_NCT : MAIN_MENU_ICONS_SOUNDCLOUD;
    Image[] images = new Image[icons.length];
    for (int i = 0; i < icons.length; i++) {
      try {
        images[i] = Image.createImage(icons[i]);
      } catch (Exception e) {
        images[i] = null;
      }
    }
    return images;
  }

  public static String[] getMainMenuItemLabels(String service) {
    String[] items = service.equals("nct") ? MAIN_MENU_ITEMS_NCT : MAIN_MENU_ITEMS_SOUNDCLOUD;
    String[] labels = new String[items.length];
    for (int i = 0; i < items.length; i++) {
      labels[i] = I18N.tr(items[i]);
    }
    return labels;
  }

  public static MainList createMainMenu(BreadCrumbTrail observer, String service) {
    Image[] images = loadMainMenuIcons(service);
    String[] labels = getMainMenuItemLabels(service);
    MainList mainMenu = new MainList(I18N.tr("app_name"), labels, images);
    mainMenu.setObserver(observer);
    return mainMenu;
  }

  private Utils() {}

  public static class QueryTask implements CommandListener, Runnable {

    private static Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);
    private static Command OKCommand = new Command("OK", Command.OK, 1);
    private static String queryText = "";
    private Utils.QueryListener queryListener;
    private Utils.BreadCrumbTrail queryBCT;

    private QueryTask(Utils.QueryListener listener, Utils.BreadCrumbTrail bct) {
      this.queryListener = listener;
      this.queryBCT = bct;
    }

    QueryTask(Utils.QueryListener x0, Utils.BreadCrumbTrail x1, Object x2) {
      this(x0, x1);
    }

    public void commandAction(Command c, Displayable s) {

      if (c == cancelCommand) {
        if (this.queryListener != null) {
          this.queryListener.queryCancelled();
        }
      } else if (c == OKCommand) {
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
}
