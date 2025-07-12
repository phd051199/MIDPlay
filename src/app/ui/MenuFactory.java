package app.ui;

import app.constants.MenuConstants;
import app.constants.Services;
import app.core.settings.SettingsManager;
import app.utils.text.LocalizationManager;
import javax.microedition.lcdui.Image;

public class MenuFactory {

  public static Image[] loadMainMenuIcons(String service) {
    String[] icons = MenuConstants.getMenuIcons(service);
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
    String[] items = MenuConstants.getMenuItems(service);
    String[] labels = new String[items.length];
    for (int i = 0; i < items.length; i++) {
      labels[i] = LocalizationManager.tr(items[i]);
    }
    return labels;
  }

  public static MainList createMainMenu(MainObserver parent, String service) {
    return Services.NCT.equals(service)
        ? createNCTMainMenu(parent)
        : createSoundcloudMainMenu(parent, service);
  }

  private static MainList createNCTMainMenu(MainObserver parent) {
    SettingsManager settingsManager = SettingsManager.getInstance();
    int[] menuOrder = settingsManager.getNctMenuOrder(MenuConstants.MAIN_MENU_ITEMS_NCT.length);
    boolean[] menuVisibility =
        settingsManager.getNctMenuVisibility(MenuConstants.MAIN_MENU_ITEMS_NCT.length);

    MainList mainList =
        new MainList(
            LocalizationManager.tr("app_name") + " - " + Services.NCT,
            LocalizationManager.tr("main_list_description"),
            Services.NCT);

    for (int i = 0; i < menuOrder.length; i++) {
      int originalIndex = menuOrder[i];
      if (originalIndex < 0 || originalIndex >= MenuConstants.MAIN_MENU_ITEMS_NCT.length) {
        continue;
      }

      if (!menuVisibility[originalIndex]) {
        continue;
      }

      try {
        mainList.append(
            LocalizationManager.tr(MenuConstants.MAIN_MENU_ITEMS_NCT[originalIndex]),
            Image.createImage(MenuConstants.MAIN_MENU_ICONS_NCT[originalIndex]));
      } catch (Exception exception) {
        mainList.append(
            LocalizationManager.tr(MenuConstants.MAIN_MENU_ITEMS_NCT[originalIndex]), null);
      }
    }

    mainList.setObserver(parent);
    return mainList;
  }

  private static MainList createSoundcloudMainMenu(MainObserver parent, String service) {
    SettingsManager settingsManager = SettingsManager.getInstance();
    int[] menuOrder =
        settingsManager.getSoundcloudMenuOrder(MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD.length);
    boolean[] menuVisibility =
        settingsManager.getSoundcloudMenuVisibility(
            MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD.length);

    MainList mainList =
        new MainList(
            LocalizationManager.tr("app_name") + " - " + service,
            LocalizationManager.tr("main_list_description"),
            service);

    for (int i = 0; i < menuOrder.length; i++) {
      int originalIndex = menuOrder[i];
      if (originalIndex < 0 || originalIndex >= MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD.length) {
        continue;
      }

      if (!menuVisibility[originalIndex]) {
        continue;
      }

      try {
        mainList.append(
            LocalizationManager.tr(MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD[originalIndex]),
            Image.createImage(MenuConstants.MAIN_MENU_ICONS_SOUNDCLOUD[originalIndex]));
      } catch (Exception exception) {
        mainList.append(
            LocalizationManager.tr(MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD[originalIndex]), null);
      }
    }

    mainList.setObserver(parent);
    return mainList;
  }

  private MenuFactory() {}
}
