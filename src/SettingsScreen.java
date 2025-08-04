import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.rms.RecordStoreException;

public final class SettingsScreen extends BaseForm {
  private final SettingsManager settingsManager;
  private final Listener listener;
  private final String[] availableLanguages = Lang.getAvailableLanguages();
  //private final Theme[] availableThemes = // STOP HERE
  private ChoiceGroup languageGroup;
  private ChoiceGroup themeGroup;
  private ChoiceGroup serviceGroup;
  private ChoiceGroup qualityGroup;
  private ChoiceGroup autoUpdateGroup;
  private ChoiceGroup playerMethodGroup;
  private String currentLanguage;
  private String currentTheme;
  private String currentService;
  private String currentQuality;
  private int currentAutoUpdate;
  private String currentPlayerMethod;

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
    languageGroup = createChoiceGroup("settings.language", availableLanguages, "language.");
    themeGroup = createChoiceGroup("settings.theme", Theme.getAllThemeNames(), null);
    serviceGroup = createChoiceGroup("settings.service", Configuration.ALL_SERVICES, null);
    qualityGroup = createChoiceGroup("settings.audio_quality", Configuration.ALL_QUALITIES, null);
    playerMethodGroup =
        createChoiceGroup(
            "settings.player_method",
            Configuration.ALL_PLAYER_METHODS,
            "settings.player_method_options.");
    autoUpdateGroup = new ChoiceGroup(Lang.tr("settings.auto_update"), ChoiceGroup.MULTIPLE);
    autoUpdateGroup.append(Lang.tr("settings.check_update"), null);
    this.append(languageGroup);
    this.append(themeGroup);
    this.append(serviceGroup);
    this.append(qualityGroup);
    this.append(playerMethodGroup);
    this.append(autoUpdateGroup);
  }

  private ChoiceGroup createChoiceGroup(
      String titleKey, String[] values, String translationPrefix) {
    ChoiceGroup group = new ChoiceGroup(Lang.tr(titleKey), ChoiceGroup.POPUP);
    for (int i = 0; i < values.length; i++) {
      String displayName =
          translationPrefix != null ? Lang.tr(translationPrefix + values[i]) : values[i];
      group.append(displayName, null);
    }
    return group;
  }

  private void loadSettings() {
    currentLanguage = settingsManager.getCurrentLanguage();
    currentTheme = Theme.getCurrentTheme().getName();
    currentService = settingsManager.getCurrentService();
    currentQuality = settingsManager.getCurrentQuality();
    currentAutoUpdate = settingsManager.getCurrentAutoUpdate();
    currentPlayerMethod = settingsManager.getCurrentPlayerMethod();
    selectChoice(languageGroup, availableLanguages, currentLanguage);
    selectChoice(themeGroup, Theme.getAllThemeNames(), currentTheme);
    selectChoice(serviceGroup, Configuration.ALL_SERVICES, currentService);
    selectChoice(qualityGroup, Configuration.ALL_QUALITIES, currentQuality);
    selectChoice(playerMethodGroup, Configuration.ALL_PLAYER_METHODS, currentPlayerMethod);
    autoUpdateGroup.setSelectedIndex(0, currentAutoUpdate == Configuration.AUTO_UPDATE_ENABLED);
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
      String selectedTheme = getSelected(themeGroup, Theme.getAllThemeNames(), Configuration.THEME_LIGHT);
      String selectedService =
          getSelected(serviceGroup, Configuration.ALL_SERVICES, Configuration.SERVICE_NCT);
      String selectedQuality =
          getSelected(qualityGroup, Configuration.ALL_QUALITIES, Configuration.QUALITY_128);
      String selectedPlayerMethod =
          getSelected(
              playerMethodGroup,
              Configuration.ALL_PLAYER_METHODS,
              settingsManager.getDefaultPlayerMethod());
      int selectedAutoUpdate =
          autoUpdateGroup.isSelected(0)
              ? Configuration.AUTO_UPDATE_ENABLED
              : Configuration.AUTO_UPDATE_DISABLED;
      boolean hasChanges = false;
      if (!currentLanguage.equals(selectedLang)) {
        hasChanges = true;
        settingsManager.saveLanguage(selectedLang);
        this.listener.onLanguageChanged(selectedLang);
      }
      if (!currentTheme.equals(selectedTheme)) {
        hasChanges = true;
        settingsManager.saveTheme(selectedTheme);
        this.listener.onThemeChanged(selectedTheme);
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
      if (!currentPlayerMethod.equals(selectedPlayerMethod)) {
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

    void onThemeChanged(String selectedTheme);
    
    void onSettingsSaved();
  }
}
