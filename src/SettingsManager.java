import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;

public class SettingsManager {
  private static final int SETTINGS_ID = 1;
  private static SettingsManager instance;
  private static String currentLanguage;
  private static String currentService;
  private static String currentQuality;
  private static String currentSearchType;
  private static int currentAutoUpdate;
  private static String currentPlayerMethod;
  private static int currentRepeatMode;
  private static int currentShuffleMode;
  private static int currentVolumeLevel;

  public static SettingsManager getInstance() {
    if (instance == null) {
      instance = new SettingsManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;

  private SettingsManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_SETTINGS);
  }

  public String getDefaultPlayerMethod() {
    return PlayerGUI.getDefaultPlayerHttpMethod();
  }

  public void loadSettings() {
    JSONObject settings = getSettingsJSON();
    currentLanguage = settings.getString("language", "en");
    currentService = settings.getString("service", Configuration.SERVICE_NCT);
    currentQuality = settings.getString("quality", Configuration.QUALITY_128);
    currentSearchType = settings.getString("searchType", Configuration.SEARCH_PLAYLIST);
    currentAutoUpdate = settings.getInt("autoUpdate", Configuration.AUTO_UPDATE_ENABLED);
    currentPlayerMethod = settings.getString("playerMethod", getDefaultPlayerMethod());
    currentRepeatMode = settings.getInt("repeatMode", Configuration.PLAYER_REPEAT_ALL);
    currentShuffleMode = settings.getInt("shuffleMode", Configuration.PLAYER_SHUFFLE_OFF);
    currentVolumeLevel = settings.getInt("volumeLevel", Configuration.PLAYER_MAX_VOLUME);
    Lang.setLang(currentLanguage);
  }

  private JSONObject getSettingsJSON() {
    try {
      if (storage.recordExists(SETTINGS_ID)) {
        String jsonString = storage.getRecordAsString(SETTINGS_ID);
        return JSON.getObject(jsonString);
      }
    } catch (RecordStoreException e) {
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
    return settings;
  }

  private void ensureSettingsRecordExists() throws RecordStoreException {
    if (!storage.recordExists(SETTINGS_ID)) {
      JSONObject defaultSettings = createDefaultSettings();
      while (storage.getNumRecords() < SETTINGS_ID) {
        if (storage.getNumRecords() == SETTINGS_ID - 1) {
          storage.addRecord(defaultSettings.toString());
        } else {
          storage.addRecord("");
        }
      }
    }
  }

  private void saveSetting(String key, String value) throws RecordStoreException {
    ensureSettingsRecordExists();
    JSONObject settings = getSettingsJSON();
    settings.put(key, value);
    storage.setRecord(SETTINGS_ID, settings.toString());
  }

  private void saveSetting(String key, int value) throws RecordStoreException {
    ensureSettingsRecordExists();
    JSONObject settings = getSettingsJSON();
    settings.put(key, value);
    storage.setRecord(SETTINGS_ID, settings.toString());
  }

  public void saveLanguage(String langCode) throws RecordStoreException {
    saveSetting("language", langCode);
    currentLanguage = langCode;
    setCurrentLanguage(langCode);
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

  public String getCurrentLanguage() {
    return currentLanguage;
  }

  public void setCurrentLanguage(String langCode) {
    currentLanguage = langCode;
    Lang.setLang(langCode);
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

  public void cleanup() {
    storage.closeRecordStore();
  }
}
