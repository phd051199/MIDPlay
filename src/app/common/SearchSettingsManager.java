package app.common;

import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class SearchSettingsManager {
  private static SearchSettingsManager instance;
  private static final String SEARCH_SETTINGS_STORE_NAME = "search_settings";

  public static synchronized SearchSettingsManager getInstance() {
    if (instance == null) {
      instance = new SearchSettingsManager();
    }
    return instance;
  }

  private final ReadWriteRecordStore recordStore;
  private Thread saveThread = null;
  private final Thread loadThread = null;
  private boolean isShuttingDown = false;

  private int searchTypeIndex = 0;

  private SearchSettingsManager() {
    recordStore = new ReadWriteRecordStore(SEARCH_SETTINGS_STORE_NAME);
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

        this.searchTypeIndex = settings.optInt("searchTypeIndex", 0);

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
    this.searchTypeIndex = 0;
  }

  public void saveSearchSettings(int searchTypeIndex) {
    this.searchTypeIndex = searchTypeIndex;

    JSONObject settings = new JSONObject();
    try {
      settings.put("searchTypeIndex", this.searchTypeIndex);
    } catch (Exception e) {
    }

    saveConfig(settings);
  }

  public int getSearchTypeIndex() {
    return this.searchTypeIndex;
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

                  synchronized (SearchSettingsManager.this) {
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
