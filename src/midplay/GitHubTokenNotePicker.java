package midplay;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import midplay.store.NotesManager;
import midplay.store.NotesManager.Note;
import midplay.ui.Commands;
import midplay.util.Lang;
import githubj2me.GH;

/** Lets GitHub-j2me import a GitHub/Gitea token from an existing MIDPlay note. */
public final class GitHubTokenNotePicker extends List implements CommandListener {
  private final int apiMode;
  private final Note[] notes;

  public GitHubTokenNotePicker(int apiMode) {
    super(Lang.tr("github.import_token_note"), List.IMPLICIT);
    this.apiMode = apiMode;
    notes = NotesManager.getInstance().getNotes();
    for (int i = 0; i < notes.length; i++) append(notes[i].title, null);
    addCommand(Commands.back());
    setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == List.SELECT_COMMAND) {
      int index = getSelectedIndex();
      if (index >= 0 && index < notes.length) {
        GH.importTokenFromText(apiMode, notes[index].text);
      }
    } else if (command == Commands.back()) {
      GH.cancelTokenImport();
    }
  }
}
