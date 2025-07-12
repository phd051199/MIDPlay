package app.ui;

import app.MIDPlay;
import app.core.settings.SettingsManager;
import app.models.Song;
import app.utils.text.LocalizationManager;
import app.utils.ui.UiUtils;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

public class SettingForm extends Form implements MainObserver, CommandListener {

  private static final SettingsManager settingManager = SettingsManager.getInstance();
  private static ChoiceGroup languageChoice;
  private static ChoiceGroup audioQualityChoice;
  private static ChoiceGroup serviceChoice;
  private static ChoiceGroup autoUpdateChoice;
  private static ChoiceGroup performanceChoice;
  private static TextField themeColorField;
  private static TextField backgroundColorField;

  public static void populateFormWithSettings() {
    try {
      String[] settings = settingManager.getAllCurrentSettings();

      String savedLanguage = settings[0];
      LocalizationManager.setLanguage(savedLanguage);

      String[] languages = LocalizationManager.getLanguages();
      if (languageChoice != null) {
        for (int i = 0; i < languages.length; i++) {
          if (languages[i].equals(savedLanguage)) {
            languageChoice.setSelectedIndex(i, true);
            break;
          }
        }
      }

      String savedQuality = settings[1];
      String[] audioQualities = settingManager.getAudioQualities();
      if (audioQualityChoice != null) {
        for (int i = 0; i < audioQualities.length; i++) {
          if (audioQualities[i].equals(savedQuality)) {
            audioQualityChoice.setSelectedIndex(i, true);
            break;
          }
        }
      }

      if (settings.length > 2) {
        String savedService = settings[2];
        String[] services = settingManager.getAvailableServices();
        if (serviceChoice != null) {
          for (int i = 0; i < services.length; i++) {
            if (services[i].equals(savedService)) {
              serviceChoice.setSelectedIndex(i, true);
              break;
            }
          }
        }
      }

      if (settings.length > 3) {
        boolean autoUpdate = "true".equals(settings[3]);
        if (autoUpdateChoice != null) {
          autoUpdateChoice.setSelectedIndex(0, autoUpdate);
        }
      }

      if (settings.length > 4) {
        boolean loadPlaylistArt = "true".equals(settings[4]);
        if (performanceChoice != null) {
          performanceChoice.setSelectedIndex(0, loadPlaylistArt);
        }
      }

      if (settings.length > 5) {
        String themeColor = settings[5];
        if (themeColorField != null) {
          themeColorField.setString(themeColor);
        }
      }

      if (settings.length > 6) {
        String backgroundColor = settings[6];
        if (backgroundColorField != null) {
          backgroundColorField.setString(backgroundColor);
        }
      }
    } catch (Exception e) {
      setDefaultSettings();
    }
  }

  private static void setDefaultSettings() {
    try {

      String defaultLanguage = LocalizationManager.getLanguage();
      String[] languages = LocalizationManager.getLanguages();
      if (languageChoice != null) {
        for (int i = 0; i < languages.length; i++) {
          if (languages[i].equals(defaultLanguage)) {
            languageChoice.setSelectedIndex(i, true);
            break;
          }
        }
      }

      if (audioQualityChoice != null) {
        audioQualityChoice.setSelectedIndex(0, true);
      }
      if (serviceChoice != null) {
        serviceChoice.setSelectedIndex(0, true);
      }
      if (autoUpdateChoice != null) {
        autoUpdateChoice.setSelectedIndex(0, true);
      }
      if (performanceChoice != null) {
        performanceChoice.setSelectedIndex(0, true);
      }
      if (themeColorField != null) {
        themeColorField.setString("410A4A");
      }
      if (backgroundColorField != null) {
        backgroundColorField.setString("F0F0F0");
      }
    } catch (Exception e) {
    }
  }

  private Command backCommand;
  private Command saveCommand;
  private final MainObserver parent;

  public SettingForm(String title, MainObserver parent) {
    super(title);
    this.parent = parent;
    initUI();

    populateFormWithSettings();
  }

  private void initUI() {
    this.backCommand = new Command(LocalizationManager.tr("back"), Command.BACK, 1);
    this.saveCommand = new Command(LocalizationManager.tr("save"), Command.SCREEN, 2);

    languageChoice = new ChoiceGroup(LocalizationManager.tr("language"), Choice.POPUP);
    String[] languages = LocalizationManager.getLanguages();
    for (int i = 0; i < languages.length; i++) {
      languageChoice.append(LocalizationManager.getLanguageName(languages[i]), null);
    }

    audioQualityChoice = new ChoiceGroup(LocalizationManager.tr("audio_quality"), Choice.POPUP);
    String[] audioQualities = settingManager.getAudioQualities();
    for (int i = 0; i < audioQualities.length; i++) {
      audioQualityChoice.append(audioQualities[i], null);
    }

    serviceChoice = new ChoiceGroup(LocalizationManager.tr("service"), Choice.POPUP);
    String[] services = settingManager.getAvailableServices();
    for (int i = 0; i < services.length; i++) {
      serviceChoice.append(services[i], null);
    }

    autoUpdateChoice = new ChoiceGroup(LocalizationManager.tr("auto_update"), Choice.MULTIPLE);
    autoUpdateChoice.append(LocalizationManager.tr("check_for_update"), null);

    performanceChoice = new ChoiceGroup(LocalizationManager.tr("performance"), Choice.MULTIPLE);
    performanceChoice.append(LocalizationManager.tr("load_playlist_art"), null);

    themeColorField =
        new TextField(
            LocalizationManager.tr("theme_color"),
            settingManager.getThemeColor(),
            6,
            TextField.ANY);
    backgroundColorField =
        new TextField(
            LocalizationManager.tr("background_color"),
            settingManager.getBackgroundColor(),
            6,
            TextField.ANY);

    append(languageChoice);
    append(audioQualityChoice);
    append(serviceChoice);
    append(autoUpdateChoice);
    append(performanceChoice);
    append(themeColorField);
    append(backgroundColorField);

    addCommand(backCommand);
    addCommand(saveCommand);
    setCommandListener(this);
  }

  private void saveSettings() {
    try {
      String selectedLanguage = getSelectedLanguage();
      String currentLanguage = settingManager.getCurrentLanguage();
      String currentService = settingManager.getCurrentService();
      String themeColor = getThemeColor();
      String backgroundColor = getBackgroundColor();

      if (!isValidHexColor(themeColor)) {
        showAlert(LocalizationManager.tr("color_format_description"), AlertType.ERROR);
        return;
      }

      if (!isValidHexColor(backgroundColor)) {
        showAlert(LocalizationManager.tr("color_format_description"), AlertType.ERROR);
        return;
      }

      boolean languageChanged = !selectedLanguage.equals(currentLanguage);
      boolean serviceChanged = !getSelectedService().equals(currentService);

      boolean previousAutoUpdate = settingManager.isAutoUpdateEnabled();
      boolean currentAutoUpdate = isAutoUpdateEnabled();
      boolean shouldCheckUpdateNow = !previousAutoUpdate && currentAutoUpdate;

      settingManager.saveSettings(
          selectedLanguage,
          getSelectedAudioQuality(),
          getSelectedService(),
          isAutoUpdateEnabled() ? "true" : "false",
          isLoadPlaylistArtEnabled() ? "true" : "false",
          themeColor,
          backgroundColor);

      if (languageChanged) {
        LocalizationManager.setLanguage(selectedLanguage);
      }

      if (shouldCheckUpdateNow && parent instanceof MIDPlay) {
        ((MIDPlay) parent).checkForUpdate(false);
      }

      if (languageChanged || serviceChanged) {
        MainList mainList = UiUtils.createMainMenu(parent, getSelectedService());
        if (parent instanceof MIDPlay) {
          ((MIDPlay) parent).clearHistory();
        }
        parent.replaceCurrent(mainList);
      } else {
        parent.goBack();
      }
    } catch (Exception e) {
    }
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    if (type == AlertType.ERROR) {
      alert.setTimeout(Alert.FOREVER);
    } else {
      alert.setTimeout(2000);
    }
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  private boolean isValidHexColor(String color) {
    if (color == null || color.trim().length() == 0) {
      return true;
    }

    if (color.charAt(0) == '#') {
      color = color.substring(1);
    }

    if (color.length() != 6) {
      return false;
    }

    try {
      Integer.parseInt(color, 16);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (d == this) {
      if (c == backCommand) {
        goBack();
      } else if (c == saveCommand) {
        saveSettings();
      }
    }
  }

  public String getSelectedLanguage() {
    String[] languages = LocalizationManager.getLanguages();
    int index = languageChoice.getSelectedIndex();
    if (index >= 0 && index < languages.length) {
      return languages[index];
    }
    return languages[0];
  }

  public String getSelectedAudioQuality() {
    String[] audioQualities = settingManager.getAudioQualities();
    return audioQualities[audioQualityChoice.getSelectedIndex()];
  }

  public String getSelectedService() {
    String[] services = settingManager.getAvailableServices();
    return services[serviceChoice.getSelectedIndex()];
  }

  public boolean isAutoUpdateEnabled() {
    return autoUpdateChoice.isSelected(0);
  }

  public boolean isLoadPlaylistArtEnabled() {
    return performanceChoice.isSelected(0);
  }

  public String getThemeColor() {
    String color = themeColorField.getString();

    if (color == null || color.trim().length() == 0) {
      return "410A4A";
    }

    if (color.charAt(0) == '#') {
      color = color.substring(1);
    }

    return color;
  }

  public String getBackgroundColor() {
    String color = backgroundColorField.getString();

    if (color == null || color.trim().length() == 0) {
      return "F0F0F0";
    }

    if (color.charAt(0) == '#') {
      color = color.substring(1);
    }

    return color;
  }

  public Displayable go(Displayable d) {
    return parent.go(d);
  }

  public Displayable goBack() {
    return parent.goBack();
  }

  public void handle(Song song) {}

  public Displayable replaceCurrent(Displayable d) {
    return parent.replaceCurrent(d);
  }

  public Displayable getCurrentDisplayable() {
    return parent.getCurrentDisplayable();
  }
}
