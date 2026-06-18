package midplay.ui;

import java.util.Hashtable;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import midplay.model.MenuItem;
import midplay.store.Configuration;
import midplay.util.Lang;
import midplay.util.Utils;

// The root menu screen, split out of the MIDlet (MIDPlay). Owns the whole menu
// feature: the List itself, its command dispatch, and the sort/visibility
// edit-mode controller (which manipulates the List directly, so it lives with
// the view). Menu-item navigation is decoupled via MenuManager.executeAction
// (the host registers the goTo* runnables); only the toolbar commands that need
// the MIDlet/other screens (exit, check-for-update, open now-playing) go back
// through the MenuHost callback.
public final class MainMenuScreen extends List implements CommandListener {
  /** Callbacks the menu needs from its host (the MIDlet). */
  public interface MenuHost {
    void exitApp();

    void checkForUpdate();

    void openNowPlaying();
  }

  private final Navigator navigator;
  private final MenuManager menuManager;
  private final MenuHost host;
  private final Hashtable iconMap = new Hashtable();
  private final Hashtable visibilityChanges = new Hashtable();
  private boolean isSortMode = false;
  private boolean isVisibilityMode = false;
  private int selectedItemIndex = -1;

  public MainMenuScreen(Navigator navigator, MenuManager menuManager, MenuHost host) {
    super(Lang.tr("app.name"), List.IMPLICIT);
    this.navigator = navigator;
    this.menuManager = menuManager;
    this.host = host;
    buildIconMap();
    populateMenu();
    addMenuCommands();
    setCommandListener(this);
  }

  private void buildIconMap() {
    iconMap.put(Configuration.MENU_SEARCH, Configuration.searchIcon);
    iconMap.put(Configuration.MENU_FAVORITES, Configuration.favoriteIcon);
    iconMap.put(Configuration.MENU_DISCOVER_PLAYLISTS, Configuration.playlistIcon);
    iconMap.put(Configuration.MENU_SETTINGS, Configuration.settingsIcon);
    iconMap.put(Configuration.MENU_ABOUT, Configuration.infoIcon);
  }

  public void commandAction(Command c, Displayable d) {
    try {
      if (c == List.SELECT_COMMAND) {
        handleListSelection();
      } else if (c == Commands.exit()) {
        host.exitApp();
      } else if (c == Commands.checkUpdate()) {
        host.checkForUpdate();
      } else if (c == Commands.menuSort()) {
        toggleSortMode();
      } else if (c == Commands.menuVisibility()) {
        toggleVisibilityMode();
      } else if (c == Commands.formSave()) {
        handleSave();
      } else if (c == Commands.formCancel()) {
        handleCancel();
      } else if (c == Commands.playerNowPlaying()) {
        host.openNowPlaying();
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  // Rebuild the list contents after a language or menu-config change, keeping
  // the selection where possible.
  public void refreshMenu() {
    int index = getSelectedIndex();
    deleteAll();
    if (isVisibilityMode) {
      populateVisibilityMenu();
    } else if (isSortMode) {
      populateSortMenu();
    } else {
      populateMenu();
    }
    Utils.clampAndSelect(this, index);
  }

  private void populateMenu() {
    MenuItem[] items = menuManager.getSortedMenuItems();
    for (int i = 0; i < items.length; i++) {
      append(Lang.tr(items[i].key), (Image) iconMap.get(items[i].key));
    }
  }

  private void addMenuCommands() {
    addCommand(Commands.exit());
    addCommand(Commands.checkUpdate());
    addCommand(Commands.menuSort());
    addCommand(Commands.menuVisibility());
    addCommand(Commands.playerNowPlaying());
  }

  private void populateVisibilityMenu() {
    MenuItem[] allItems = menuManager.getAllMenuItems();
    for (int i = 0; i < allItems.length; i++) {
      Boolean tempEnabled = (Boolean) visibilityChanges.get(allItems[i].key);
      boolean currentEnabled =
          (tempEnabled != null) ? tempEnabled.booleanValue() : allItems[i].enabled;
      String prefix = currentEnabled ? Configuration.VISIBILITY_ICON : Configuration.HIDDEN_ICON;
      append(prefix + Lang.tr(allItems[i].key), (Image) iconMap.get(allItems[i].key));
    }
  }

  private void populateSortMenu() {
    MenuItem[] items = menuManager.getSortedMenuItems();
    for (int i = 0; i < items.length; i++) {
      String displayText = Lang.tr(items[i].key);
      if (isSortMode && selectedItemIndex >= 0 && i == selectedItemIndex) {
        displayText = Configuration.SORT_ICON + displayText;
      }
      append(displayText, (Image) iconMap.get(items[i].key));
    }
  }

  private void handleListSelection() {
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= size()) {
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
    // populateMenu appends getSortedMenuItems() in order, so the selected row
    // index maps directly to that array — no display-string reverse lookup.
    MenuItem[] items = menuManager.getSortedMenuItems();
    if (selectedIndex >= 0 && selectedIndex < items.length) {
      menuManager.executeAction(items[selectedIndex].key);
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

  private void toggleSortMode() {
    if (!isSortMode) {
      startSortMode();
    } else {
      exitSortMode();
    }
    refreshMenu();
  }

  private void startSortMode() {
    isSortMode = true;
    selectedItemIndex = -1;
    switchToEditCommands();
    navigator.showAlert(Lang.tr("settings.reorder.instructions"), AlertType.INFO);
  }

  private void exitSortMode() {
    isSortMode = false;
    selectedItemIndex = -1;
    switchToNormalCommands();
  }

  private void handleSortSelection(int currentIndex) {
    if (selectedItemIndex == -1) {
      selectedItemIndex = currentIndex;
      String currentText = getString(currentIndex);
      set(currentIndex, Configuration.SORT_ICON + currentText, getImage(currentIndex));
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
    String item1 = getString(index1);
    String item2 = getString(index2);
    Image image1 = getImage(index1);
    Image image2 = getImage(index2);
    if (item1.startsWith(Configuration.SORT_ICON)) {
      item1 = item1.substring(Configuration.SORT_ICON.length());
    }
    set(index1, item2, image2);
    set(index2, item1, image1);
  }

  private void deselectSortItem(int index) {
    String currentText = getString(index);
    if (currentText.startsWith(Configuration.SORT_ICON)) {
      set(index, currentText.substring(Configuration.SORT_ICON.length()), getImage(index));
    }
  }

  private void toggleVisibilityMode() {
    if (!isVisibilityMode) {
      startVisibilityMode();
    }
  }

  private void startVisibilityMode() {
    isVisibilityMode = true;
    visibilityChanges.clear();
    switchToEditCommands();
    navigator.showAlert(Lang.tr("settings.visibility.instructions"), AlertType.INFO);
    refreshMenu();
  }

  private void exitVisibilityMode() {
    isVisibilityMode = false;
    visibilityChanges.clear();
    switchToNormalCommands();
  }

  private void handleVisibilitySelection(int currentIndex) {
    String currentText = getString(currentIndex);
    Image currentImage = getImage(currentIndex);
    String displayText;
    boolean newEnabled;
    if (currentText.startsWith(Configuration.VISIBILITY_ICON)) {
      displayText = currentText.substring(Configuration.VISIBILITY_ICON.length());
      newEnabled = false;
      set(currentIndex, Configuration.HIDDEN_ICON + displayText, currentImage);
    } else if (currentText.startsWith(Configuration.HIDDEN_ICON)) {
      displayText = currentText.substring(Configuration.HIDDEN_ICON.length());
      newEnabled = true;
      set(currentIndex, Configuration.VISIBILITY_ICON + displayText, currentImage);
    } else {
      return;
    }
    updateVisibilityChange(currentIndex, newEnabled);
  }

  private void updateVisibilityChange(int selectedIndex, boolean enabled) {
    // populateVisibilityMenu appends getAllMenuItems() in order, so the row
    // index maps directly to that array — no display-string reverse lookup.
    MenuItem[] allItems = menuManager.getAllMenuItems();
    if (selectedIndex >= 0 && selectedIndex < allItems.length) {
      visibilityChanges.put(allItems[selectedIndex].key, enabled ? Boolean.TRUE : Boolean.FALSE);
    }
  }

  private void switchToEditCommands() {
    removeCommand(Commands.exit());
    removeCommand(Commands.checkUpdate());
    removeCommand(Commands.menuSort());
    removeCommand(Commands.menuVisibility());
    removeCommand(Commands.playerNowPlaying());
    addCommand(Commands.formSave());
    addCommand(Commands.formCancel());
  }

  private void switchToNormalCommands() {
    removeCommand(Commands.formSave());
    removeCommand(Commands.formCancel());
    addMenuCommands();
  }

  private void saveCurrentOrder() {
    MenuItem[] originalItems = menuManager.getSortedMenuItems();
    for (int i = 0; i < size(); i++) {
      String displayText = getString(i);
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
    java.util.Enumeration keys = visibilityChanges.keys();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      Boolean enabled = (Boolean) visibilityChanges.get(key);
      menuManager.setItemEnabled(key, enabled.booleanValue());
    }
    navigator.showAlert(Lang.tr("settings.visibility.saved"), AlertType.CONFIRMATION);
  }
}
