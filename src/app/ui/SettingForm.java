package app.ui;

import app.MIDPlay;
import app.common.SettingManager;
import app.model.Song;
import app.utils.I18N;
import app.utils.Utils;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

public class SettingForm extends Form implements Utils.BreadCrumbTrail, CommandListener {

  private static final SettingManager settingManager = SettingManager.getInstance();
  private static ChoiceGroup languageChoice;
  private static ChoiceGroup audioQualityChoice;
  private static ChoiceGroup serviceChoice;

  public static void populateFormWithSettings() {
    try {
      String[] settings = settingManager.getAllCurrentSettings();

      String savedLanguage = settings[0];
      I18N.setLanguage(savedLanguage);

      String[] languages = I18N.getLanguages();
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
    } catch (Exception e) {
      setDefaultSettings();
    }
  }

  private static void setDefaultSettings() {
    try {

      String defaultLanguage = I18N.getLanguage();
      String[] languages = I18N.getLanguages();
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
    } catch (Exception e) {
    }
  }

  private Command backCommand;
  private Command saveCommand;
  private final Utils.BreadCrumbTrail parent;

  public SettingForm(String title, Utils.BreadCrumbTrail parent) {
    super(title);
    this.parent = parent;
    initUI();

    populateFormWithSettings();
  }

  private void initUI() {
    this.backCommand = new Command(I18N.tr("back"), Command.BACK, 1);
    this.saveCommand = new Command(I18N.tr("save"), Command.SCREEN, 2);

    languageChoice = new ChoiceGroup(I18N.tr("language"), Choice.EXCLUSIVE);
    String[] languages = I18N.getLanguages();
    for (int i = 0; i < languages.length; i++) {
      languageChoice.append(I18N.getLanguageName(languages[i]), null);
    }

    audioQualityChoice = new ChoiceGroup(I18N.tr("audio_quality"), Choice.EXCLUSIVE);
    String[] audioQualities = settingManager.getAudioQualities();
    for (int i = 0; i < audioQualities.length; i++) {
      audioQualityChoice.append(audioQualities[i], null);
    }

    serviceChoice = new ChoiceGroup(I18N.tr("service"), Choice.EXCLUSIVE);
    String[] services = settingManager.getAvailableServices();
    for (int i = 0; i < services.length; i++) {
      serviceChoice.append(services[i], null);
    }

    append(languageChoice);
    append(audioQualityChoice);
    append(serviceChoice);

    addCommand(backCommand);
    addCommand(saveCommand);
    setCommandListener(this);
  }

  private void saveSettings() {
    try {
      String selectedLanguage = getSelectedLanguage();
      String currentLanguage = settingManager.getCurrentLanguage();
      String currentService = settingManager.getCurrentService();

      boolean languageChanged = !selectedLanguage.equals(currentLanguage);
      boolean serviceChanged = !getSelectedService().equals(currentService);

      settingManager.saveSettings(
          selectedLanguage, getSelectedAudioQuality(), getSelectedService());

      if (languageChanged) {
        I18N.setLanguage(selectedLanguage);
      }

      if (languageChanged || serviceChanged) {
        MainList mainList = Utils.createMainMenu(parent, getSelectedService());
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
    String[] languages = I18N.getLanguages();
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
