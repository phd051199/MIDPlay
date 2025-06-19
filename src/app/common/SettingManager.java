package app.common;

import app.utils.I18N;
import java.util.Vector;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class SettingManager {
  private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};
  private static final String[] AVAILABLE_SERVICES = {"nct", "soundcloud"};
  private static SettingManager instance;
  private ReadWriteRecordStore recordStore;

  private SettingManager() {
    recordStore = new ReadWriteRecordStore("settings");
  }

  public static synchronized SettingManager getInstance() {
    if (instance == null) {
      instance = new SettingManager();
    }
    return instance;
  }

  public String[] getAudioQualities() {
    return AUDIO_QUALITIES;
  }

  public String[] getAvailableServices() {
    return AVAILABLE_SERVICES;
  }

  public void saveSettings(String language, String audioQuality, String service) throws Exception {
    try {
      recordStore.deleteRecStore();
      recordStore.openRecStore();

      JSONObject settings = new JSONObject();
      settings.put("language", language);
      settings.put("audioQuality", audioQuality);
      settings.put("service", service);

      recordStore.writeRecord(settings.toString());
    } finally {
      recordStore.closeRecStore();
    }
  }

  public String[] loadSettings() throws Exception {
    String[] result = new String[3];

    try {
      recordStore.openRecStore();
      Vector records = recordStore.readRecords();

      if (records != null && !records.isEmpty()) {
        String settingsJson = (String) records.elementAt(0);
        try {
          JSONObject settings = new JSONObject(settingsJson);

          try {
            result[0] = settings.getString("language");
          } catch (JSONException e) {
            result[0] = null;
          }
          try {
            result[1] = settings.getString("audioQuality");
          } catch (JSONException e) {
            result[1] = null;
          }
          try {
            result[2] = settings.getString("service");
          } catch (JSONException e) {
            result[2] = null;
          }
        } catch (JSONException e) {

          result[0] = null;
          result[1] = null;
        }
      }
    } finally {
      recordStore.closeRecStore();
    }

    if (result[0] == null || result[0].length() == 0) {
      result[0] = I18N.getLanguage();
    }
    if (result[1] == null || result[1].length() == 0) {
      result[1] = AUDIO_QUALITIES[0];
    }
    if (result[2] == null || result[2].length() == 0) {
      result[2] = AVAILABLE_SERVICES[0];
    }

    return result;
  }

  public String getCurrentAudioQuality() {
    try {
      String[] settings = loadSettings();
      return settings[1];
    } catch (Exception e) {
      return AUDIO_QUALITIES[0];
    }
  }

  public String getCurrentService() {
    try {
      String[] settings = loadSettings();
      return settings[2];
    } catch (Exception e) {
      return AVAILABLE_SERVICES[0];
    }
  }
}
