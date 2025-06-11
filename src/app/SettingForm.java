package app;

import java.util.Vector;
import javax.microedition.lcdui.*;
import app.common.ReadWriteRecordStore;
import app.model.Song;
import app.utils.Utils;

public class SettingForm extends Form implements Utils.BreadCrumbTrail, CommandListener {

    private static final String[] LANGUAGES = {"Tiếng Việt", "English"};
    private static final String[] AUDIO_QUALITIES = {"128kbps", "320kbps"};

    private ChoiceGroup languageChoice;
    private ChoiceGroup audioQualityChoice;
    private Command backCommand;
    private Command saveCommand;
    private final Utils.BreadCrumbTrail parent;

    public SettingForm(String title, Utils.BreadCrumbTrail parent) {
        super(title);
        this.parent = parent;

        this.backCommand = new Command("Trở lại", Command.BACK, 1);
        this.saveCommand = new Command("Lưu", Command.SCREEN, 2);

        languageChoice = new ChoiceGroup("Ngôn ngữ / Language", Choice.EXCLUSIVE);
        for (int i = 0; i < LANGUAGES.length; i++) {
            languageChoice.append(LANGUAGES[i], null);
        }

        audioQualityChoice = new ChoiceGroup("Chất lượng âm thanh / Audio Quality", Choice.EXCLUSIVE);
        for (int i = 0; i < AUDIO_QUALITIES.length; i++) {
            audioQualityChoice.append(AUDIO_QUALITIES[i], null);
        }

        append(languageChoice);
        append(audioQualityChoice);

        addCommand(backCommand);
        addCommand(saveCommand);
        setCommandListener(this);

        loadSettings();
    }

    private void saveSettings() {
        try {

            ReadWriteRecordStore storeToDelete = new ReadWriteRecordStore();
            storeToDelete.deleteRecStore();

            ReadWriteRecordStore recordStore = new ReadWriteRecordStore();
            recordStore.openRecStore();

            String settings = getSelectedLanguage() + "|" + getSelectedAudioQuality();
            recordStore.writeRecord(settings);
            recordStore.closeRecStore();

            parent.goBack();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        ReadWriteRecordStore recordStore = new ReadWriteRecordStore();
        recordStore.openRecStore();
        Vector records = recordStore.readRecords();
        recordStore.closeRecStore();

        if (records != null && !records.isEmpty()) {
            String settings = (String) records.elementAt(0);
            int separatorIndex = settings.indexOf('|');
            if (separatorIndex != -1) {
                String language = settings.substring(0, separatorIndex);
                String audioQuality = settings.substring(separatorIndex + 1);

                for (int i = 0; i < LANGUAGES.length; i++) {
                    if (LANGUAGES[i].equals(language)) {
                        languageChoice.setSelectedIndex(i, true);
                        break;
                    }
                }

                for (int i = 0; i < AUDIO_QUALITIES.length; i++) {
                    if (AUDIO_QUALITIES[i].equals(audioQuality)) {
                        audioQualityChoice.setSelectedIndex(i, true);
                        break;
                    }
                }
            }
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (d == this) {
            if (c == backCommand) {
                goBack();
            } else if (c == saveCommand) {
                saveSettings();
            }
        }
    }

    public String getSelectedLanguage() {
        return LANGUAGES[languageChoice.getSelectedIndex()];
    }

    public String getSelectedAudioQuality() {
        return AUDIO_QUALITIES[audioQualityChoice.getSelectedIndex()];
    }

    // Implement Utils.BreadCrumbTrail methods
    public Displayable go(Displayable d) {
        return parent.go(d);
    }

    public Displayable goBack() {
        return parent.goBack();
    }

    public void handle(Song song) {

    }

    public Displayable replaceCurrent(Displayable d) {
        return parent.replaceCurrent(d);
    }

    public Displayable getCurrentDisplayable() {
        return parent.getCurrentDisplayable();
    }
}
