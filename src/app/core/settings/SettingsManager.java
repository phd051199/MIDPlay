package app.core.settings;

import app.constants.Services;
import app.utils.concurrent.ThreadManager;
import app.utils.text.LocalizationManager;
import java.util.Vector;
import org.json.me.JSONObject;

public class SettingsManager {

  public static final int CATEGORY_APP = 0;
  public static final int CATEGORY_MENU = 1;
  public static final int CATEGORY_PLAYER = 2;
  public static final int CATEGORY_SEARCH = 3;

  private static final String[] CATEGORY_STORE_NAMES = {
    "app_settings", "menu_settings", "player_settings", "search_settings"
  };

  private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};
  private static final String[] AVAILABLE_SERVICES = {
    Services.NCT, Services.SOUNDCLOUD, Services.YTMUSIC, Services.SPOTIFY
  };
  private static final long SAVE_THROTTLE = 2000;

  private static SettingsManager instance;

  public static String getStoreName(int category) {
    if (category >= 0 && category < CATEGORY_STORE_NAMES.length) {
      return CATEGORY_STORE_NAMES[category];
    }
    return CATEGORY_STORE_NAMES[CATEGORY_APP];
  }

  public static synchronized SettingsManager getInstance() {
    if (instance == null) {
      instance = new SettingsManager();
    }
    return instance;
  }

  private final SettingsStore settingsStore;
  private Thread saveThread = null;
  private boolean isShuttingDown = false;
  private boolean settingsModified = false;
  private long lastSaveTime = 0;

  private String language;
  private String audioQuality;
  private String service;
  private boolean autoUpdate;
  private boolean loadPlaylistArt;
  private String themeColor;
  private String backgroundColor;
  private int cachedThemeColorRGB = -1;
  private int cachedBackgroundColorRGB = -1;
  private final String lastThemeColor = "";
  private final String lastBackgroundColor = "";

  private int[] nctMenuOrder = null;
  private int[] soundcloudMenuOrder = null;
  private boolean[] nctMenuVisibility = null;
  private boolean[] soundcloudMenuVisibility = null;

  private int volumeLevel = 100;
  private int repeatMode = 2;
  private boolean shuffleMode = false;

  private int searchTypeIndex = 0;

  private SettingsManager() {
    settingsStore = new SettingsStore();
    loadAllSettings();
  }

  private void loadAllSettings() {
    loadAppSettings();
    loadMenuSettings();
    loadPlayerSettings();
    loadSearchSettings();
  }

  private void loadAppSettings() {
    try {
      JSONObject settings = settingsStore.loadSettings(CATEGORY_APP);
      if (settings != null) {
        this.language = settings.optString("language", LocalizationManager.getLanguage());
        this.audioQuality = settings.optString("audioQuality", AUDIO_QUALITIES[0]);
        this.service = settings.optString("service", AVAILABLE_SERVICES[0]);
        this.autoUpdate = settings.optBoolean("autoUpdate", true);
        this.loadPlaylistArt = settings.optBoolean("loadPlaylistArt", true);
        this.themeColor = settings.optString("themeColor", "410A4A");
        this.backgroundColor = settings.optString("backgroundColor", "F0F0F0");
      } else {
        setDefaultAppSettings();
      }
    } catch (Exception e) {
      setDefaultAppSettings();
    }
  }

  private void loadMenuSettings() {
    try {
      JSONObject settings = settingsStore.loadSettings(CATEGORY_MENU);
      if (settings != null) {
        loadMenuArraysFromJson(settings);
      } else {
        setDefaultMenuSettings();
      }
    } catch (Exception e) {
      setDefaultMenuSettings();
    }
  }

  private void loadMenuArraysFromJson(JSONObject settings) {
    try {

      if (settings.has("nctMenuOrder")) {
        String orderStr = settings.getString("nctMenuOrder");
        nctMenuOrder = parseIntArray(orderStr);
      }

      if (settings.has("nctMenuVisibility")) {
        String visibilityStr = settings.getString("nctMenuVisibility");
        nctMenuVisibility = parseBooleanArray(visibilityStr);
      }

      if (settings.has("soundcloudMenuOrder")) {
        String orderStr = settings.getString("soundcloudMenuOrder");
        soundcloudMenuOrder = parseIntArray(orderStr);
      }

      if (settings.has("soundcloudMenuVisibility")) {
        String visibilityStr = settings.getString("soundcloudMenuVisibility");
        soundcloudMenuVisibility = parseBooleanArray(visibilityStr);
      }
    } catch (Exception e) {
      setDefaultMenuSettings();
    }
  }

  private int[] parseIntArray(String arrayStr) {
    if (arrayStr == null || arrayStr.length() == 0) {
      return null;
    }

    try {
      String[] parts = split(arrayStr, ",");
      int[] result = new int[parts.length];
      for (int i = 0; i < parts.length; i++) {
        result[i] = Integer.parseInt(parts[i].trim());
      }
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean[] parseBooleanArray(String arrayStr) {
    if (arrayStr == null || arrayStr.length() == 0) {
      return null;
    }

    try {
      String[] parts = split(arrayStr, ",");
      boolean[] result = new boolean[parts.length];
      for (int i = 0; i < parts.length; i++) {
        result[i] = "true".equals(parts[i].trim());
      }
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  private String[] split(String str, String delimiter) {
    Vector parts = new Vector();
    int start = 0;
    int end = str.indexOf(delimiter);

    while (end != -1) {
      parts.addElement(str.substring(start, end));
      start = end + delimiter.length();
      end = str.indexOf(delimiter, start);
    }
    parts.addElement(str.substring(start));

    String[] result = new String[parts.size()];
    for (int i = 0; i < parts.size(); i++) {
      result[i] = (String) parts.elementAt(i);
    }
    return result;
  }

  private void loadPlayerSettings() {
    try {
      JSONObject settings = settingsStore.loadSettings(CATEGORY_PLAYER);
      if (settings != null) {
        this.volumeLevel = settings.optInt("volumeLevel", 100);
        this.repeatMode = settings.optInt("repeatMode", 2);
        this.shuffleMode = settings.optBoolean("shuffleMode", false);
      } else {
        setDefaultPlayerSettings();
      }
    } catch (Exception e) {
      setDefaultPlayerSettings();
    }
  }

  private void loadSearchSettings() {
    try {
      JSONObject settings = settingsStore.loadSettings(CATEGORY_SEARCH);
      if (settings != null) {
        this.searchTypeIndex = settings.optInt("searchTypeIndex", 0);
      } else {
        setDefaultSearchSettings();
      }
    } catch (Exception e) {
      setDefaultSearchSettings();
    }
  }

  private void setDefaultAppSettings() {
    this.language = LocalizationManager.getLanguage();
    this.audioQuality = AUDIO_QUALITIES[0];
    this.service = AVAILABLE_SERVICES[0];
    this.autoUpdate = true;
    this.loadPlaylistArt = true;
    this.themeColor = "410A4A";
    this.backgroundColor = "F0F0F0";
  }

  private void setDefaultMenuSettings() {

    nctMenuOrder = null;
    nctMenuVisibility = null;
    soundcloudMenuOrder = null;
    soundcloudMenuVisibility = null;
  }

  private void setDefaultPlayerSettings() {
    this.volumeLevel = 100;
    this.repeatMode = 2;
    this.shuffleMode = false;
  }

  private void setDefaultSearchSettings() {
    this.searchTypeIndex = 0;
  }

  public String getCurrentLanguage() {
    return language;
  }

  public void setCurrentLanguage(String language) {
    if (!this.language.equals(language)) {
      this.language = language;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public String getCurrentAudioQuality() {
    return audioQuality;
  }

  public void setCurrentAudioQuality(String audioQuality) {
    if (!this.audioQuality.equals(audioQuality)) {
      this.audioQuality = audioQuality;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public String getCurrentService() {
    return service;
  }

  public void setCurrentService(String service) {
    if (!this.service.equals(service)) {
      this.service = service;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public boolean isAutoUpdate() {
    return autoUpdate;
  }

  public void setAutoUpdate(boolean autoUpdate) {
    if (this.autoUpdate != autoUpdate) {
      this.autoUpdate = autoUpdate;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public boolean isLoadPlaylistArt() {
    return loadPlaylistArt;
  }

  public void setLoadPlaylistArt(boolean loadPlaylistArt) {
    if (this.loadPlaylistArt != loadPlaylistArt) {
      this.loadPlaylistArt = loadPlaylistArt;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public String getThemeColor() {
    return themeColor;
  }

  public void setThemeColor(String themeColor) {
    if (!this.themeColor.equals(themeColor)) {
      this.themeColor = themeColor;
      this.cachedThemeColorRGB = -1;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(String backgroundColor) {
    if (!this.backgroundColor.equals(backgroundColor)) {
      this.backgroundColor = backgroundColor;
      this.cachedBackgroundColorRGB = -1;
      markModifiedAndSave(CATEGORY_APP);
    }
  }

  public String[] getAudioQualities() {
    return AUDIO_QUALITIES;
  }

  public String[] getAvailableServices() {
    return AVAILABLE_SERVICES;
  }

  public boolean isLoadPlaylistArtEnabled() {
    return isLoadPlaylistArt();
  }

  public boolean isAutoUpdateEnabled() {
    return isAutoUpdate();
  }

  public String[] getAllCurrentSettings() {
    return new String[] {
      getCurrentLanguage(),
      getCurrentAudioQuality(),
      getCurrentService(),
      String.valueOf(isAutoUpdate()),
      String.valueOf(isLoadPlaylistArt()),
      getThemeColor(),
      getBackgroundColor()
    };
  }

  public void saveSettings(
      String language,
      String audioQuality,
      String service,
      String autoUpdate,
      String loadPlaylistArt,
      String themeColor,
      String backgroundColor) {
    setCurrentLanguage(language);
    setCurrentAudioQuality(audioQuality);
    setCurrentService(service);
    setAutoUpdate("true".equals(autoUpdate));
    setLoadPlaylistArt("true".equals(loadPlaylistArt));
    setThemeColor(themeColor);
    setBackgroundColor(backgroundColor);
  }

  public void savePlayerSettings(int volumeLevel, int repeatMode, boolean shuffleMode) {
    setVolumeLevel(volumeLevel);
    setRepeatMode(repeatMode);
    setShuffleMode(shuffleMode);
  }

  public int[] getNctMenuOrder(int totalItems) {
    if (nctMenuOrder == null) {
      nctMenuOrder = new int[totalItems];
      for (int i = 0; i < totalItems; i++) {
        nctMenuOrder[i] = i;
      }
    }
    return nctMenuOrder;
  }

  public int[] getSoundcloudMenuOrder(int totalItems) {
    if (soundcloudMenuOrder == null) {
      soundcloudMenuOrder = new int[totalItems];
      for (int i = 0; i < totalItems; i++) {
        soundcloudMenuOrder[i] = i;
      }
    }
    return soundcloudMenuOrder;
  }

  public boolean[] getNctMenuVisibility(int totalItems) {
    if (nctMenuVisibility == null) {
      nctMenuVisibility = new boolean[totalItems];
      for (int i = 0; i < totalItems; i++) {
        nctMenuVisibility[i] = true;
      }
    }
    return nctMenuVisibility;
  }

  public boolean[] getSoundcloudMenuVisibility(int totalItems) {
    if (soundcloudMenuVisibility == null) {
      soundcloudMenuVisibility = new boolean[totalItems];
      for (int i = 0; i < totalItems; i++) {
        soundcloudMenuVisibility[i] = true;
      }
    }
    return soundcloudMenuVisibility;
  }

  public void saveNctMenuOrder(int[] order) {
    this.nctMenuOrder = order;
    markModifiedAndSave(CATEGORY_MENU);
  }

  public void saveSoundcloudMenuOrder(int[] order) {
    this.soundcloudMenuOrder = order;
    markModifiedAndSave(CATEGORY_MENU);
  }

  public void saveNctMenuVisibility(boolean[] visibility) {
    this.nctMenuVisibility = visibility;
    markModifiedAndSave(CATEGORY_MENU);
  }

  public void saveSoundcloudMenuVisibility(boolean[] visibility) {
    this.soundcloudMenuVisibility = visibility;
    markModifiedAndSave(CATEGORY_MENU);
  }

  public int getVolumeLevel() {
    return volumeLevel;
  }

  public void setVolumeLevel(int volumeLevel) {
    if (this.volumeLevel != volumeLevel) {
      this.volumeLevel = volumeLevel;
      markModifiedAndSave(CATEGORY_PLAYER);
    }
  }

  public int getRepeatMode() {
    return repeatMode;
  }

  public void setRepeatMode(int repeatMode) {
    if (this.repeatMode != repeatMode) {
      this.repeatMode = repeatMode;
      markModifiedAndSave(CATEGORY_PLAYER);
    }
  }

  public boolean isShuffleMode() {
    return shuffleMode;
  }

  public void setShuffleMode(boolean shuffleMode) {
    if (this.shuffleMode != shuffleMode) {
      this.shuffleMode = shuffleMode;
      markModifiedAndSave(CATEGORY_PLAYER);
    }
  }

  public int getSearchTypeIndex() {
    return searchTypeIndex;
  }

  public void setSearchTypeIndex(int searchTypeIndex) {
    if (this.searchTypeIndex != searchTypeIndex) {
      this.searchTypeIndex = searchTypeIndex;
      markModifiedAndSave(CATEGORY_SEARCH);
    }
  }

  private void markModifiedAndSave(int category) {
    settingsModified = true;
    saveSettingsAsync(category);
  }

  private void saveSettingsAsync(final int category) {
    if (isShuttingDown) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastSaveTime < SAVE_THROTTLE && saveThread != null && saveThread.isAlive()) {
      return;
    }

    lastSaveTime = currentTime;

    if (saveThread != null && saveThread.isAlive()) {
      saveThread.interrupt();
    }

    final JSONObject config = createConfigForCategory(category);

    saveThread =
        ThreadManager.createThread(
            new Runnable() {
              public void run() {
                try {
                  if (isShuttingDown) {
                    return;
                  }
                  settingsStore.saveSettings(category, config);
                } catch (Exception e) {

                } finally {
                  synchronized (SettingsManager.this) {
                    if (Thread.currentThread() == saveThread) {
                      saveThread = null;
                    }
                  }
                }
              }
            },
            "SettingsSaver");
    ThreadManager.safeStartThread(saveThread);
  }

  private JSONObject createConfigForCategory(int category) {
    JSONObject config = new JSONObject();
    try {
      if (category == CATEGORY_APP) {
        config.put("language", language);
        config.put("audioQuality", audioQuality);
        config.put("service", service);
        config.put("autoUpdate", autoUpdate);
        config.put("loadPlaylistArt", loadPlaylistArt);
        config.put("themeColor", themeColor);
        config.put("backgroundColor", backgroundColor);
      } else if (category == CATEGORY_PLAYER) {
        config.put("volumeLevel", volumeLevel);
        config.put("repeatMode", repeatMode);
        config.put("shuffleMode", shuffleMode);
      } else if (category == CATEGORY_SEARCH) {
        config.put("searchTypeIndex", searchTypeIndex);
      } else if (category == CATEGORY_MENU) {
        saveMenuArraysToJson(config);
      }
    } catch (Exception e) {
    }
    return config;
  }

  private void saveMenuArraysToJson(JSONObject config) {
    try {

      if (nctMenuOrder != null) {
        config.put("nctMenuOrder", arrayToString(nctMenuOrder));
      }

      if (nctMenuVisibility != null) {
        config.put("nctMenuVisibility", arrayToString(nctMenuVisibility));
      }

      if (soundcloudMenuOrder != null) {
        config.put("soundcloudMenuOrder", arrayToString(soundcloudMenuOrder));
      }

      if (soundcloudMenuVisibility != null) {
        config.put("soundcloudMenuVisibility", arrayToString(soundcloudMenuVisibility));
      }
    } catch (Exception e) {
    }
  }

  private String arrayToString(int[] array) {
    if (array == null || array.length == 0) {
      return "";
    }

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < array.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(array[i]);
    }
    return sb.toString();
  }

  private String arrayToString(boolean[] array) {
    if (array == null || array.length == 0) {
      return "";
    }

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < array.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(array[i]);
    }
    return sb.toString();
  }

  public synchronized void shutdown() {
    isShuttingDown = true;

    if (saveThread != null && saveThread.isAlive()) {
      try {
        saveThread.interrupt();
      } catch (Exception e) {
      }
      saveThread = null;
    }

    isShuttingDown = false;
  }
}
