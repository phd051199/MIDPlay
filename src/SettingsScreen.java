import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.rms.RecordStoreException;

public final class SettingsScreen extends BaseForm {
  private final SettingsManager settingsManager;
  private final Listener listener;
  private final String[] availableLanguages = Lang.getAvailableLanguages();
  private ChoiceGroup languageGroup;
  private ChoiceGroup serviceGroup;
  private ChoiceGroup qualityGroup;
  private ChoiceGroup autoUpdateGroup;
  private ChoiceGroup playerMethodChoice;
  private String currentLanguage;
  private String currentService;
  private String currentQuality;
  private int currentAutoUpdate;
  private int currentPlayerMethod;

  public SettingsScreen(Navigator navigator, Listener listener) {
    super(Lang.tr(Configuration.MENU_SETTINGS), navigator);
    this.listener = listener;
    this.settingsManager = SettingsManager.getInstance();
    addComponents();
    loadSettings();
    addCommand(Commands.formSave());
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.formSave()) {
      saveSettings();
    }
  }

  private void addComponents() {
    languageGroup = createChoiceGroup("settings.language", availableLanguages, true);
    serviceGroup = createChoiceGroup("settings.service", Configuration.ALL_SERVICES, false);
    qualityGroup = createChoiceGroup("settings.audio_quality", Configuration.ALL_QUALITIES, false);
    autoUpdateGroup = new ChoiceGroup(Lang.tr("settings.auto_update"), ChoiceGroup.MULTIPLE);
    autoUpdateGroup.append(Lang.tr("settings.check_update"), null);
    playerMethodChoice = new ChoiceGroup(Lang.tr("settings.player_method"), ChoiceGroup.MULTIPLE);
    playerMethodChoice.append(Lang.tr("settings.force_pass_connection"), null);
    this.append(languageGroup);
    this.append(serviceGroup);
    this.append(qualityGroup);
    this.append(autoUpdateGroup);
    this.append(playerMethodChoice);
  }

  private ChoiceGroup createChoiceGroup(String titleKey, String[] values, boolean isLanguage) {
    ChoiceGroup group = new ChoiceGroup(Lang.tr(titleKey), ChoiceGroup.POPUP);
    for (int i = 0; i < values.length; i++) {
      String displayName = isLanguage ? Lang.tr("language." + values[i]) : values[i];
      group.append(displayName, null);
    }
    return group;
  }

  private void loadSettings() {
    currentLanguage = settingsManager.getCurrentLanguage();
    currentService = settingsManager.getCurrentService();
    currentQuality = settingsManager.getCurrentQuality();
    currentAutoUpdate = settingsManager.getCurrentAutoUpdate();
    currentPlayerMethod = settingsManager.getCurrentPlayerMethod();
    selectChoice(languageGroup, availableLanguages, currentLanguage);
    selectChoice(serviceGroup, Configuration.ALL_SERVICES, currentService);
    selectChoice(qualityGroup, Configuration.ALL_QUALITIES, currentQuality);
    autoUpdateGroup.setSelectedIndex(0, currentAutoUpdate == Configuration.AUTO_UPDATE_ENABLED);
    playerMethodChoice.setSelectedIndex(
        0, currentPlayerMethod == Configuration.PLAYER_INPUTSTREAM_ENABLED);
  }

  private void selectChoice(ChoiceGroup group, String[] values, String target) {
    for (int i = 0; i < values.length; i++) {
      if (values[i].equals(target)) {
        group.setSelectedIndex(i, true);
        break;
      }
    }
  }

  private void saveSettings() {
    try {
      String selectedLang = getSelected(languageGroup, availableLanguages, "en");
      String selectedService =
          getSelected(serviceGroup, Configuration.ALL_SERVICES, Configuration.SERVICE_NCT);
      String selectedQuality =
          getSelected(qualityGroup, Configuration.ALL_QUALITIES, Configuration.QUALITY_128);
      int selectedAutoUpdate =
          autoUpdateGroup.isSelected(0)
              ? Configuration.AUTO_UPDATE_ENABLED
              : Configuration.AUTO_UPDATE_DISABLED;
      int selectedPlayerMethod =
          playerMethodChoice.isSelected(0)
              ? Configuration.PLAYER_INPUTSTREAM_ENABLED
              : Configuration.PLAYER_INPUTSTREAM_DISABLED;
      boolean hasChanges = false;
      if (!currentLanguage.equals(selectedLang)) {
        hasChanges = true;
        settingsManager.saveLanguage(selectedLang);
        this.listener.onLanguageChanged(selectedLang);
      }
      if (!currentService.equals(selectedService)) {
        hasChanges = true;
        settingsManager.saveService(selectedService);
      }
      if (!currentQuality.equals(selectedQuality)) {
        hasChanges = true;
        settingsManager.saveQuality(selectedQuality);
      }
      if (currentAutoUpdate != selectedAutoUpdate) {
        hasChanges = true;
        settingsManager.saveAutoUpdate(selectedAutoUpdate);
      }
      if (currentPlayerMethod != selectedPlayerMethod) {
        hasChanges = true;
        settingsManager.savePlayerMethod(selectedPlayerMethod);
      }
      if (hasChanges) {
        this.listener.onSettingsSaved();
      } else {
        navigator.back();
      }
    } catch (RecordStoreException e) {
      navigator.showAlert(Lang.tr("settings.error.save_failed"), AlertType.ERROR);
    }
  }

  private String getSelected(ChoiceGroup group, String[] values, String defaultValue) {
    int selectedIndex = group.getSelectedIndex();
    return selectedIndex >= 0 ? values[selectedIndex] : defaultValue;
  }

  public interface Listener {
    void onLanguageChanged(String selectedLang);

    void onSettingsSaved();
  }
}
