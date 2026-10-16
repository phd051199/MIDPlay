package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.MIDPlay;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;

public final class NoteEditorScreen extends TextBox implements CommandListener {
  private final Navigator navigator;
  private int noteId;
  private final Command clearCommand;
  private final Runnable onClose;
  private String savedText;

  public NoteEditorScreen(Note note, Navigator navigator, Runnable onClose) {
    super(note == null ? Lang.tr("note.new") : note.title, note == null ? "" : note.text, 4096, TextField.ANY);
    this.navigator = navigator;
    this.onClose = onClose;
    noteId = note == null ? -1 : note.id;
    savedText = getString();
    clearCommand = new Command(Lang.tr("note.clear"), Command.SCREEN, 2);
    addCommand(Commands.formSave());
    addCommand(clearCommand);
    addCommand(Commands.playerNowPlaying());
    addCommand(Commands.back());
    setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    try {
      if (command == Commands.back()) {
        if (hasUnsavedChanges()) {
          showSaveDiscardAlert();
        } else {
          closeEditor();
        }
      }
      else if (command == clearCommand) setString("");
      else if (command == Commands.formSave()) {
        saveNote();
        savedText = getString();
        navigator.showAlert(Lang.tr("note.saved"), AlertType.CONFIRMATION);
      } else if (command == Commands.playerNowPlaying()) PlayerNavHelper.showNowPlaying(navigator);
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void saveNote() throws Exception {
    if (noteId < 0) {
      Note saved = NotesManager.getInstance().create(getString());
      noteId = saved.id;
    } else {
      NotesManager.getInstance().update(noteId, getString());
    }
  }

  private boolean hasUnsavedChanges() {
    return !savedText.equals(getString());
  }

  private void showSaveDiscardAlert() {
    final Alert alert = new Alert(null, Lang.tr("note.save_changes"), null, AlertType.CONFIRMATION);
    final Command discard = new Command(Lang.tr("note.discard"), Command.CANCEL, 2);
    alert.addCommand(Commands.formSave());
    alert.addCommand(discard);
    alert.setTimeout(Alert.FOREVER);
    alert.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            try {
              if (command == Commands.formSave()) {
                saveNote();
                savedText = getString();
              }
              closeEditor();
            } catch (Exception e) {
              navigator.showAlert(e.toString(), AlertType.ERROR);
            }
          }
        });
    Display.getDisplay(MIDPlay.getInstance()).setCurrent(alert, this);
  }

  private void closeEditor() {
    if (onClose != null) {
      onClose.run();
    }
    navigator.back();
  }
}
