package app.core.settings;

import app.core.storage.RecordStoreManager;
import app.core.threading.ThreadManager;
import app.core.threading.ThreadManagerIntegration;
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

  private final RecordStoreManager recordStore;
  private final Thread loadThread = null;
  private boolean isShuttingDown = false;

  private int searchTypeIndex = 0;

  private SearchSettingsManager() {
    recordStore = new RecordStoreManager(SEARCH_SETTINGS_STORE_NAME);
    loadSettingsFromRMS();
  }

  private void loadSettingsFromRMS() {
    try {
      RecordEnumeration re = recordStore.enumerateRecords();
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
      recordStore.closeRecordStore();
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
    ThreadManager.getInstance().interruptThread("SearchSettingsSave");

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
              try {
                recordStore.closeRecordStore();
              } catch (Exception e) {
              }
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
        if (recordBytes != null && recordBytes.length > 0) {
          String configJson = new String(recordBytes);
          if (configJson.trim().length() > 0) {
            config = new JSONObject(configJson);
          }
        }
      }
    } catch (Exception e) {
    } finally {
      if (re != null) {
        try {
          re.destroy();
        } catch (Exception e) {
        }
      }
      try {
        recordStore.closeRecordStore();
      } catch (Exception e) {
      }
    }
    return config;
  }

  public synchronized void shutdown() {
    isShuttingDown = true;

    ThreadManager.getInstance().interruptThread("SearchSettingsSave");

    recordStore.closeRecordStore();
  }
}
