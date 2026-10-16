package midplay.ui.screen;

import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public final class NotesScreen extends List implements CommandListener {
  private final Navigator navigator;
  private final Command newCommand;
  private final Command searchCommand;
  private final Command clearSearchCommand;
  private final Command customTitleCommand;
  private final Command pinCommand;
  private final Command deleteCommand;
  private final Command selectMultipleCommand;
  private Note[] notes;
  private String searchQuery = "";

  public NotesScreen(Navigator navigator) {
    super(Lang.tr("note.title"), List.IMPLICIT);
    this.navigator = navigator;
    newCommand = new Command(Lang.tr("note.new"), Command.SCREEN, 1);
    searchCommand = new Command(Lang.tr("note.search"), Command.SCREEN, 5);
    clearSearchCommand = new Command(Lang.tr("note.clear_search"), Command.SCREEN, 6);
    customTitleCommand = new Command(Lang.tr("note.custom_title"), Command.SCREEN, 7);
    pinCommand = new Command(Lang.tr("note.toggle_pin"), Command.SCREEN, 8);
    deleteCommand = new Command(Lang.tr("note.delete"), Command.SCREEN, 9);
    selectMultipleCommand = new Command(Lang.tr("action.select"), Command.SCREEN, 10);
    addCommand(newCommand);
    addCommand(searchCommand);
    addCommand(clearSearchCommand);
    addCommand(customTitleCommand);
    addCommand(pinCommand);
    addCommand(deleteCommand);
    addCommand(selectMultipleCommand);
    addCommand(Commands.back());
    addCommand(Commands.playerNowPlaying());
    setCommandListener(this);
    populateItems();
  }

  private void populateItems() {
    deleteAll();
    Note[] all = NotesManager.getInstance().getNotes();
    Vector visible = new Vector();
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

  public void showNotify() {
    
    refresh();
  }

  public void commandAction(Command command, Displayable displayable) {
    try {
      if (command == List.SELECT_COMMAND) {
        Note note = selected();
        if (note != null) navigator.forward(new NoteReaderScreen(note, navigator, refreshOnClose()));
      } else if (command == Commands.back()) {
        navigator.back();
      } else if (command == Commands.playerNowPlaying()) {
        midplay.ui.PlayerNavHelper.showNowPlaying(navigator);
      } else if (command == newCommand) createNote();
      else if (command == searchCommand) searchNotes();
      else if (command == clearSearchCommand) {
        searchQuery = "";
        refresh();
      } else if (command == customTitleCommand) setCustomTitle();
      else if (command == pinCommand) togglePin();
      else if (command == deleteCommand) deleteSelected();
      else if (command == selectMultipleCommand) selectMultiple();
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
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

  private void deleteSelected() {
    final Note note = selected();
    if (note == null) {
      return;
    }
    navigator.showConfirmationAlert(Lang.tr("note.delete_confirm", note.title), new Runnable() {
      public void run() {
        try {
          NotesManager.getInstance().remove(note.id);
          refresh();
        } catch (Exception e) {
          navigator.showAlert(e.toString(), AlertType.ERROR);
        }
      }
    });
  }

  private void selectMultiple() {
    if (notes == null || notes.length == 0) {
      return;
    }
    navigator.forward(new NotesMultiSelectScreen(navigator, notes, refreshOnClose()));
  }

  private void refresh() {
    populateItems();
  }
}
