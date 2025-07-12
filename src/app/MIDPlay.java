package app;

import app.core.data.AsyncDataManager;
import app.core.network.ApiEndpoints;
import app.core.platform.Observer;
import app.core.settings.SettingsManager;
import app.models.Song;
import app.ui.FavoritesList;
import app.ui.MainList;
import app.ui.MainObserver;
import app.ui.PlaylistList;
import app.ui.SettingForm;
import app.ui.player.PlayerCanvas;
import app.utils.concurrent.ThreadManager;
import app.utils.image.ImageUtils;
import app.utils.text.LocalizationManager;
import app.utils.ui.UiUtils;
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

  private static MIDPlay instance;

  private static final int MAX_HISTORY_SIZE = 20;
  private static String appVersion = "";

  public static MIDPlay getInstance() {
    return instance;
  }

  public static String getAppVersion() {
    return appVersion;
  }

  private final Vector history = new Vector(MAX_HISTORY_SIZE);
  private Displayable currentDisplayable;

  private Command downloadCmd;
  private Command cancelCmd;

  public MIDPlay() {
    instance = this;
    initializeApplication();
    initializeCommands();
  }

  private void initializeCommands() {
    downloadCmd = new Command(LocalizationManager.tr("update"), Command.OK, 1);
    cancelCmd = new Command(LocalizationManager.tr("cancel"), Command.CANCEL, 2);
  }

  private void initializeApplication() {
    try {
      LocalizationManager.initialize(this);
      SettingForm.populateFormWithSettings();
      appVersion = getAppProperty("MIDlet-Version");
    } catch (Exception e) {
      showErrorAlert("Init Error", "Failed to initialize");
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
      history.removeAllElements();

      Displayable current = getCurrentDisplayable();
      if (current != null) {
        if (current instanceof PlayerCanvas) {
          ((PlayerCanvas) current).close();
        }
      }

      ImageUtils.clearImageCache();

      AsyncDataManager.getInstance().shutdown();

    } catch (Exception e) {
    }
  }

  public void commandAction(Command command, Displayable displayable) {}

  private void setMainScreen() {
    try {
      MainList mainMenu =
          UiUtils.createMainMenu(this, SettingsManager.getInstance().getCurrentService());
      go(mainMenu);
    } catch (Exception e) {
      showErrorAlert("Error", "Cannot create main screen");
    }
  }

  public void checkForUpdate() {
    checkForUpdate(true);
  }

  public void checkForUpdate(boolean respectAutoUpdateSetting) {
    try {
      if (respectAutoUpdateSetting && !SettingsManager.getInstance().isAutoUpdate()) {
        return;
      }

      checkForUpdateAsync();
    } catch (Exception e) {
    }
  }

  private void checkForUpdateAsync() {
    try {
      final String updateUrl = ApiEndpoints.checkForUpdate();
      if (updateUrl != null) {
        AsyncDataManager.getInstance()
            .getAsync(
                updateUrl,
                new AsyncDataManager.NetworkCallback() {
                  public void onSuccess(String result) {
                    handleUpdateResponse(result);
                  }

                  public void onError(Exception error) {}

                  public void onCancelled() {}
                });
      }
    } catch (Exception e) {
    }
  }

  private void handleUpdateResponse(final String response) {
    if (response == null || "".equals(response)) {

      return;
    }

    if (response.startsWith("http://") || response.startsWith("https://")) {

      ThreadManager.runOnUiThread(
          new Runnable() {
            public void run() {
              showUpdateDialog(response);
            }
          });
    }
  }

  private void showUpdateDialog(final String updateInfo) {
    Alert updateAlert =
        new Alert(null, LocalizationManager.tr("update_message"), null, AlertType.INFO);
    updateAlert.setTimeout(Alert.FOREVER);
    updateAlert.addCommand(downloadCmd);
    updateAlert.addCommand(cancelCmd);
    updateAlert.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            handleUpdateCommand(c, updateInfo);
          }
        });
    getDisplay().setCurrent(updateAlert);
  }

  private void handleUpdateCommand(Command command, String updateUrl) {
    if (command == downloadCmd) {
      try {
        boolean success = platformRequest(updateUrl);
        if (success) {
          exit();
        }
      } catch (Exception e) {
        showErrorAlert("Error", "Cannot open download link");
      }
    } else if (command == cancelCmd) {
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
    return !history.contains(current);
  }

  private void addToHistory(Displayable displayable) {
    if (history.size() >= MAX_HISTORY_SIZE) {
      history.removeElementAt(0);
    }
    history.addElement(displayable);
  }

  public Displayable goBack() {
    if (history.isEmpty()) {
      exit();
      return null;
    }
    Displayable previous = null;
    while (!history.isEmpty()) {
      previous = (Displayable) history.lastElement();
      history.removeElementAt(history.size() - 1);
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
    history.removeAllElements();
  }

  public Display getDisplay() {
    return Display.getDisplay(this);
  }

  public void showErrorAlert(String title, String message) {
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
    return history.size();
  }

  public boolean isHistoryEmpty() {
    return history.isEmpty();
  }
}
