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

/** Selects a saved MIDPlay note and sends its text to the active MPGram chat. */
public final class MPGramNotePicker extends List implements CommandListener {
  private final String peerId;
  private final String topMessageId;
  private final Note[] notes;

  public MPGramNotePicker(String peerId, String topMessageId) {
    super(Lang.tr("mpgram.send_note"), List.IMPLICIT);
    this.peerId = peerId;
    this.topMessageId = topMessageId;
    notes = NotesManager.getInstance().getNotes();
    for (int i = 0; i < notes.length; i++) {
      append(notes[i].title, null);
    }
    addCommand(Commands.back());
    setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == List.SELECT_COMMAND) {
      int index = getSelectedIndex();
      if (index >= 0 && index < notes.length) {
        MP.sendNoteToChat(peerId, topMessageId, notes[index].text);
      }
    } else if (command == Commands.back()) {
      MP.returnToCurrentChat();
    }
  }
}
