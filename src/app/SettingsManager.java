package app;

import java.util.Vector;
import app.common.ReadWriteRecordStore;

public class SettingsManager {
    private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};
    
    public static String getAudioQuality() {
        try {
            ReadWriteRecordStore recordStore = new ReadWriteRecordStore();
            recordStore.openRecStore();
            Vector records = recordStore.readRecords();
            recordStore.closeRecStore();
            
            if (records != null && !records.isEmpty()) {
                String settings = (String) records.elementAt(0);
                int separatorIndex = settings.indexOf('|');
                if (separatorIndex != -1) {
                    String audioQuality = settings.substring(separatorIndex + 1);
                    return audioQuality;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return AUDIO_QUALITIES[0];
    }
}
