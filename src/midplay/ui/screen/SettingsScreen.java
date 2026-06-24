package midplay.ui.screen;

import cc.nnproject.json.JSONObject;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStoreException;
import midplay.MIDPlay;
import midplay.net.ThemeColorOperation;
import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public final class SettingsScreen extends BaseForm {
  private static Image[] themeColorSwatches;

  private final SettingsManager settingsManager;
  private final Listener listener;
  private final String[] availableLanguages = Lang.getAvailableLanguages();

  private ChoiceGroup languageGroup;
  private ChoiceGroup themeModeGroup;
  private ChoiceGroup themeColorGroup;
  private ChoiceGroup serviceGroup;
  private ChoiceGroup qualityGroup;
  private ChoiceGroup togglesGroup;
  private ChoiceGroup playerMethodGroup;
  private int autoUpdateIndex;
  private int saveLastSessionIndex;
  private int blackberryWifiIndex = -1;
  private int thumbnailsIndex;

  private String currentLanguage;
  private String currentThemeMode;
  private String currentService;
  private String currentQuality;
  private int currentAutoUpdate;
  private int currentBlackberryWifi;
  private int currentSaveLastSession;
  private int currentThumbnails;
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

  protected boolean onBackPressed() {
    if (!isDirty()) {
      return false;
    }
    navigator.showConfirmationAlert(
        Lang.tr("confirm.discard_changes"),
        new Runnable() {
          public void run() {
            navigator.back();
          }
        },
        AlertType.WARNING);
    return true;
  }

  private boolean isDirty() {
    if (!currentLanguage.equals(getSelected(languageGroup, availableLanguages, "en"))) {
      return true;
    }
    if (!currentThemeMode.equals(
        getSelected(themeModeGroup, Configuration.ALL_THEME_MODES, Configuration.THEME_LIGHT))) {
      return true;
    }
    if (currentColorIndex != themeColorGroup.getSelectedIndex()) {
      return true;
    }
    if (!currentService.equals(
        getSelected(serviceGroup, Configuration.ALL_SERVICES, Configuration.SERVICE_NCT))) {
      return true;
    }
    if (!currentQuality.equals(
        getSelected(qualityGroup, Configuration.ALL_QUALITIES, Configuration.QUALITY_128))) {
      return true;
    }
    if (!currentPlayerMethod.equals(
        getSelected(
            playerMethodGroup,
            Configuration.ALL_PLAYER_METHODS,
            settingsManager.getDefaultPlayerMethod()))) {
      return true;
    }
    if ((currentAutoUpdate == Configuration.AUTO_UPDATE_ENABLED)
        != togglesGroup.isSelected(autoUpdateIndex)) {
      return true;
    }
    if ((currentSaveLastSession == Configuration.SAVE_LAST_SESSION_ON)
        != togglesGroup.isSelected(saveLastSessionIndex)) {
      return true;
    }
    if ((currentThumbnails == Configuration.THUMBNAILS_ON)
        != togglesGroup.isSelected(thumbnailsIndex)) {
      return true;
    }
    if (blackberryWifiIndex >= 0
        && (currentBlackberryWifi == Configuration.BLACKBERRY_WIFI_ON)
            != togglesGroup.isSelected(blackberryWifiIndex)) {
      return true;
    }
    return false;
  }

  private static Image[] getThemeColorSwatches() {
    if (themeColorSwatches == null) {
      themeColorSwatches = new Image[Configuration.THEME_COLORS.length];
      for (int i = 0; i < Configuration.THEME_COLORS.length; i++) {
        themeColorSwatches[i] = Utils.createImageFromHex(Configuration.THEME_COLORS[i], 12, 12);
      }
    }
    return themeColorSwatches;
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

    togglesGroup = new ChoiceGroup(Lang.tr("settings.options"), ChoiceGroup.MULTIPLE);
    autoUpdateIndex = togglesGroup.append(Lang.tr("settings.check_update"), null);
    saveLastSessionIndex = togglesGroup.append(Lang.tr("settings.save_last_session"), null);
    thumbnailsIndex = togglesGroup.append(Lang.tr("settings.load_thumbnails"), null);
    if (Utils.isBlackberry) {
      blackberryWifiIndex = togglesGroup.append(Lang.tr("settings.use_wifi"), null);
    }

    themeColorGroup = new ChoiceGroup(Lang.tr("settings.theme_color"), ChoiceGroup.POPUP);
    Image[] swatches = getThemeColorSwatches();
    for (int i = 0; i < Configuration.THEME_COLOR_NAMES.length; i++) {
      themeColorGroup.append(Configuration.THEME_COLOR_NAMES[i], swatches[i]);
    }

    append(languageGroup);
    append(themeModeGroup);
    append(themeColorGroup);
    append(serviceGroup);
    append(qualityGroup);
    append(playerMethodGroup);
    append(togglesGroup);
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
    currentBlackberryWifi = settingsManager.getCurrentBlackberryWifi();
    currentSaveLastSession = settingsManager.getCurrentSaveLastSession();
    currentThumbnails = settingsManager.getCurrentThumbnails();
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
    togglesGroup.setSelectedIndex(
        autoUpdateIndex, currentAutoUpdate == Configuration.AUTO_UPDATE_ENABLED);
    togglesGroup.setSelectedIndex(
        saveLastSessionIndex, currentSaveLastSession == Configuration.SAVE_LAST_SESSION_ON);
    togglesGroup.setSelectedIndex(
        thumbnailsIndex, currentThumbnails == Configuration.THUMBNAILS_ON);
    if (blackberryWifiIndex >= 0) {
      togglesGroup.setSelectedIndex(
          blackberryWifiIndex, currentBlackberryWifi == Configuration.BLACKBERRY_WIFI_ON);
    }
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
          togglesGroup.isSelected(autoUpdateIndex)
              ? Configuration.AUTO_UPDATE_ENABLED
              : Configuration.AUTO_UPDATE_DISABLED;
      int selectedSaveLastSession =
          togglesGroup.isSelected(saveLastSessionIndex)
              ? Configuration.SAVE_LAST_SESSION_ON
              : Configuration.SAVE_LAST_SESSION_OFF;
      int selectedThumbnails =
          togglesGroup.isSelected(thumbnailsIndex)
              ? Configuration.THUMBNAILS_ON
              : Configuration.THUMBNAILS_OFF;
      int selectedBlackberryWifi =
          blackberryWifiIndex >= 0 && togglesGroup.isSelected(blackberryWifiIndex)
              ? Configuration.BLACKBERRY_WIFI_ON
              : Configuration.BLACKBERRY_WIFI_OFF;
      int selectedThemeColor = themeColorGroup.getSelectedIndex();
      boolean hasChanges = false;
      if (!currentLanguage.equals(selectedLang)) {
        hasChanges = true;
        settingsManager.saveLanguage(selectedLang);
        listener.onLanguageChanged(selectedLang);
      }
      boolean hasColorChange = currentColorIndex != selectedThemeColor;
      boolean hasModeChange = !currentThemeMode.equals(selectedThemeMode);
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
      if (currentSaveLastSession != selectedSaveLastSession) {
        hasChanges = true;
        settingsManager.saveSaveLastSession(selectedSaveLastSession);
      }
      if (currentThumbnails != selectedThumbnails) {
        hasChanges = true;
        settingsManager.saveThumbnails(selectedThumbnails);
      }
      if (currentBlackberryWifi != selectedBlackberryWifi) {
        hasChanges = true;
        settingsManager.saveBlackberryWifi(selectedBlackberryWifi);
      }
      if (!currentPlayerMethod.equals(selectedPlayerMethod)) {
        hasChanges = true;
        settingsManager.savePlayerMethod(selectedPlayerMethod);
      }

      if (hasColorChange || hasModeChange) {
        hasChanges = true;
        handleThemeChanges(selectedThemeMode, selectedThemeColor, hasColorChange, hasModeChange);
      } else if (hasChanges) {
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
      saveThemeMode(newThemeMode, true);
    }
  }

  private boolean saveThemeMode(String newThemeMode, boolean notifySaved) {
    try {
      settingsManager.saveTheme(newThemeMode);
      listener.onThemeChanged();
      if (notifySaved) {
        listener.onSettingsSaved();
      }
      return true;
    } catch (RecordStoreException e) {
      navigator.showAlert(Lang.tr("settings.error.save_theme", e.toString()), AlertType.ERROR);
      return false;
    }
  }

  private void loadThemeColors(
      final int selected, final String newThemeMode, final boolean hasModeChange) {
    if (selected < 0 || selected >= Configuration.THEME_COLORS.length) {
      if (hasModeChange) {
        saveThemeMode(newThemeMode, true);
      }
      return;
    }

    String colorHex = Utils.toHexRgb(Configuration.THEME_COLORS[selected]);
    MIDPlay.startOperation(
        new ThemeColorOperation(
            colorHex,
            new ThemeColorOperation.ThemeColorListener() {
              public void onThemeColorsReceived(JSONObject lightColors, JSONObject darkColors) {
                try {
                  settingsManager.saveThemeColors(lightColors, darkColors, selected);
                  if (hasModeChange) {
                    settingsManager.saveTheme(newThemeMode);
                  }
                  settingsManager.loadAndApplyThemeColors();
                  listener.onThemeChanged();
                  listener.onSettingsSaved();
                } catch (Exception e) {
                  navigator.showAlert(
                      Lang.tr("settings.error.save_theme", e.toString()), AlertType.WARNING);
                }
              }

              public void onError(Exception e) {
                if (hasModeChange) {
                  if (saveThemeMode(newThemeMode, false)) {
                    navigator.showAlert(
                        Lang.tr("settings.error.color_update_failed"), AlertType.WARNING);
                  }
                } else {
                  navigator.showAlert(
                      Lang.tr("settings.error.load_theme", e.toString()), AlertType.ERROR);
                }
              }
            }));
  }

  public interface Listener {
    void onLanguageChanged(String selectedLang);

    void onThemeChanged();

    void onSettingsSaved();
  }
}
