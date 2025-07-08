package app.common;

import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class PlayerSettingsManager {
  private static PlayerSettingsManager instance;
  private static final String PLAYER_SETTINGS_STORE_NAME = "player_settings";

  public static synchronized PlayerSettingsManager getInstance() {
    if (instance == null) {
      instance = new PlayerSettingsManager();
    }
    return instance;
  }

  private final ReadWriteRecordStore recordStore;
  private Thread saveThread = null;
  private final Thread loadThread = null;
  private boolean isShuttingDown = false;

  private int volumeLevel = 100;
  private int repeatMode = 2;
  private boolean shuffleMode = false;

  private PlayerSettingsManager() {
    recordStore = new ReadWriteRecordStore(PLAYER_SETTINGS_STORE_NAME);
    loadSettingsFromRMS();
  }

  private void loadSettingsFromRMS() {
    try {
      recordStore.openRecStore();
      RecordEnumeration re = recordStore.enumerateRecords(null, null, false);
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
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
      }
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

  public boolean getShuffleMode() {
    return this.shuffleMode;
  }

  public synchronized void saveConfig(final JSONObject config) {
    if (saveThread != null && saveThread.isAlive()) {
      saveThread.interrupt();
    }

    saveThread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  if (isShuttingDown) {
                    return;
                  }

                  recordStore.openRecStore();
                  byte[] recordBytes = config.toString().getBytes("UTF-8");

                  RecordEnumeration re = recordStore.enumerateRecords(null, null, false);
                  if (re.hasNextElement()) {
                    int recordId = re.nextRecordId();
                    recordStore.setRecord(recordId, recordBytes, 0, recordBytes.length);
                  } else {
                    recordStore.writeRecord(config.toString());
                  }
                  re.destroy();
                } catch (Exception e) {
                } finally {
                  try {
                    recordStore.closeRecStore();
                  } catch (Exception e) {
                  }

                  synchronized (PlayerSettingsManager.this) {
                    if (Thread.currentThread() == saveThread) {
                      saveThread = null;
                    }
                  }
                }
              }
            });
    saveThread.start();
  }

  public synchronized JSONObject loadConfigSync() {
    JSONObject config = new JSONObject();
    try {
      recordStore.openRecStore();
      RecordEnumeration re = recordStore.enumerateRecords(null, null, false);
      if (re.hasNextElement()) {
        byte[] recordBytes = re.nextRecord();
        String configJson = new String(recordBytes);
        config = new JSONObject(configJson);
        re.destroy();
      }
    } catch (Exception e) {
    } finally {
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
      }
    }
    return config;
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
