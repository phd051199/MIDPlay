import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import model.MenuItem;
import model.Playlist;
import model.Playlists;

public class MIDPlay extends MIDlet implements CommandListener {
  private static NetworkOperation operation;
  private static final Hashtable iconMap = new Hashtable();
  public static String APP_VERSION = "1.0";
  private static PlayerScreen playerScreen;
  private static int playerHttpMethod;
  private static MIDPlay instance;

  public static void startOperation(NetworkOperation op) {
    operation = op;
    operation.start();
  }

  public static void cancelOperation() {
    if (operation != null) {
      operation.stop();
    }
  }

  public static String replace(String text, String searchString, String replacement) {
    StringBuffer sb = new StringBuffer();
    int pos = 0;
    int found;
    int searchLength = searchString.length();
    while ((found = text.indexOf(searchString, pos)) != -1) {
      sb.append(text.substring(pos, found)).append(replacement);
      pos = found + searchLength;
    }
    sb.append(text.substring(pos));
    return sb.toString();
  }

  public static String urlEncode(String input) {
    StringBuffer encoded = new StringBuffer();
    for (int i = 0; i < input.length(); ++i) {
      char ch = input.charAt(i);
      if ((ch >= 'A' && ch <= 'Z')
          || (ch >= 'a' && ch <= 'z')
          || (ch >= '0' && ch <= '9')
          || ch == '-'
          || ch == '_'
          || ch == '.'
          || ch == '~') {
        encoded.append(ch);
      } else {
        encoded.append('%');
        encoded.append(toHexChar((ch >> 4) & 0xF));
        encoded.append(toHexChar(ch & 0xF));
      }
    }
    return encoded.toString();
  }

  private static char toHexChar(int digit) {
    if (digit < 10) {
      return (char) ('0' + digit);
    } else {
      return (char) ('A' + digit - 10);
    }
  }

  public static PlayerScreen getPlayerScreen() {
    return playerScreen;
  }

  public static void setPlayerScreen(PlayerScreen ps) {
    playerScreen = ps;
  }

  public static MIDPlay getInstance() {
    return instance;
  }

  public static void bubbleSort(Vector vector, int sortType) {
    for (int i = 0; i < vector.size() - 1; i++) {
      for (int j = 0; j < vector.size() - 1 - i; j++) {
        Object obj1 = vector.elementAt(j);
        Object obj2 = vector.elementAt(j + 1);
        boolean shouldSwap = false;
        if (sortType == 1) {
          Playlist p1 = (Playlist) obj1;
          Playlist p2 = (Playlist) obj2;
          shouldSwap = p1.getId() < p2.getId();
        } else if (sortType == 2) {
          MenuItem m1 = (MenuItem) obj1;
          MenuItem m2 = (MenuItem) obj2;
          shouldSwap = m1.order > m2.order;
        }
        if (shouldSwap) {
          vector.setElementAt(obj2, j);
          vector.setElementAt(obj1, j + 1);
        }
      }
    }
  }

  private final SettingsManager settingsManager;
  private final Navigator navigator;
  private List menu;
  private final MenuManager menuManager;
  private final Hashtable visibilityChanges = new Hashtable();
  private boolean isSortMode = false;
  private boolean isVisibilityMode = false;
  private int selectedItemIndex = -1;

  public MIDPlay() {
    instance = this;
    menuManager = MenuManager.getInstance();
    settingsManager = SettingsManager.getInstance();
    navigator = new Navigator(this);
    registerMenuActions();
  }

  private void registerMenuActions() {
    menuManager.registerAction(
        Configuration.Menu.SEARCH,
        new Runnable() {
          public void run() {
            goToSearchScreen();
          }
        });
    menuManager.registerAction(
        Configuration.Menu.FAVORITES,
        new Runnable() {
          public void run() {
            goToFavoritesScreen();
          }
        });
    menuManager.registerAction(
        Configuration.Menu.DISCOVER_PLAYLISTS,
        new Runnable() {
          public void run() {
            goToDiscoverPlaylistsScreen();
          }
        });
    menuManager.registerAction(
        Configuration.Menu.SETTINGS,
        new Runnable() {
          public void run() {
            goToSettingsScreen();
          }
        });
    menuManager.registerAction(
        Configuration.Menu.ABOUT,
        new Runnable() {
          public void run() {
            goToAboutScreen();
          }
        });
  }

  private void loadMenuIcons() {
    try {
      Configuration.Images.load();
      iconMap.put(Configuration.Menu.SEARCH, Configuration.Images.searchIcon);
      iconMap.put(Configuration.Menu.FAVORITES, Configuration.Images.favoriteIcon);
      iconMap.put(Configuration.Menu.DISCOVER_PLAYLISTS, Configuration.Images.playlistIcon);
      iconMap.put(Configuration.Menu.SETTINGS, Configuration.Images.settingsIcon);
      iconMap.put(Configuration.Menu.ABOUT, Configuration.Images.infoIcon);
    } catch (IOException e) {
      showError(e.toString());
    }
  }

  protected void startApp() throws MIDletStateChangeException {
    APP_VERSION = getAppProperty("MIDlet-Version");
    settingsManager.loadSettings();
    loadMenuIcons();
    createMenu();
    checkForUpdate();
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {}

  public void commandAction(Command c, Displayable d) {
    try {
      if (c == List.SELECT_COMMAND) {
        handleListSelection();
      } else if (c == Commands.exit()) {
        showExitConfirmation();
      } else if (c == Commands.Menu.sort()) {
        toggleSortMode();
      } else if (c == Commands.Menu.visibility()) {
        toggleVisibilityMode();
      } else if (c == Commands.Form.save()) {
        handleSave();
      } else if (c == Commands.Form.cancel()) {
        handleCancel();
      } else if (c == Commands.Player.nowPlaying()) {
        navigator.forward(getPlayerScreen());
      }
    } catch (Exception e) {
      showError(e.toString());
    }
  }

  private void handleSave() {
    if (isSortMode) {
      saveCurrentOrder();
      exitSortMode();
    } else if (isVisibilityMode) {
      saveCurrentVisibility();
      exitVisibilityMode();
    }
    refreshMenu();
  }

  private void handleCancel() {
    if (isSortMode) {
      exitSortMode();
    } else if (isVisibilityMode) {
      exitVisibilityMode();
    }
    refreshMenu();
  }

  private void createMenu() {
    menu = new List(Lang.tr("app.name"), List.IMPLICIT);
    populateMenu();
    addMenuCommands();
    menu.setCommandListener(this);
    navigator.forward(menu);
  }

  private void populateMenu() {
    MenuItem[] items = menuManager.getSortedMenuItems();
    for (int i = 0; i < items.length; i++) {
      menu.append(Lang.tr(items[i].key), (Image) iconMap.get(items[i].key));
    }
  }

  private void addMenuCommands() {
    menu.addCommand(Commands.exit());
    menu.addCommand(Commands.Menu.sort());
    menu.addCommand(Commands.Menu.visibility());
    menu.addCommand(Commands.Player.nowPlaying());
  }

  public void refreshMenu() {
    if (navigator == null || menu == null) {
      return;
    }
    int index = menu.getSelectedIndex();
    menu.deleteAll();
    if (isVisibilityMode) {
      populateVisibilityMenu();
    } else if (isSortMode) {
      populateSortMenu();
    } else {
      populateMenu();
    }
    int newSize = menu.size();
    if (newSize > 0) {
      if (index < 0) {
        index = 0;
      }
      if (index >= newSize) {
        index = newSize - 1;
      }
      menu.setSelectedIndex(index, true);
    }
  }

  private void populateVisibilityMenu() {
    MenuItem[] allItems = menuManager.getAllMenuItems();
    for (int i = 0; i < allItems.length; i++) {
      Boolean tempEnabled = (Boolean) visibilityChanges.get(allItems[i].key);
      boolean currentEnabled =
          (tempEnabled != null) ? tempEnabled.booleanValue() : allItems[i].enabled;
      String prefix = currentEnabled ? Configuration.VISIBILITY_ICON : Configuration.HIDDEN_ICON;
      menu.append(prefix + Lang.tr(allItems[i].key), (Image) iconMap.get(allItems[i].key));
    }
  }

  private void populateSortMenu() {
    MenuItem[] items = menuManager.getSortedMenuItems();
    for (int i = 0; i < items.length; i++) {
      String displayText = Lang.tr(items[i].key);
      if (isSortMode && selectedItemIndex >= 0 && i == selectedItemIndex) {
        displayText = Configuration.SORT_ICON + displayText;
      }
      menu.append(displayText, (Image) iconMap.get(items[i].key));
    }
  }

  private void handleListSelection() {
    if (menu == null) {
      return;
    }
    int selectedIndex = menu.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= menu.size()) {
      return;
    }
    if (isSortMode) {
      handleSortSelection(selectedIndex);
    } else if (isVisibilityMode) {
      handleVisibilitySelection(selectedIndex);
    } else {
      executeMenuAction(selectedIndex);
    }
  }

  private void executeMenuAction(int selectedIndex) {
    String selectedDisplay = menu.getString(selectedIndex);
    MenuItem[] items = menuManager.getSortedMenuItems();
    for (int i = 0; i < items.length; i++) {
      if (Lang.tr(items[i].key).equals(selectedDisplay)) {
        menuManager.executeAction(items[i].key);
        break;
      }
    }
  }

  private void toggleSortMode() {
    if (!isSortMode) {
      startSortMode();
    } else {
      exitSortMode();
    }
    refreshMenu();
  }

  private void startSortMode() {
    if (menu == null) {
      return;
    }
    isSortMode = true;
    selectedItemIndex = -1;
    switchToEditCommands();
    navigator.showAlert(Lang.tr("settings.reorder.instructions"), AlertType.INFO);
  }

  private void exitSortMode() {
    if (menu == null) {
      return;
    }
    isSortMode = false;
    selectedItemIndex = -1;
    switchToNormalCommands();
  }

  private void handleSortSelection(int currentIndex) {
    if (selectedItemIndex == -1) {
      selectedItemIndex = currentIndex;
      String currentText = menu.getString(currentIndex);
      menu.set(currentIndex, Configuration.SORT_ICON + currentText, menu.getImage(currentIndex));
    } else {
      if (selectedItemIndex != currentIndex) {
        swapMenuItems(selectedItemIndex, currentIndex);
      } else {
        deselectSortItem(currentIndex);
      }
      selectedItemIndex = -1;
    }
  }

  private void swapMenuItems(int index1, int index2) {
    String item1 = menu.getString(index1);
    String item2 = menu.getString(index2);
    Image image1 = menu.getImage(index1);
    Image image2 = menu.getImage(index2);
    if (item1.startsWith(Configuration.SORT_ICON)) {
      item1 = item1.substring(Configuration.SORT_ICON.length());
    }
    menu.set(index1, item2, image2);
    menu.set(index2, item1, image1);
  }

  private void deselectSortItem(int index) {
    String currentText = menu.getString(index);
    if (currentText.startsWith(Configuration.SORT_ICON)) {
      menu.set(
          index, currentText.substring(Configuration.SORT_ICON.length()), menu.getImage(index));
    }
  }

  private void toggleVisibilityMode() {
    if (!isVisibilityMode) {
      startVisibilityMode();
    }
  }

  private void startVisibilityMode() {
    if (menu == null) {
      return;
    }
    isVisibilityMode = true;
    visibilityChanges.clear();
    switchToEditCommands();
    navigator.showAlert(Lang.tr("settings.visibility.instructions"), AlertType.INFO);
    refreshMenu();
  }

  private void exitVisibilityMode() {
    if (menu == null) {
      return;
    }
    isVisibilityMode = false;
    visibilityChanges.clear();
    switchToNormalCommands();
  }

  private void handleVisibilitySelection(int currentIndex) {
    String currentText = menu.getString(currentIndex);
    Image currentImage = menu.getImage(currentIndex);
    String displayText;
    boolean newEnabled;
    if (currentText.startsWith(Configuration.VISIBILITY_ICON)) {
      displayText = currentText.substring(Configuration.VISIBILITY_ICON.length());
      newEnabled = false;
      menu.set(currentIndex, Configuration.HIDDEN_ICON + displayText, currentImage);
    } else if (currentText.startsWith(Configuration.HIDDEN_ICON)) {
      displayText = currentText.substring(Configuration.HIDDEN_ICON.length());
      newEnabled = true;
      menu.set(currentIndex, Configuration.VISIBILITY_ICON + displayText, currentImage);
    } else {
      return;
    }
    updateVisibilityChange(displayText, newEnabled);
  }

  private void updateVisibilityChange(String displayText, boolean enabled) {
    MenuItem[] allItems = menuManager.getAllMenuItems();
    for (int i = 0; i < allItems.length; i++) {
      if (Lang.tr(allItems[i].key).equals(displayText)) {
        visibilityChanges.put(allItems[i].key, enabled ? Boolean.TRUE : Boolean.FALSE);
        break;
      }
    }
  }

  private void switchToEditCommands() {
    menu.removeCommand(Commands.exit());
    menu.removeCommand(Commands.Menu.sort());
    menu.removeCommand(Commands.Menu.visibility());
    menu.removeCommand(Commands.Player.nowPlaying());
    menu.addCommand(Commands.Form.save());
    menu.addCommand(Commands.Form.cancel());
  }

  private void switchToNormalCommands() {
    menu.removeCommand(Commands.Form.save());
    menu.removeCommand(Commands.Form.cancel());
    menu.addCommand(Commands.exit());
    menu.addCommand(Commands.Menu.sort());
    menu.addCommand(Commands.Menu.visibility());
    menu.addCommand(Commands.Player.nowPlaying());
  }

  private void saveCurrentOrder() {
    if (menu == null) {
      return;
    }
    MenuItem[] originalItems = menuManager.getSortedMenuItems();
    for (int i = 0; i < menu.size(); i++) {
      String displayText = menu.getString(i);
      if (displayText.startsWith(Configuration.SORT_ICON)) {
        displayText = displayText.substring(Configuration.SORT_ICON.length());
      }
      for (int j = 0; j < originalItems.length; j++) {
        if (Lang.tr(originalItems[j].key).equals(displayText)) {
          menuManager.updateItemOrder(originalItems[j].key, i + 1);
          break;
        }
      }
    }
    navigator.showAlert(Lang.tr("settings.reorder.saved"), AlertType.CONFIRMATION);
  }

  private void saveCurrentVisibility() {
    Enumeration keys = visibilityChanges.keys();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      Boolean enabled = (Boolean) visibilityChanges.get(key);
      menuManager.setItemEnabled(key, enabled.booleanValue());
    }
    navigator.showAlert(Lang.tr("settings.visibility.saved"), AlertType.CONFIRMATION);
  }

  private void goToSearchScreen() {
    SearchScreen searchScreen = new SearchScreen(navigator);
    navigator.forward(searchScreen);
  }

  private void goToDiscoverPlaylistsScreen() {
    navigator.showLoadingAlert(Lang.tr("app.loading"));
    MIDPlay.startOperation(
        new GetHotPlaylistOperation(
            new GetHotPlaylistOperation.Listener() {
              public void onDataReceived(Playlists items) {
                PlaylistListScreen playlistScreen =
                    new PlaylistListScreen(Lang.tr("menu.hot_playlists"), items, navigator);
                navigator.forward(playlistScreen);
              }

              public void onNoDataReceived() {
                navigator.showAlert(Lang.tr("status.no_data"), AlertType.INFO);
              }

              public void onError(Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            }));
  }

  private void goToAboutScreen() {
    Form f = new Form(Lang.tr("app.about"));
    f.append("Application: " + getAppProperty("MIDlet-Name") + "\n");
    f.append("Version: " + APP_VERSION + "\n");
    f.append("Developer: " + getAppProperty("MIDlet-Vendor") + "\n");
    f.addCommand(Commands.back());
    f.addCommand(Commands.checkUpdate());
    f.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.back()) {
              navigator.back();
            } else if (c == Commands.checkUpdate()) {
              manualCheckForUpdate();
            }
          }
        });
    navigator.forward(f);
  }

  private void goToSettingsScreen() {
    SettingsScreen settingsScreen =
        new SettingsScreen(
            navigator,
            new SettingsScreen.Listener() {
              public void onLanguageChanged(String selectedLang) {
                int index = menu.getSelectedIndex();
                if (playerScreen != null) {
                  playerScreen.clearCommands();
                  Commands.refresh();
                  playerScreen.addCommands();
                } else {
                  Commands.refresh();
                }
                navigator.clear();
                createMenu();
                int newSize = menu.size();
                if (newSize > 0) {
                  if (index < 0) {
                    index = 0;
                  }
                  if (index >= newSize) {
                    index = newSize - 1;
                  }
                  menu.setSelectedIndex(index, true);
                }
              }

              public void onSettingsSaved() {
                navigator.showAlert(Lang.tr("settings.status.saved"), AlertType.CONFIRMATION, menu);
              }
            });
    navigator.forward(settingsScreen);
  }

  private void goToFavoritesScreen() {
    FavoritesScreen favoritesScreen = new FavoritesScreen(navigator);
    navigator.forward(favoritesScreen);
  }

  private void showExitConfirmation() {
    navigator.showConfirmationAlert(
        Lang.tr("confirm.exit"),
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              notifyDestroyed();
            } else if (c == Commands.cancel()) {
              navigator.dismissAlert();
            }
          }
        });
  }

  private void showError(String message) {
    navigator.showAlert(message, AlertType.ERROR);
  }

  private void checkForUpdate() {
    if (settingsManager.getCurrentAutoUpdate() == Configuration.AutoUpdate.ENABLED) {
      performUpdateCheck(false);
    }
  }

  private void manualCheckForUpdate() {
    performUpdateCheck(true);
  }

  private void performUpdateCheck(final boolean isManual) {
    if (isManual) {
      navigator.showLoadingAlert(Lang.tr("app.loading"));
    }
    MIDPlay.startOperation(
        new CheckUpdateOperation(
            new CheckUpdateOperation.Listener() {
              public void onUpdateAvailable(String updateUrl) {
                if (isManual) {
                  navigator.dismissAlert();
                }
                showUpdateDialog(updateUrl);
              }

              public void onNoUpdateAvailable() {
                if (isManual) {
                  navigator.showAlert(Lang.tr("status.no_updates"), AlertType.CONFIRMATION);
                }
              }

              public void onError(Exception e) {
                showError(e.toString());
              }
            }));
  }

  private void showUpdateDialog(final String updateUrl) {
    navigator.showConfirmationAlert(
        Lang.tr("status.update_available"),
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              try {
                boolean success = platformRequest(updateUrl);
                if (!success) {
                  navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
                }
              } catch (ConnectionNotFoundException e) {
                navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
              }
            } else if (c == Commands.cancel()) {
              navigator.dismissAlert();
            }
          }
        });
  }
}
