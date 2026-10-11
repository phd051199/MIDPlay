package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public final class NotesScreen extends BaseList {
  private final Command newCommand;
  private final Command deleteCommand;
  private final Command exportCommand;
  private final Command importCommand;
  private final Command searchCommand;
  private final Command clearSearchCommand;
  private final Command customTitleCommand;
  private final Command pinCommand;
  private Note[] notes;
  private String searchQuery = "";

  public NotesScreen(Navigator navigator) {
    super(Lang.tr("note.title"), navigator);
    newCommand = new Command(Lang.tr("note.new"), Command.SCREEN, 1);
    deleteCommand = new Command(Lang.tr("note.delete"), Command.SCREEN, 2);
    exportCommand = new Command(Lang.tr("note.export"), Command.SCREEN, 3);
    importCommand = new Command(Lang.tr("note.import"), Command.SCREEN, 4);
    searchCommand = new Command(Lang.tr("note.search"), Command.SCREEN, 5);
    clearSearchCommand = new Command(Lang.tr("note.clear_search"), Command.SCREEN, 6);
    customTitleCommand = new Command(Lang.tr("note.custom_title"), Command.SCREEN, 7);
    pinCommand = new Command(Lang.tr("note.toggle_pin"), Command.SCREEN, 8);
    addCommand(newCommand);
    addCommand(deleteCommand);
    addCommand(exportCommand);
    addCommand(importCommand);
    addCommand(searchCommand);
    addCommand(clearSearchCommand);
    addCommand(customTitleCommand);
    addCommand(pinCommand);
    populateItems();
  }

  protected void populateItems() {
    Note[] all = NotesManager.getInstance().getNotes();
    java.util.Vector visible = new java.util.Vector();
    for (int i = 0; i < all.length; i++) {
      if (matches(all[i])) visible.addElement(all[i]);
    }
    notes = new Note[visible.size()];
    for (int i = 0; i < notes.length; i++) {
      notes[i] = (Note) visible.elementAt(i);
      append((notes[i].pinned ? "[PIN] " : "") + notes[i].title, null);
    }
    if (notes.length == 0) append(searchQuery.length() == 0 ? Lang.tr("note.empty") : Lang.tr("note.no_search_results"), null);
  }

  protected void handleSelection() {
    Note note = selected();
    if (note != null) navigator.forward(new NoteReaderScreen(note, navigator, refreshOnClose()));
  }

  protected void showNotify() {
    super.showNotify();
    refresh();
  }

  protected void handleCommand(Command command, Displayable displayable) {
    if (command == newCommand) createNote();
    else if (command == deleteCommand) deleteNote();
    else if (command == exportCommand) exportNotes();
    else if (command == importCommand) importNotes();
    else if (command == searchCommand) searchNotes();
    else if (command == clearSearchCommand) {
      searchQuery = "";
      refresh();
    } else if (command == customTitleCommand) setCustomTitle();
    else if (command == pinCommand) togglePin();
  }

  private boolean matches(Note note) {
    if (searchQuery.length() == 0) return true;
    return Utils.indexOfIgnoreCase(note.title, searchQuery) >= 0
        || Utils.indexOfIgnoreCase(note.text, searchQuery) >= 0;
  }

  private void searchNotes() {
    final TextBox box = new TextBox(Lang.tr("note.search"), searchQuery, 100, TextField.ANY);
    box.addCommand(Commands.ok());
    box.addCommand(Commands.cancel());
    box.setCommandListener(new CommandListener() {
      public void commandAction(Command command, Displayable displayable) {
        if (command == Commands.ok()) {
          searchQuery = box.getString().trim();
          navigator.back();
          refresh();
        } else if (command == Commands.cancel()) {
          navigator.back();
        }
      }
    });
    navigator.forward(box);
  }

  private void togglePin() {
    Note note = selected();
    if (note == null) return;
    try {
      NotesManager.getInstance().togglePinned(note.id);
      refresh();
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void setCustomTitle() {
    final Note note = selected();
    if (note == null) return;
    final Form form = new Form(Lang.tr("note.custom_title"));
    final TextField field = new TextField(Lang.tr("note.custom_title_field"), note.customTitle, 60, TextField.ANY);
    form.append(field);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(new CommandListener() {
      public void commandAction(Command command, Displayable displayable) {
        if (command == Commands.ok()) {
          try {
            NotesManager.getInstance().updateCustomTitle(note.id, field.getString());
            navigator.back();
            refresh();
          } catch (Exception e) {
            navigator.showAlert(e.toString(), AlertType.ERROR);
          }
        } else if (command == Commands.cancel()) {
          navigator.back();
        }
      }
    });
    navigator.forward(form);
  }

  private Note selected() {
    int index = getSelectedIndex();
    return notes != null && index >= 0 && index < notes.length ? notes[index] : null;
  }

  private void createNote() {
    navigator.forward(new NoteEditorScreen(null, navigator, refreshOnClose()));
  }

  private Runnable refreshOnClose() {
    return new Runnable() {
      public void run() {
        refresh();
      }
    };
  }

  private void deleteNote() {
    final Note note = selected();
    if (note == null) return;
    navigator.showConfirmationAlert(Lang.tr("note.delete"), new Runnable() {
      public void run() {
        try {
          NotesManager.getInstance().remove(note.id);
          refresh();
          navigator.dismissAlert();
        } catch (Exception e) {
          navigator.showAlert(e.toString(), AlertType.ERROR);
        }
      }
    }, AlertType.WARNING);
  }

  private void exportNotes() {
    navigator.forward(new NoteExportSelectionScreen(navigator));
  }

  private void importNotes() {
    navigator.forward(new FileBrowserScreen(navigator, new FileBrowserScreen.FileSelectionListener() {
      public void onFileSelected(String path) {
        try {
          NotesManager.getInstance().importFromDevice(path);
          navigator.back();
          refresh();
          navigator.showAlert(Lang.tr("note.imported"), AlertType.CONFIRMATION);
        } catch (Exception e) {
          navigator.showAlert(e.toString(), AlertType.ERROR);
        }
      }
    }));
  }
}
