package app.utils.ui;

import app.ui.MainList;
import app.ui.MainObserver;
import app.ui.MenuFactory;

public class UiUtils {

  public static boolean DEBUG = false;

  public static MainList createMainMenu(MainObserver parent, String service) {
    return MenuFactory.createMainMenu(parent, service);
  }

  private UiUtils() {}
}
