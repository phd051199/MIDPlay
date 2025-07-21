package app.core.settings;

import app.core.storage.RecordStoreManager;
import app.core.threading.ThreadManager;
import app.core.threading.ThreadManagerIntegration;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class PlayerSettingsManager {
  private static volatile PlayerSettingsManager instance;
  private static final Object instanceLock = new Object();
  private static final String PLAYER_SETTINGS_STORE_NAME = "player_settings";

  public static PlayerSettingsManager getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new PlayerSettingsManager();
        }
      }
    }
    return instance;
  }

  private final ThreadManager threadManager;
  private final RecordStoreManager recordStore;
  private final Thread loadThread = null;
  private boolean isShuttingDown = false;

  private int volumeLevel = 100;
  private int repeatMode = 2;
  private boolean shuffleMode = false;

  private PlayerSettingsManager() {
    threadManager = ThreadManager.getInstance();
    recordStore = new RecordStoreManager(PLAYER_SETTINGS_STORE_NAME);
    loadSettingsFromRMS();
  }

  private void loadSettingsFromRMS() {
    try {
      RecordEnumeration re = recordStore.enumerateRecords();
      if (re.hasNextElement()) {
        byte[] recordBytes = re.nextRecord();
        String settingsJson = new String(recordBytes);
        JSONObject settings = new JSONObject(settingsJson);

        this.volumeLevel = settings.optInt("volumeLevel", 100);
        this.repeatMode = settings.optInt("repeatMode", 2);
        this.shuffleMode = settings.optBoolean("shuffleMode", false);

        re.destroy();
      } else {
        setDefaultSettings();
      }
    } catch (Exception e) {
      setDefaultSettings();
    } finally {
      recordStore.closeRecordStore();
    }
  }

  private void setDefaultSettings() {
    this.volumeLevel = 100;
    this.repeatMode = 2;
    this.shuffleMode = false;
  }

  public void savePlayerSettings(int volumeLevel, int repeatMode, boolean shuffleMode) {
    this.volumeLevel = volumeLevel;
    this.repeatMode = repeatMode;
    this.shuffleMode = shuffleMode;

    JSONObject settings = new JSONObject();
    try {
      settings.put("volumeLevel", this.volumeLevel);
      settings.put("repeatMode", this.repeatMode);
      settings.put("shuffleMode", this.shuffleMode);
    } catch (Exception e) {
    }

    saveConfig(settings);
  }

  public int getVolumeLevel() {
    return this.volumeLevel;
  }

  public int getRepeatMode() {
    return this.repeatMode;
  }

  public boolean isShuffleMode() {
    return this.shuffleMode;
  }

  public synchronized void saveConfig(final JSONObject config) {
    threadManager.interruptThread("PlayerSettingsSave");

    ThreadManagerIntegration.executeSettingsSave(
        new Runnable() {
          public void run() {
            RecordEnumeration re = null;
            try {
              if (isShuttingDown) {
                return;
              }

              re = recordStore.enumerateRecords();
              if (re.hasNextElement()) {
                int recordId = re.nextRecordId();
                recordStore.setRecord(recordId, config.toString());
              } else {
                recordStore.addRecord(config.toString());
              }
            } catch (Exception e) {
            } finally {
              if (re != null) {
                try {
                  re.destroy();
                } catch (Exception e) {
                }
              }
              recordStore.closeRecordStore();
            }
          }
        });
  }

  public synchronized JSONObject loadConfigSync() {
    JSONObject config = new JSONObject();
    RecordEnumeration re = null;
    try {
      re = recordStore.enumerateRecords();
      if (re.hasNextElement()) {
        byte[] recordBytes = re.nextRecord();
        String configJson = new String(recordBytes);
        config = new JSONObject(configJson);
      }
    } catch (Exception e) {
    } finally {
      if (re != null) {
        try {
          re.destroy();
        } catch (Exception e) {
        }
      }
      recordStore.closeRecordStore();
    }
    return config;
  }

  public synchronized void shutdown() {
    isShuttingDown = true;

    threadManager.interruptThread("PlayerSettingsSave");

    recordStore.closeRecordStore();
  }
}
