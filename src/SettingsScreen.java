import cc.nnproject.json.JSONObject;
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
  private ChoiceGroup themeModeGroup;
  private ChoiceGroup themeColorGroup;
  private ChoiceGroup serviceGroup;
  private ChoiceGroup qualityGroup;
  private ChoiceGroup autoUpdateGroup;
  private ChoiceGroup playerMethodGroup;

  private String currentLanguage;
  private String currentThemeMode;
  private String currentService;
  private String currentQuality;
  private int currentAutoUpdate;
  private String currentPlayerMethod;
  private int currentColorIndex;

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
    themeModeGroup =
        createChoiceGroup(
            "settings.theme_mode", Configuration.ALL_THEME_MODES, "settings.theme_mode_options.");
    serviceGroup = createChoiceGroup("settings.service", Configuration.ALL_SERVICES, null);
    qualityGroup = createChoiceGroup("settings.audio_quality", Configuration.ALL_QUALITIES, null);
    playerMethodGroup =
        createChoiceGroup(
            "settings.player_method",
            Configuration.ALL_PLAYER_METHODS,
            "settings.player_method_options.");

    autoUpdateGroup = new ChoiceGroup(Lang.tr("settings.auto_update"), ChoiceGroup.MULTIPLE);
    autoUpdateGroup.append(Lang.tr("settings.check_update"), null);

    themeColorGroup = new ChoiceGroup(Lang.tr("settings.theme_color"), ChoiceGroup.POPUP);
    for (int i = 0; i < Configuration.THEME_COLOR_NAMES.length; i++) {
      themeColorGroup.append(
          Configuration.THEME_COLOR_NAMES[i],
          Utils.createImageFromHex(Configuration.THEME_COLORS[i], 12, 12));
    }

    append(languageGroup);
    append(themeModeGroup);
    append(themeColorGroup);
    append(serviceGroup);
    append(qualityGroup);
    append(playerMethodGroup);
    append(autoUpdateGroup);
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
    currentThemeMode = settingsManager.getCurrentThemeMode();
    currentService = settingsManager.getCurrentService();
    currentQuality = settingsManager.getCurrentQuality();
    currentAutoUpdate = settingsManager.getCurrentAutoUpdate();
    currentPlayerMethod = settingsManager.getCurrentPlayerMethod();
    currentColorIndex = settingsManager.getSavedColorIndex();
    selectChoice(languageGroup, availableLanguages, currentLanguage);
    selectChoice(themeModeGroup, Configuration.ALL_THEME_MODES, currentThemeMode);
    selectChoice(
        themeColorGroup,
        Configuration.THEME_COLOR_NAMES,
        Configuration.THEME_COLOR_NAMES[currentColorIndex]);
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
      String selectedThemeMode =
          getSelected(themeModeGroup, Configuration.ALL_THEME_MODES, Configuration.THEME_LIGHT);
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
      int selectedThemeColor = themeColorGroup.getSelectedIndex();
      boolean hasChanges = false;
      if (!currentLanguage.equals(selectedLang)) {
        hasChanges = true;
        settingsManager.saveLanguage(selectedLang);
        listener.onLanguageChanged(selectedLang);
      }
      boolean hasColorChange = currentColorIndex != selectedThemeColor;
      boolean hasModeChange = !currentThemeMode.equals(selectedThemeMode);

      if (hasColorChange || hasModeChange) {
        hasChanges = true;
        handleThemeChanges(selectedThemeMode, selectedThemeColor, hasColorChange, hasModeChange);
        return;
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
        listener.onSettingsSaved();
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

  private void handleThemeChanges(
      String newThemeMode, int newColorIndex, boolean hasColorChange, boolean hasModeChange) {
    if (hasColorChange) {
      loadThemeColors(newColorIndex, newThemeMode, hasModeChange);
    } else if (hasModeChange) {
      saveThemeMode(newThemeMode);
    }
  }

  private void saveThemeMode(String newThemeMode) {
    try {
      settingsManager.saveTheme(newThemeMode);
      listener.onThemeChanged();
      listener.onSettingsSaved();
    } catch (RecordStoreException e) {
      navigator.showAlert("Error saving theme: " + e.toString(), AlertType.ERROR);
    }
  }

  private void loadThemeColors(
      final int selected, final String newThemeMode, final boolean hasModeChange) {
    if (selected < 0 || selected >= Configuration.THEME_COLORS.length) {
      if (hasModeChange) saveThemeMode(newThemeMode);
      return;
    }

    String colorHex = Integer.toHexString(Configuration.THEME_COLORS[selected]);
    MIDPlay.startOperation(
        new ThemeColorOperation(
            colorHex,
            new ThemeColorOperation.ThemeColorListener() {
              public void onThemeColorsReceived(JSONObject lightColors, JSONObject darkColors) {
                try {
                  settingsManager.saveThemeColors(lightColors, darkColors, selected);
                  if (hasModeChange) settingsManager.saveTheme(newThemeMode);
                  settingsManager.loadAndApplyThemeColors();
                  listener.onThemeChanged();
                  listener.onSettingsSaved();
                } catch (Exception e) {
                  navigator.showAlert("Error saving theme: " + e.toString(), AlertType.WARNING);
                }
              }

              public void onError(Exception e) {
                navigator.showAlert("Error loading theme: " + e.toString(), AlertType.ERROR);
                if (hasModeChange) saveThemeMode(newThemeMode);
                else listener.onSettingsSaved();
              }
            }));
  }

  public interface Listener {
    void onLanguageChanged(String selectedLang);

    void onThemeChanged();

    void onSettingsSaved();
  }
}
