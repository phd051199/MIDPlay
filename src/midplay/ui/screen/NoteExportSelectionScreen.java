package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class NoteExportSelectionScreen extends List implements CommandListener {
  private final Navigator navigator;
  private final Note[] notes;
  private final Command exportCommand;
  private final Command selectAllCommand;
  private final Command deselectAllCommand;

  public NoteExportSelectionScreen(Navigator navigator) {
    super(Lang.tr("note.export_select"), Choice.MULTIPLE);
    this.navigator = navigator;
    notes = NotesManager.getInstance().getNotes();
    for (int i = 0; i < notes.length; i++) append(notes[i].title, null);
    exportCommand = new Command(Lang.tr("note.export_selected"), Command.OK, 0);
    selectAllCommand = new Command(Lang.tr("note.select_all"), Command.SCREEN, 1);
    deselectAllCommand = new Command(Lang.tr("note.deselect_all"), Command.SCREEN, 2);
    addCommand(exportCommand);
    addCommand(selectAllCommand);
    addCommand(deselectAllCommand);
    addCommand(Commands.back());
    setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == Commands.back()) {
      navigator.back();
    } else if (command == selectAllCommand) {
      setAllSelected(true);
    } else if (command == deselectAllCommand) {
      setAllSelected(false);
    } else if (command == exportCommand) {
      exportSelected();
    }
  }

  private void setAllSelected(boolean selected) {
    boolean[] flags = new boolean[size()];
    for (int i = 0; i < flags.length; i++) flags[i] = selected;
    setSelectedFlags(flags);
  }

  private void exportSelected() {
    boolean[] flags = new boolean[notes.length];
    getSelectedFlags(flags);
    int count = 0;
    for (int i = 0; i < flags.length; i++) if (flags[i]) count++;
    if (count == 0) {
      navigator.showAlert(Lang.tr("note.select_at_least_one"), AlertType.INFO);
      return;
    }
    final Note[] selected = new Note[count];
    int index = 0;
    for (int i = 0; i < flags.length; i++) if (flags[i]) selected[index++] = notes[i];
    navigator.forward(
        new FileBrowserScreen(
            navigator,
            new FileBrowserScreen.DirectorySelectionListener() {
              public void onDirectorySelected(final String directory) {
                navigator.back();
                promptFileName(directory, selected);
              }
            }));
  }

  private void promptFileName(final String directory, final Note[] selected) {
    final Form form = new Form(Lang.tr("note.export_filename"));
    final TextField name = new TextField(Lang.tr("note.file_name"), "MIDPlay.vnt", 100, TextField.ANY);
    form.append(name);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.cancel()) {
              navigator.back();
            } else if (command == Commands.ok()) {
              try {
                String path = NotesManager.getInstance().exportToDevice(directory, selected, name.getString());
                navigator.back();
                navigator.back();
                navigator.showAlert(Lang.tr("note.exported", path), AlertType.CONFIRMATION);
              } catch (Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            }
          }
        });
    navigator.forward(form);
  }
}
