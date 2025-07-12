package app.ui;

import app.constants.MenuConstants;
import app.constants.Services;
import app.core.settings.SettingsManager;
import app.utils.text.LocalizationManager;
import app.utils.ui.UiUtils;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;

public class MenuVisibilityForm extends Form implements CommandListener, ItemStateListener {
  private final MainObserver observer;
  private final String service;
  private Command saveCommand;
  private Command cancelCommand;
  private ChoiceGroup menuItemsGroup;

  public MenuVisibilityForm(String title, MainObserver observer, String service) {
    super(title);
    this.observer = observer;
    this.service = service;

    this.initComponents();
    this.setCommandListener(this);
    this.setItemStateListener(this);
  }

  private void initComponents() {
    StringItem descriptionItem =
        new StringItem(null, LocalizationManager.tr("select_visible_items"));
    this.append(descriptionItem);

    menuItemsGroup = new ChoiceGroup(LocalizationManager.tr("menu_items"), ChoiceGroup.MULTIPLE);

    if (Services.NCT.equals(service)) {
      loadNCTMenuItems();
    } else {
      loadSoundcloudMenuItems();
    }

    this.append(menuItemsGroup);

    saveCommand = new Command(LocalizationManager.tr("save"), Command.OK, 1);
    cancelCommand = new Command(LocalizationManager.tr("cancel"), Command.BACK, 2);

    this.addCommand(saveCommand);
    this.addCommand(cancelCommand);
  }

  private void loadNCTMenuItems() {
    SettingsManager menuSettingsManager = SettingsManager.getInstance();
    boolean[] visibility =
        menuSettingsManager.getNctMenuVisibility(MenuConstants.MAIN_MENU_ITEMS_NCT.length);

    for (int i = 0; i < MenuConstants.MAIN_MENU_ITEMS_NCT.length; i++) {
      String itemLabel = LocalizationManager.tr(MenuConstants.MAIN_MENU_ITEMS_NCT[i]);
      menuItemsGroup.append(itemLabel, null);
      menuItemsGroup.setSelectedIndex(i, visibility[i]);
    }
  }

  private void loadSoundcloudMenuItems() {
    SettingsManager menuSettingsManager = SettingsManager.getInstance();
    boolean[] visibility =
        menuSettingsManager.getSoundcloudMenuVisibility(
            MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD.length);

    for (int i = 0; i < MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD.length; i++) {
      String itemLabel = LocalizationManager.tr(MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD[i]);
      menuItemsGroup.append(itemLabel, null);
      menuItemsGroup.setSelectedIndex(i, visibility[i]);
    }
  }

  private void saveVisibility() {
    SettingsManager menuSettingsManager = SettingsManager.getInstance();

    if (Services.NCT.equals(service)) {
      boolean[] visibility = new boolean[MenuConstants.MAIN_MENU_ITEMS_NCT.length];
      for (int i = 0; i < visibility.length; i++) {
        visibility[i] = menuItemsGroup.isSelected(i);
      }

      boolean anyVisible = false;
      for (int i = 0; i < visibility.length; i++) {
        if (visibility[i]) {
          anyVisible = true;
          break;
        }
      }

      if (!anyVisible) {
        showAlert(LocalizationManager.tr("visibility_at_least_one"), AlertType.ERROR);
        return;
      }

      menuSettingsManager.saveNctMenuVisibility(visibility);
    } else {
      boolean[] visibility = new boolean[MenuConstants.MAIN_MENU_ITEMS_SOUNDCLOUD.length];
      for (int i = 0; i < visibility.length; i++) {
        visibility[i] = menuItemsGroup.isSelected(i);
      }

      boolean anyVisible = false;
      for (int i = 0; i < visibility.length; i++) {
        if (visibility[i]) {
          anyVisible = true;
          break;
        }
      }

      if (!anyVisible) {
        showAlert(LocalizationManager.tr("visibility_at_least_one"), AlertType.ERROR);
        return;
      }

      menuSettingsManager.saveSoundcloudMenuVisibility(visibility);
    }

    MainList mainList = UiUtils.createMainMenu(observer, service);

    if (observer instanceof app.MIDPlay) {
      ((app.MIDPlay) observer).clearHistory();
    }

    observer.replaceCurrent(mainList);
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    if (type == AlertType.ERROR) {
      alert.setTimeout(Alert.FOREVER);
    } else {
      alert.setTimeout(2000);
    }
    Display.getDisplay(app.MIDPlay.getInstance()).setCurrent(alert, this);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == saveCommand) {
      saveVisibility();
    } else if (c == cancelCommand) {
      observer.goBack();
    }
  }

  public void itemStateChanged(Item item) {}
}
