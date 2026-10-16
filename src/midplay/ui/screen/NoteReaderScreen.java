package midplay.ui.screen;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;

public final class NoteReaderScreen extends BaseForm {
  private final int noteId;
  private final Runnable onClose;
  private final StringItem content;
  private final Command editCommand;

  public NoteReaderScreen(Note note, Navigator navigator, Runnable onClose) {
    super(note.title, navigator);
    noteId = note.id;
    this.onClose = onClose;
    content = new StringItem(null, note.text);
    editCommand = new Command(Lang.tr("note.edit"), Command.SCREEN, 1);
    append(content);
    addCommand(editCommand);
    addCommand(Commands.playerNowPlaying());
  }

  protected void handleCommand(Command command, Displayable displayable) {
    if (command == editCommand) {
      Note note = NotesManager.getInstance().getNote(noteId);
      if (note != null) {
        navigator.forward(new NoteEditorScreen(note, navigator, refreshReader()));
      }
    } else if (command == Commands.playerNowPlaying()) {
      PlayerNavHelper.showNowPlaying(navigator);
    }
  }

  private Runnable refreshReader() {
    return new Runnable() {
      public void run() {
        Note current = NotesManager.getInstance().getNote(noteId);
        if (current != null) {
          content.setText(current.text);
          setTitle(current.title);
        }
        if (onClose != null) {
          onClose.run();
        }
      }
    };
  }
}
