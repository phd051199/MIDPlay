package app.core.settings;

import app.constants.Services;
import app.core.storage.RecordStoreManager;
import app.core.threading.ThreadManager;
import app.core.threading.ThreadManagerIntegration;
import app.utils.I18N;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class SettingsManager {
  private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};
  private static final String[] AVAILABLE_SERVICES = {
    Services.NCT, Services.SOUNDCLOUD, Services.YTMUSIC, Services.SPOTIFY
  };
  private static SettingsManager instance;

  private static final String SETTINGS_STORE_NAME = "settings";
  private static final long SAVE_THROTTLE = 2000;

  public static synchronized SettingsManager getInstance() {
    if (instance == null) {
      instance = new SettingsManager();
    }
    return instance;
  }

  private final RecordStoreManager recordStore;
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
  private String lastThemeColor = "";
  private String lastBackgroundColor = "";

  private SettingsManager() {
    recordStore = new RecordStoreManager(SETTINGS_STORE_NAME);
    loadSettingsFromRMS();
  }

  private void loadSettingsFromRMS() {
    try {
      recordStore.openRecordStore();
      RecordEnumeration re = recordStore.enumerateRecords();
      if (re.hasNextElement()) {
        byte[] recordBytes = re.nextRecord();
        String settingsJson = new String(recordBytes);
        JSONObject settings = new JSONObject(settingsJson);

        this.language = settings.optString("language", I18N.getLanguage());
        this.audioQuality = settings.optString("audioQuality", AUDIO_QUALITIES[0]);
        this.service = settings.optString("service", AVAILABLE_SERVICES[0]);
        this.autoUpdate = settings.optBoolean("autoUpdate", true);
        this.loadPlaylistArt = settings.optBoolean("loadPlaylistArt", true);
        this.themeColor = settings.optString("themeColor", "410A4A");
        this.backgroundColor = settings.optString("backgroundColor", "F0F0F0");

        re.destroy();
      } else {
        setDefaultSettings();
      }
    } catch (Exception e) {
      setDefaultSettings();
    } finally {
      try {
        recordStore.closeRecordStore();
      } catch (Exception e) {
      }
    }
  }

  private void setDefaultSettings() {
    this.language = I18N.getLanguage();
    this.audioQuality = AUDIO_QUALITIES[0];
    this.service = AVAILABLE_SERVICES[0];
    this.autoUpdate = true;
    this.loadPlaylistArt = true;
    this.themeColor = "410A4A";
    this.backgroundColor = "F0F0F0";
  }

  public String[] getAudioQualities() {
    return AUDIO_QUALITIES;
  }

  public String[] getAvailableServices() {
    return AVAILABLE_SERVICES;
  }

  public void saveSettings(
      String language,
      String audioQuality,
      String service,
      String autoUpdate,
      String loadPlaylistArt,
      String themeColor,
      String backgroundColor) {
    this.language = language;
    this.audioQuality = audioQuality;
    this.service = service;
    this.autoUpdate = "true".equals(autoUpdate);
    this.loadPlaylistArt = "true".equals(loadPlaylistArt);
    this.themeColor = themeColor;
    this.backgroundColor = backgroundColor;

    JSONObject settings = new JSONObject();
    try {
      settings.put("language", this.language);
      settings.put("audioQuality", this.audioQuality);
      settings.put("service", this.service);
      settings.put("autoUpdate", this.autoUpdate);
      settings.put("loadPlaylistArt", this.loadPlaylistArt);
      settings.put("themeColor", this.themeColor);
      settings.put("backgroundColor", this.backgroundColor);
    } catch (Exception e) {
    }

    saveConfig(settings);
  }

  public synchronized void saveConfig(final JSONObject config) {
    if (ThreadManager.getInstance().isThreadAlive("SettingsSave")) {
      settingsModified = true;
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastSaveTime < SAVE_THROTTLE) {
      settingsModified = true;

      ThreadManagerIntegration.scheduleDelayedTask(
          new Runnable() {
            public void run() {
              if (settingsModified) {
                performSave(config);
              }
            }
          },
          "SettingsSave",
          SAVE_THROTTLE);
      return;
    }

    performSave(config);
  }

  private void performSave(final JSONObject config) {
    ThreadManagerIntegration.executeSettingsSave(
        new Runnable() {
          public void run() {
            settingsModified = false;
            lastSaveTime = System.currentTimeMillis();

            try {
              if (isShuttingDown) {
                return;
              }

              RecordEnumeration re = recordStore.enumerateRecords();
              if (re.hasNextElement()) {
                int recordId = re.nextRecordId();
                recordStore.setRecord(recordId, config.toString());
              } else {
                recordStore.addRecord(config.toString());
              }
              re.destroy();
            } catch (Exception e) {
            } finally {
              recordStore.closeRecordStore();
            }
          }
        });
  }

  public synchronized void loadConfig(final ConfigCallback callback) {

    ThreadManager.getInstance().interruptThread("SettingsLoad");

    ThreadManagerIntegration.executeBackgroundTask(
        new Runnable() {
          public void run() {
            try {
              if (isShuttingDown) {
                return;
              }

              RecordEnumeration re = recordStore.enumerateRecords();
              if (re.hasNextElement()) {
                byte[] recordBytes = re.nextRecord();
                String configJson = new String(recordBytes);
                final JSONObject config = new JSONObject(configJson);

                if (callback != null) {
                  callback.onConfigLoaded(config);
                }
                re.destroy();
              } else {
                if (callback != null) {
                  callback.onConfigLoaded(new JSONObject());
                }
              }
            } catch (Exception e) {
              if (callback != null) {
                callback.onConfigLoaded(new JSONObject());
              }
            } finally {
              recordStore.closeRecordStore();
            }
          }
        },
        "SettingsLoad");
  }

  public synchronized JSONObject loadConfigSync() {
    JSONObject config = new JSONObject();
    try {
      RecordEnumeration re = recordStore.enumerateRecords();
      if (re.hasNextElement()) {
        byte[] recordBytes = re.nextRecord();
        String configJson = new String(recordBytes);
        config = new JSONObject(configJson);
        re.destroy();
      }
    } catch (Exception e) {
    } finally {
      recordStore.closeRecordStore();
    }
    return config;
  }

  public synchronized void shutdown() {
    isShuttingDown = true;

    ThreadManager.getInstance().interruptThread("SettingsSave");
    ThreadManager.getInstance().interruptThread("SettingsLoad");

    recordStore.closeRecordStore();
  }

  public String[] getAllCurrentSettings() {
    return new String[] {
      this.language,
      this.audioQuality,
      this.service,
      this.autoUpdate ? "true" : "false",
      this.loadPlaylistArt ? "true" : "false",
      this.themeColor,
      this.backgroundColor
    };
  }

  public String getCurrentLanguage() {
    return this.language;
  }

  public String getCurrentAudioQuality() {
    return this.audioQuality;
  }

  public String getCurrentService() {
    return this.service;
  }

  public boolean isAutoUpdateEnabled() {
    return this.autoUpdate;
  }

  public boolean isLoadPlaylistArtEnabled() {
    return this.loadPlaylistArt;
  }

  public void setLoadPlaylistArt(boolean loadPlaylistArt) {
    this.loadPlaylistArt = loadPlaylistArt;
  }

  public String getThemeColor() {
    return this.themeColor;
  }

  public int getThemeColorRGB() {
    if (!themeColor.equals(lastThemeColor) || cachedThemeColorRGB == -1) {
      try {
        cachedThemeColorRGB = Integer.parseInt(themeColor, 16);
      } catch (Exception e) {
        cachedThemeColorRGB = 0x410A4A;
      }
      lastThemeColor = themeColor;
    }
    return cachedThemeColorRGB;
  }

  public String getBackgroundColor() {
    return this.backgroundColor;
  }

  public int getBackgroundColorRGB() {
    if (!backgroundColor.equals(lastBackgroundColor) || cachedBackgroundColorRGB == -1) {
      try {
        cachedBackgroundColorRGB = Integer.parseInt(backgroundColor, 16);
      } catch (Exception e) {
        cachedBackgroundColorRGB = 0xF0F0F0;
      }
      lastBackgroundColor = backgroundColor;
    }
    return cachedBackgroundColorRGB;
  }

  public interface ConfigCallback {
    void onConfigLoaded(JSONObject config);
  }
}
