package midplay;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.ui.Commands;
import midplay.util.Lang;
import mpgram.MP;

/** Imports an MPGram session code from a saved MIDPlay note. */
public final class MPGramSessionNotePicker extends List implements CommandListener {
  private final Note[] notes;

  public MPGramSessionNotePicker() {
    super(Lang.tr("mpgram.import_session_note"), List.IMPLICIT);
    notes = NotesManager.getInstance().getNotes();
    for (int i = 0; i < notes.length; i++) append(notes[i].title, null);
    addCommand(Commands.back());
    setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == List.SELECT_COMMAND) {
      int index = getSelectedIndex();
      if (index >= 0 && index < notes.length) {
        MP.importSessionFromText(notes[index].text);
      }
    } else if (command == Commands.back()) {
      MP.cancelSessionImport();
    }
  }
}
