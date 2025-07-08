package app.common;

import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONArray;
import org.json.me.JSONObject;

public class MenuSettingsManager {
  private static MenuSettingsManager instance;
  private static final String MENU_SETTINGS_STORE_NAME = "menu_settings";

  public static synchronized MenuSettingsManager getInstance() {
    if (instance == null) {
      instance = new MenuSettingsManager();
    }
    return instance;
  }

  private final ReadWriteRecordStore recordStore;
  private Thread saveThread = null;
  private boolean isShuttingDown = false;

  private int[] nctMenuOrder = null;
  private int[] soundcloudMenuOrder = null;

  private MenuSettingsManager() {
    recordStore = new ReadWriteRecordStore(MENU_SETTINGS_STORE_NAME);
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

        if (settings.has("nctMenuOrder")) {
          JSONArray nctOrderArray = settings.getJSONArray("nctMenuOrder");
          nctMenuOrder = new int[nctOrderArray.length()];
          for (int i = 0; i < nctOrderArray.length(); i++) {
            nctMenuOrder[i] = Integer.parseInt(nctOrderArray.get(i).toString());
          }
        }

        if (settings.has("soundcloudMenuOrder")) {
          JSONArray scOrderArray = settings.getJSONArray("soundcloudMenuOrder");
          soundcloudMenuOrder = new int[scOrderArray.length()];
          for (int i = 0; i < scOrderArray.length(); i++) {
            soundcloudMenuOrder[i] = Integer.parseInt(scOrderArray.get(i).toString());
          }
        }

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

    nctMenuOrder = null;
    soundcloudMenuOrder = null;
  }

  public void saveNctMenuOrder(int[] order) {
    this.nctMenuOrder = order;
    saveSettings();
  }

  public void saveSoundcloudMenuOrder(int[] order) {
    this.soundcloudMenuOrder = order;
    saveSettings();
  }

  private void saveSettings() {
    JSONObject settings = new JSONObject();
    try {
      if (nctMenuOrder != null) {
        JSONArray nctArray = new JSONArray();
        for (int i = 0; i < nctMenuOrder.length; i++) {
          nctArray.put(nctMenuOrder[i]);
        }
        settings.put("nctMenuOrder", nctArray);
      }

      if (soundcloudMenuOrder != null) {
        JSONArray scArray = new JSONArray();
        for (int i = 0; i < soundcloudMenuOrder.length; i++) {
          scArray.put(soundcloudMenuOrder[i]);
        }
        settings.put("soundcloudMenuOrder", scArray);
      }
    } catch (Exception e) {
    }

    saveConfig(settings);
  }

  public int[] getNctMenuOrder(int size) {
    if (nctMenuOrder != null && nctMenuOrder.length == size) {
      return nctMenuOrder;
    }

    int[] defaultOrder = new int[size];
    for (int i = 0; i < size; i++) {
      defaultOrder[i] = i;
    }
    return defaultOrder;
  }

  public int[] getSoundcloudMenuOrder(int size) {
    if (soundcloudMenuOrder != null && soundcloudMenuOrder.length == size) {
      return soundcloudMenuOrder;
    }

    int[] defaultOrder = new int[size];
    for (int i = 0; i < size; i++) {
      defaultOrder[i] = i;
    }
    return defaultOrder;
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

                  synchronized (MenuSettingsManager.this) {
                    if (Thread.currentThread() == saveThread) {
                      saveThread = null;
                    }
                  }
                }
              }
            });
    saveThread.start();
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
