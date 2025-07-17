package app;

import app.core.data.DataLoader;
import app.core.data.DataParser;
import app.core.data.LoadDataListener;
import app.core.platform.Observer;
import app.core.settings.SettingsManager;
import app.core.threading.ThreadManager;
import app.core.threading.ThreadManagerIntegration;
import app.models.Song;
import app.ui.FavoritesList;
import app.ui.MainList;
import app.ui.MainObserver;
import app.ui.MenuFactory;
import app.ui.PlaylistList;
import app.ui.SettingForm;
import app.utils.I18N;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class MIDPlay extends MIDlet implements CommandListener, MainObserver {

  private static final String EMPTY_STRING = "";
  private static final int MAX_HISTORY_SIZE = 10;

  private static MIDPlay instance;
  private static String appVersion = "";

  private final Vector navigationHistory = new Vector(MAX_HISTORY_SIZE);
  private Displayable currentDisplayable;

  private Command downloadUpdateCommand;
  private Command cancelUpdateCommand;

  public static MIDPlay getInstance() {
    return instance;
  }

  public static String getAppVersion() {
    return appVersion;
  }

  public MIDPlay() {
    instance = this;
    initializeApplication();
    initializeCommands();
  }

  private void initializeCommands() {
    downloadUpdateCommand = new Command(I18N.tr("update"), Command.OK, 1);
    cancelUpdateCommand = new Command(I18N.tr("cancel"), Command.CANCEL, 2);
  }

  private void initializeApplication() {
    try {
      I18N.initialize(this);
      SettingForm.populateFormWithSettings();
      appVersion = getAppProperty("MIDlet-Version");
    } catch (Exception e) {
      showErrorAlert(e.toString());
    }
  }

  public void startApp() {
    setMainScreen();
    checkForUpdate();
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    try {
      cleanup();
    } catch (Exception e) {
      if (!unconditional) {
        throw new MIDletStateChangeException();
      }
    }
  }

  private void cleanup() {
    try {
      navigationHistory.removeAllElements();

      try {
        SettingsManager.getInstance().shutdown();
      } catch (Exception e) {
      }

      try {
        ThreadManagerIntegration.shutdownAllPools();
        ThreadManager.getInstance().shutdown();
      } catch (Exception e) {
      }
    } catch (Exception e) {
    }
  }

  public void commandAction(Command command, Displayable displayable) {}

  private void setMainScreen() {
    try {
      MainList mainMenu =
          MenuFactory.createMainMenu(this, SettingsManager.getInstance().getCurrentService());
      go(mainMenu);
    } catch (Exception e) {
      showErrorAlert(e.toString());
    }
  }

  public void checkForUpdate() {
    checkForUpdate(true);
  }

  public void checkForUpdate(final boolean respectAutoUpdateSetting) {
    if (respectAutoUpdateSetting && !SettingsManager.getInstance().isAutoUpdateEnabled()) {
      return;
    }

    ThreadManagerIntegration.loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            String updateInfo = DataParser.checkForUpdate();
            Vector result = new Vector();
            result.addElement(updateInfo != null ? updateInfo : EMPTY_STRING);
            return result;
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            if (data != null && data.size() > 0) {
              String updateInfo = (String) data.elementAt(0);
              if (updateInfo != null && !EMPTY_STRING.equals(updateInfo)) {
                showUpdateDialog(updateInfo);
              }
            }
          }

          public void loadError() {}

          public void noData() {}
        });
  }

  private void showUpdateDialog(final String updateInfo) {
    Alert updateAlert = new Alert(null, I18N.tr("update_message"), null, AlertType.INFO);
    updateAlert.setTimeout(Alert.FOREVER);
    updateAlert.addCommand(downloadUpdateCommand);
    updateAlert.addCommand(cancelUpdateCommand);
    updateAlert.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            handleUpdateCommand(c, updateInfo);
          }
        });
    getDisplay().setCurrent(updateAlert);
  }

  private void handleUpdateCommand(Command command, String updateUrl) {
    if (command == downloadUpdateCommand) {
      try {
        boolean success = platformRequest(updateUrl);
        if (success) {
          exit();
        }
      } catch (Exception e) {
        showErrorAlert(e.toString());
      }
    } else if (command == cancelUpdateCommand) {
      Displayable current = getCurrentDisplayable();
      if (current != null) {
        getDisplay().setCurrent(current);
      } else {
        setMainScreen();
      }
    }
  }

  public Displayable go(Displayable displayable) {
    if (displayable == null) {
      return getCurrentDisplayable();
    }
    Displayable current = getCurrentDisplayable();
    if (shouldAddToHistory(current)) {
      addToHistory(current);
    }
    return replaceCurrent(displayable);
  }

  private boolean shouldAddToHistory(Displayable current) {
    if (current == null) {
      return false;
    }
    if (current instanceof Observer) {
      return false;
    }
    return !navigationHistory.contains(current);
  }

  private void addToHistory(Displayable displayable) {
    if (navigationHistory.size() >= MAX_HISTORY_SIZE) {
      navigationHistory.removeElementAt(0);
    }
    navigationHistory.addElement(displayable);
  }

  public Displayable goBack() {
    if (navigationHistory.isEmpty()) {
      exit();
      return null;
    }
    Displayable previous = null;
    while (!navigationHistory.isEmpty()) {
      previous = (Displayable) navigationHistory.lastElement();
      navigationHistory.removeElementAt(navigationHistory.size() - 1);
      if (!isSameScreenType(previous, getCurrentDisplayable())) {
        break;
      }
    }
    if (previous == null) {
      exit();
      return null;
    }

    if (previous instanceof PlaylistList) {
      PlaylistList playlistList = (PlaylistList) previous;
      playlistList.resumeImageLoading();
      playlistList.startSelectionMonitoring();
    } else if (previous instanceof FavoritesList) {
      FavoritesList favoritesList = (FavoritesList) previous;
      favoritesList.resumeImageLoading();
      favoritesList.startSelectionMonitoring();
    }

    return replaceCurrent(previous);
  }

  private boolean isSameScreenType(Displayable d1, Displayable d2) {
    if (d1 == null || d2 == null) {
      return false;
    }
    String class1 = d1.getClass().toString();
    String class2 = d2.getClass().toString();
    return class1.equals(class2);
  }

  public void handle(Song song) {
    throw new RuntimeException("Not implemented");
  }

  public Displayable replaceCurrent(Displayable displayable) {
    if (displayable == null) {
      return getCurrentDisplayable();
    }
    getDisplay().setCurrent(displayable);
    if (!(displayable instanceof Alert)) {
      this.currentDisplayable = displayable;
    }
    return displayable;
  }

  public Displayable getCurrentDisplayable() {
    return this.currentDisplayable;
  }

  public void exit() {
    try {
      destroyApp(false);
    } catch (MIDletStateChangeException e) {
    }
    notifyDestroyed();
  }

  public void clearHistory() {
    navigationHistory.removeAllElements();
  }

  public Display getDisplay() {
    return Display.getDisplay(this);
  }

  public void showErrorAlert(String message) {
    Alert alert = new Alert(null, message, null, AlertType.ERROR);
    alert.setTimeout(2000);
    Displayable current = getCurrentDisplayable();
    if (current != null) {
      getDisplay().setCurrent(alert, current);
    } else {
      getDisplay().setCurrent(alert);
    }
  }

  public int getHistorySize() {
    return navigationHistory.size();
  }

  public boolean isHistoryEmpty() {
    return navigationHistory.isEmpty();
  }
}
