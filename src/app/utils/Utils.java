package app.utils;

import app.common.MenuSettingsManager;
import app.constants.Services;
import app.interfaces.MainObserver;
import app.ui.MainList;
import javax.microedition.lcdui.Image;

public class Utils {

  public static boolean DEBUG = false;

  public static final String[] MAIN_MENU_ICONS_NCT = {
    "/images/Magnifier.png",
    "/images/Heart.png",
    "/images/Album.png",
    "/images/MusicPlaylist.png",
    "/images/MusicNoteBlue.png",
    "/images/MusicNote.png",
    "/images/MessagingChat.png",
    "/images/Setting.png",
    "/images/Information.png",
  };

  public static final String[] MAIN_MENU_ITEMS_NCT = {
    "search_title",
    "favorites",
    "genres",
    "billboard",
    "new_playlists",
    "hot_playlists",
    "chat",
    "settings",
    "app_info",
  };

  public static final String[] MAIN_MENU_ICONS_SOUNDCLOUD = {
    "/images/Magnifier.png",
    "/images/Heart.png",
    "/images/Album.png",
    "/images/MessagingChat.png",
    "/images/Setting.png",
    "/images/Information.png",
  };

  public static final String[] MAIN_MENU_ITEMS_SOUNDCLOUD = {
    "search_title", "favorites", "discover_playlists", "chat", "settings", "app_info",
  };

  public static Image[] loadMainMenuIcons(String service) {
    String[] icons =
        service.equals(Services.NCT) ? MAIN_MENU_ICONS_NCT : MAIN_MENU_ICONS_SOUNDCLOUD;
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
    String[] items =
        service.equals(Services.NCT) ? MAIN_MENU_ITEMS_NCT : MAIN_MENU_ITEMS_SOUNDCLOUD;
    String[] labels = new String[items.length];
    for (int i = 0; i < items.length; i++) {
      labels[i] = I18N.tr(items[i]);
    }
    return labels;
  }

  public static MainList createMainMenu(MainObserver parent, String service) {
    return Services.NCT.equals(service)
        ? createNCTMainMenu(parent)
        : createSoundcloudMainMenu(parent, service);
  }

  private static MainList createNCTMainMenu(MainObserver parent) {
    MenuSettingsManager menuSettingsManager = MenuSettingsManager.getInstance();
    int[] menuOrder = menuSettingsManager.getNctMenuOrder(MAIN_MENU_ITEMS_NCT.length);
    boolean[] menuVisibility = menuSettingsManager.getNctMenuVisibility(MAIN_MENU_ITEMS_NCT.length);

    MainList l =
        new MainList(
            I18N.tr("app_name") + " - " + Services.NCT,
            I18N.tr("main_list_description"),
            Services.NCT);

    for (int i = 0; i < menuOrder.length; i++) {
      int originalIndex = menuOrder[i];
      if (originalIndex < 0 || originalIndex >= MAIN_MENU_ITEMS_NCT.length) {
        continue;
      }

      if (!menuVisibility[originalIndex]) {
        continue;
      }

      try {
        l.append(
            I18N.tr(MAIN_MENU_ITEMS_NCT[originalIndex]),
            Image.createImage(MAIN_MENU_ICONS_NCT[originalIndex]));
      } catch (Exception var4) {
        l.append(I18N.tr(MAIN_MENU_ITEMS_NCT[originalIndex]), null);
      }
    }

    l.setObserver(parent);
    return l;
  }

  private static MainList createSoundcloudMainMenu(MainObserver parent, String service) {
    MenuSettingsManager menuSettingsManager = MenuSettingsManager.getInstance();
    int[] menuOrder = menuSettingsManager.getSoundcloudMenuOrder(MAIN_MENU_ITEMS_SOUNDCLOUD.length);
    boolean[] menuVisibility =
        menuSettingsManager.getSoundcloudMenuVisibility(MAIN_MENU_ITEMS_SOUNDCLOUD.length);

    MainList l =
        new MainList(
            I18N.tr("app_name") + " - " + service, I18N.tr("main_list_description"), service);

    for (int i = 0; i < menuOrder.length; i++) {
      int originalIndex = menuOrder[i];
      if (originalIndex < 0 || originalIndex >= MAIN_MENU_ITEMS_SOUNDCLOUD.length) {
        continue;
      }

      if (!menuVisibility[originalIndex]) {
        continue;
      }

      try {
        l.append(
            I18N.tr(MAIN_MENU_ITEMS_SOUNDCLOUD[originalIndex]),
            Image.createImage(MAIN_MENU_ICONS_SOUNDCLOUD[originalIndex]));
      } catch (Exception var4) {
        l.append(I18N.tr(MAIN_MENU_ITEMS_SOUNDCLOUD[originalIndex]), null);
      }
    }

    l.setObserver(parent);
    return l;
  }

  private Utils() {}
}
