package app.common;

import app.utils.I18N;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class SettingManager {
  private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};
  private static final String[] AVAILABLE_SERVICES = {"nct", "soundcloud"};
  private static SettingManager instance;

  public static synchronized SettingManager getInstance() {
    if (instance == null) {
      instance = new SettingManager();
    }
    return instance;
  }

  private final ReadWriteRecordStore recordStore;

  private String language;
  private String audioQuality;
  private String service;
  private boolean autoUpdate;

  private SettingManager() {
    recordStore = new ReadWriteRecordStore("settings");

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

        this.language = settings.optString("language", I18N.getLanguage());
        this.audioQuality = settings.optString("audioQuality", AUDIO_QUALITIES[0]);
        this.service = settings.optString("service", AVAILABLE_SERVICES[0]);
        this.autoUpdate = settings.optBoolean("autoUpdate", true);

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
    this.language = I18N.getLanguage();
    this.audioQuality = AUDIO_QUALITIES[0];
    this.service = AVAILABLE_SERVICES[0];
    this.autoUpdate = true;
  }

  public String[] getAudioQualities() {
    return AUDIO_QUALITIES;
  }

  public String[] getAvailableServices() {
    return AVAILABLE_SERVICES;
  }

  public void saveSettings(String language, String audioQuality, String service) {
    saveSettings(language, audioQuality, service, "true");
  }

  public void saveSettings(
      String language, String audioQuality, String service, String autoUpdate) {
    this.language = language;
    this.audioQuality = audioQuality;
    this.service = service;
    this.autoUpdate = "true".equals(autoUpdate);
    try {
      recordStore.openRecStore();
      JSONObject settings = new JSONObject();
      settings.put("language", this.language);
      settings.put("audioQuality", this.audioQuality);
      settings.put("service", this.service);
      settings.put("autoUpdate", this.autoUpdate);

      byte[] recordBytes = settings.toString().getBytes("UTF-8");

      RecordEnumeration re = recordStore.enumerateRecords(null, null, false);
      if (re.hasNextElement()) {

        int recordId = re.nextRecordId();
        recordStore.setRecord(recordId, recordBytes, 0, recordBytes.length);
      } else {

        recordStore.writeRecord(settings.toString());
      }
      re.destroy();
    } catch (Exception e) {
    } finally {
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
      }
    }
  }

  public String[] getAllCurrentSettings() {
    return new String[] {
      this.language, this.audioQuality, this.service, this.autoUpdate ? "true" : "false"
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
}
