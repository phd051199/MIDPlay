package app.common;

import app.utils.I18N;
import java.util.Vector;

public class SettingManager {
  private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};
  private static SettingManager instance;
  private ReadWriteRecordStore recordStore;

  private SettingManager() {
    recordStore = new ReadWriteRecordStore();
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

  public void saveSettings(String language, String audioQuality) throws Exception {
    recordStore.deleteRecStore();
    recordStore.openRecStore();
    String settings = language + "|" + audioQuality;
    recordStore.writeRecord(settings);
    recordStore.closeRecStore();
  }

  public String[] loadSettings() throws Exception {
    recordStore.openRecStore();
    Vector records = recordStore.readRecords();
    recordStore.closeRecStore();

    String[] result = new String[2];

    if (records != null && !records.isEmpty()) {
      String settings = (String) records.elementAt(0);
      int separatorIndex = settings.indexOf('|');

      if (separatorIndex != -1) {
        result[0] = settings.substring(0, separatorIndex);
        if (settings.length() > separatorIndex + 1) {
          result[1] = settings.substring(separatorIndex + 1);
        }
      }
    }

    if (result[0] == null) {
      result[0] = I18N.getLanguage();
    }
    if (result[1] == null) {
      result[1] = AUDIO_QUALITIES[0];
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
}
