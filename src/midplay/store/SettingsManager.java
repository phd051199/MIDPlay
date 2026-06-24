package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;
import midplay.ui.Theme;
import midplay.util.Lang;
import midplay.util.Utils;

public class SettingsManager {
  private static final int SETTINGS_ID = 1;
  private static SettingsManager instance;
  private static String currentLanguage;
  private static String currentService;
  private static String currentQuality;
  private static String currentSearchType;
  private static int currentAutoUpdate;
  private static String currentPlayerMethod;
  private static String detectedDefaultMethod;
  private static boolean detectedDefaultMethodResolved;
  private static int currentRepeatMode;
  private static int currentShuffleMode;
  private static int currentVolumeLevel;
  private static String currentThemeMode;
  private static int currentBlackberryWifi;
  private static int currentSaveLastSession;
  private static int currentThumbnails;
  private static boolean currentEqEnabled;
  private static int currentEqPreset; // -1 = manual/custom levels
  private static int[] currentEqLevels;

  public static SettingsManager getInstance() {
    if (instance == null) {
      instance = new SettingsManager();
    }
    return instance;
  }

  private final JsonRecordStore storage;
  private JSONObject cachedSettings;

  private JSONObject settings() {
    if (cachedSettings == null) {
      cachedSettings = getSettingsJSON();
    }
    return cachedSettings;
  }

  private SettingsManager() {
    storage =
        new JsonRecordStore(
            Configuration.STORAGE_SETTINGS, SETTINGS_ID, createDefaultSettings().toString());
  }

  public String getDefaultPlayerMethod() {
    if (!detectedDefaultMethodResolved) {
      detectedDefaultMethodResolved = true;
      detectedDefaultMethod = detectPlayerHttpMethod();
    }
    return detectedDefaultMethod;
  }

  // Detect the best MMAPI player method (URL vs InputStream) from device platform
  // quirks. reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  private static String detectPlayerHttpMethod() {
    String platform = System.getProperty("microedition.platform");

    boolean symbianJrt = platform != null && platform.indexOf("platform=S60") != -1;
    boolean symbian =
        symbianJrt
            || Utils.hasProperty("com.symbian.midp.serversocket.support")
            || Utils.hasProperty("com.symbian.default.to.suite.icon")
            || Utils.hasClass("com.symbian.midp.io.protocol.http.Protocol")
            || Utils.hasClass("com.symbian.lcdjava.io.File");

    String method = Configuration.PLAYER_METHOD_PASS_URL;

    if (Utils.hasClass("com.nokia.mid.impl.isa.jam.Jam")) {
      // S40v1 uses sun impl for media and i/o so it should work fine with URL
      // S40v2+ breaks http locator parsing so needs InputStream
      method =
          Utils.hasClass("com.sun.mmedia.protocol.CommonDS")
              ? Configuration.PLAYER_METHOD_PASS_URL
              : Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    } else if (symbian) {
      if (symbianJrt
          && platform != null
          && (platform.indexOf("java_build_version=2.") != -1
              || platform.indexOf("java_build_version=1.4") != -1)) {
        // EMC (S60v5+) supports mp3 streaming - keep default URL method
      } else if (Utils.hasClass("com.symbian.mmapi.PlayerImpl")) {
        // UIQ - use InputStream
        method = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
      } else {
        // MMF (S60v3.2-) - use InputStream
        method = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
      }
    } else if (Utils.isJ2MELoader()) {
      method = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    }

    return method;
  }

  public void loadSettings() {
    JSONObject settings = settings();
    setCurrentLanguage(settings.getString("language", "en"));
    setCurrentThemeMode(settings.getString("themeMode", Configuration.THEME_LIGHT));
    currentService = settings.getString("service", Configuration.SERVICE_NCT);
    currentQuality = settings.getString("quality", Configuration.QUALITY_128);
    currentSearchType = settings.getString("searchType", Configuration.SEARCH_PLAYLIST);
    currentAutoUpdate = settings.getInt("autoUpdate", Configuration.AUTO_UPDATE_ENABLED);
    currentPlayerMethod = settings.getString("playerMethod", getDefaultPlayerMethod());
    currentRepeatMode = settings.getInt("repeatMode", Configuration.PLAYER_REPEAT_ALL);
    currentShuffleMode = settings.getInt("shuffleMode", Configuration.PLAYER_SHUFFLE_OFF);
    currentVolumeLevel = settings.getInt("volumeLevel", Configuration.PLAYER_MAX_VOLUME);
    currentBlackberryWifi = settings.getInt("blackberryWifi", Configuration.BLACKBERRY_WIFI_ON);
    currentSaveLastSession =
        settings.getInt("saveLastSession", Configuration.SAVE_LAST_SESSION_OFF);
    currentThumbnails = settings.getInt("thumbnails", Configuration.THUMBNAILS_ON);
    currentEqEnabled = settings.getBoolean("eqEnabled", false);
    currentEqPreset = settings.getInt("eqPreset", -1);
    currentEqLevels = parseLevelsCsv(settings.getString("eqLevels", ""));
  }

  private JSONObject getSettingsJSON() {
    try {
      return JSON.getObject(storage.load());
    } catch (JSONException e) {
    }
    return createDefaultSettings();
  }

  private JSONObject createDefaultSettings() {
    JSONObject settings = new JSONObject();
    settings.put("language", "en");
    settings.put("service", Configuration.SERVICE_NCT);
    settings.put("quality", Configuration.QUALITY_128);
    settings.put("repeatMode", Configuration.PLAYER_REPEAT_ALL);
    settings.put("shuffleMode", Configuration.PLAYER_SHUFFLE_OFF);
    settings.put("volumeLevel", Configuration.PLAYER_MAX_VOLUME);
    settings.put("searchType", Configuration.SEARCH_PLAYLIST);
    settings.put("autoUpdate", Configuration.AUTO_UPDATE_ENABLED);
    settings.put("playerMethod", getDefaultPlayerMethod());
    settings.put("themeMode", Configuration.THEME_LIGHT);
    settings.put("blackberryWifi", Configuration.BLACKBERRY_WIFI_ON);
    settings.put("saveLastSession", Configuration.SAVE_LAST_SESSION_OFF);
    settings.put("thumbnails", Configuration.THUMBNAILS_ON);
    settings.put("eqEnabled", false);
    settings.put("eqPreset", -1);
    settings.put("eqLevels", "");
    return settings;
  }

  private void saveJSON(JSONObject settings) throws RecordStoreException {
    storage.save(settings.toString());
  }

  private void saveSetting(String key, String value) throws RecordStoreException {
    JSONObject settings = settings();
    if (settings.has(key) && value != null && value.equals(settings.getString(key, ""))) {
      return;
    }
    settings.put(key, value);
    saveJSON(settings);
  }

  private void saveSetting(String key, int value) throws RecordStoreException {
    JSONObject settings = settings();
    if (settings.has(key) && value == settings.getInt(key, value)) {
      return;
    }
    settings.put(key, value);
    saveJSON(settings);
  }

  private void saveSetting(String key, JSONObject value) throws RecordStoreException {
    JSONObject settings = settings();
    if (settings.has(key) && value != null) {
      try {
        JSONObject existing = settings.getObject(key);
        if (existing != null && value.build().equals(existing.build())) {
          return;
        }
      } catch (Exception e) {
      }
    }
    settings.put(key, value);
    saveJSON(settings);
  }

  public void saveLanguage(String langCode) throws RecordStoreException {
    saveSetting("language", langCode);
    setCurrentLanguage(langCode);
  }

  public void saveTheme(String mode) throws RecordStoreException {
    saveSetting("themeMode", mode);
    setCurrentThemeMode(mode);
  }

  public void saveService(String serviceCode) throws RecordStoreException {
    saveSetting("service", serviceCode);
    currentService = serviceCode;
  }

  public void saveQuality(String qualityCode) throws RecordStoreException {
    saveSetting("quality", qualityCode);
    currentQuality = qualityCode;
  }

  public void saveSearchType(String searchType) throws RecordStoreException {
    saveSetting("searchType", searchType);
    currentSearchType = searchType;
  }

  public void saveAutoUpdate(int autoUpdate) throws RecordStoreException {
    saveSetting("autoUpdate", autoUpdate);
    currentAutoUpdate = autoUpdate;
  }

  public void saveSaveLastSession(int saveLastSession) throws RecordStoreException {
    saveSetting("saveLastSession", saveLastSession);
    currentSaveLastSession = saveLastSession;
  }

  public void savePlayerMethod(String playerMethod) throws RecordStoreException {
    saveSetting("playerMethod", playerMethod);
    currentPlayerMethod = playerMethod;
  }

  public void saveRepeatMode(int repeatMode) throws RecordStoreException {
    saveSetting("repeatMode", repeatMode);
    currentRepeatMode = repeatMode;
  }

  public void saveShuffleMode(int shuffleMode) throws RecordStoreException {
    saveSetting("shuffleMode", shuffleMode);
    currentShuffleMode = shuffleMode;
  }

  public void saveVolumeLevel(int volumeLevel) throws RecordStoreException {
    saveSetting("volumeLevel", volumeLevel);
    currentVolumeLevel = volumeLevel;
  }

  public void saveBlackberryWifi(int blackberryWifi) throws RecordStoreException {
    saveSetting("blackberryWifi", blackberryWifi);
    currentBlackberryWifi = blackberryWifi;
  }

  public void saveThumbnails(int thumbnails) throws RecordStoreException {
    saveSetting("thumbnails", thumbnails);
    currentThumbnails = thumbnails;
  }

  public void saveThemeColors(JSONObject lightColors, JSONObject darkColors, int selected)
      throws RecordStoreException {
    JSONObject themeData = new JSONObject();
    themeData.put("light", lightColors);
    themeData.put("dark", darkColors);
    themeData.put("selected", selected);
    saveSetting("themeColors", themeData);
  }

  public void loadAndApplyThemeColors() {
    try {
      JSONObject settings = settings();
      if (settings.has("themeColors")) {
        JSONObject themeData = settings.getObject("themeColors");
        String colorType = Theme.isDark() ? "dark" : "light";
        if (themeData.has(colorType)) {
          JSONObject colors = themeData.getObject(colorType);
          Theme.applyColors(colors);
          return;
        }
      }
    } catch (Exception e) {
    }
    Theme.applyDefaults(Theme.isDark());
  }

  public int getSavedColorIndex() {
    try {
      JSONObject settings = settings();
      if (settings.has("themeColors")) {
        JSONObject themeData = settings.getObject("themeColors");
        return themeData.getInt("selected", 0);
      }
    } catch (Exception e) {
    }
    return 0;
  }

  public String getCurrentLanguage() {
    return currentLanguage;
  }

  public void setCurrentLanguage(String langCode) {
    currentLanguage = langCode;
    Lang.setLang(langCode);
  }

  public String getCurrentThemeMode() {
    return currentThemeMode;
  }

  public void setCurrentThemeMode(String mode) {
    currentThemeMode = mode;
    Theme.setDark(Configuration.THEME_DARK.equals(mode));
    loadAndApplyThemeColors();
  }

  public String getCurrentService() {
    return currentService;
  }

  public String getCurrentQuality() {
    return currentQuality;
  }

  public String getCurrentSearchType() {
    return currentSearchType;
  }

  public int getCurrentAutoUpdate() {
    return currentAutoUpdate;
  }

  public int getCurrentSaveLastSession() {
    return currentSaveLastSession;
  }

  public String getCurrentPlayerMethod() {
    return currentPlayerMethod;
  }

  public int getCurrentRepeatMode() {
    return currentRepeatMode;
  }

  public int getCurrentShuffleMode() {
    return currentShuffleMode;
  }

  public int getCurrentVolumeLevel() {
    return currentVolumeLevel;
  }

  public int getCurrentBlackberryWifi() {
    return currentBlackberryWifi;
  }

  public int getCurrentThumbnails() {
    return currentThumbnails;
  }

  public void setEqEnabledLive(boolean enabled) {
    currentEqEnabled = enabled;
  }

  public void setEqPresetLive(int preset) {
    currentEqPreset = preset;
  }

  public void setEqLevelsLive(int[] levels) {
    currentEqLevels = levels;
  }

  public boolean isEqEnabled() {
    return currentEqEnabled;
  }

  public int getEqPreset() {
    return currentEqPreset;
  }

  public int[] getEqLevels() {
    return currentEqLevels;
  }

  public void saveEqualizer(boolean enabled, int preset, int[] levels) throws RecordStoreException {
    JSONObject settings = settings();
    settings.put("eqEnabled", enabled);
    settings.put("eqPreset", preset);
    settings.put("eqLevels", toLevelsCsv(levels));
    saveJSON(settings);
    currentEqEnabled = enabled;
    currentEqPreset = preset;
    currentEqLevels = levels;
  }

  private static String toLevelsCsv(int[] levels) {
    if (levels == null || levels.length == 0) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < levels.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(levels[i]);
    }
    return sb.toString();
  }

  private static int[] parseLevelsCsv(String csv) {
    if (csv == null || csv.length() == 0) {
      return null;
    }
    int count = 1;
    for (int i = 0; i < csv.length(); i++) {
      if (csv.charAt(i) == ',') {
        count++;
      }
    }
    int[] result = new int[count];
    int idx = 0;
    int start = 0;
    for (int i = 0; i <= csv.length(); i++) {
      if (i == csv.length() || csv.charAt(i) == ',') {
        try {
          result[idx] = Integer.parseInt(csv.substring(start, i).trim());
        } catch (NumberFormatException e) {
          result[idx] = 0;
        }
        idx++;
        start = i + 1;
      }
    }
    return result;
  }

  public void cleanup() {
    storage.close();
  }
}
