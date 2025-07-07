package app.utils;

import app.interfaces.MainObserver;
import app.ui.MainList;
import javax.microedition.lcdui.Image;

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

  public static MainList createMainMenu(MainObserver observer, String service) {
    Image[] images = loadMainMenuIcons(service);
    String[] labels = getMainMenuItemLabels(service);
    MainList mainMenu = new MainList(I18N.tr("app_name"), labels, images);
    mainMenu.setObserver(observer);
    return mainMenu;
  }

  private Utils() {}
}
