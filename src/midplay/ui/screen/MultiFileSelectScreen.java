package midplay.ui.screen;

import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import midplay.model.Track;
import midplay.store.Configuration;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;

/** Multiple-selection actions for files in the current folder. */
public final class MultiFileSelectScreen extends List implements CommandListener {
  private final Navigator navigator;
  private final String title;
  private final String[] paths;
  private final boolean[] audioFiles;
  private final Displayable previousScreen;
  private final Command selectAllCommand = new Command(Lang.tr("note.select_all"), Command.SCREEN, 1);
  private final Command deselectAllCommand = new Command(Lang.tr("note.deselect_all"), Command.SCREEN, 2);
  private final Command addQueueCommand = new Command(Lang.tr("queue.add_to_queue"), Command.SCREEN, 3);
  private final Command addPlaylistCommand = new Command(Lang.tr("playlist.add_track"), Command.SCREEN, 4);
  private final Command deleteCommand = new Command(Lang.tr("files.delete"), Command.SCREEN, 5);

  public MultiFileSelectScreen(
      Navigator navigator, String title, String[] paths, boolean[] audioFiles,
      Displayable previousScreen) {
    super(Lang.tr("files.select_multiple") + " - " + title, Choice.MULTIPLE);
    this.navigator = navigator;
    this.title = title;
    this.paths = paths == null ? new String[0] : paths;
    this.audioFiles = audioFiles == null ? new boolean[0] : audioFiles;
    this.previousScreen = previousScreen;
    for (int i = 0; i < this.paths.length; i++) {
      append(fileName(this.paths[i]), this.audioFiles[i] ? Configuration.musicIcon : Configuration.infoIcon);
    }
    addCommand(selectAllCommand);
    addCommand(deselectAllCommand);
    addCommand(addQueueCommand);
    addCommand(addPlaylistCommand);
    addCommand(deleteCommand);
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
    } else if (command == addQueueCommand) {
      Track[] tracks = selectedAudioTracks();
      if (tracks.length == 0) showNoAudioFiles();
      else PlayerNavHelper.addToQueue(tracks, title, navigator);
    } else if (command == addPlaylistCommand) {
      Track[] tracks = selectedAudioTracks();
      if (tracks.length == 0) showNoAudioFiles();
      else navigator.forward(new PlaylistPickerScreen(navigator, tracks, this));
    } else if (command == deleteCommand) {
      confirmDeleteSelected();
    }
  }

  private void setAllSelected(boolean selected) {
    boolean[] flags = new boolean[size()];
    for (int i = 0; i < flags.length; i++) flags[i] = selected;
    setSelectedFlags(flags);
  }

  private Track[] selectedAudioTracks() {
    boolean[] flags = new boolean[paths.length];
    getSelectedFlags(flags);
    Vector selected = new Vector();
    for (int i = 0; i < flags.length; i++) {
      if (flags[i] && audioFiles[i]) selected.addElement(trackFor(paths[i]));
    }
    Track[] result = new Track[selected.size()];
    selected.copyInto(result);
    return result;
  }

  private void showNoAudioFiles() {
    navigator.showAlert(Lang.tr("files.not_audio"), AlertType.INFO);
  }

  private void confirmDeleteSelected() {
    final String[] selected = selectedPaths();
    if (selected.length == 0) {
      navigator.showAlert(Lang.tr("bulk.select_empty"), AlertType.INFO);
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("files.confirm.delete_selected"),
        new Runnable() {
          public void run() {
            int deleted = 0;
            for (int i = 0; i < selected.length; i++) {
              if (deleteFile(selected[i])) deleted++;
            }
            navigator.back();
            navigator.showAlert(Lang.tr("bulk.files_deleted", Integer.toString(deleted)), AlertType.CONFIRMATION);
          }
        },
        AlertType.WARNING);
  }

  private String[] selectedPaths() {
    boolean[] flags = new boolean[paths.length];
    getSelectedFlags(flags);
    Vector selected = new Vector();
    for (int i = 0; i < flags.length; i++) if (flags[i]) selected.addElement(paths[i]);
    String[] result = new String[selected.size()];
    selected.copyInto(result);
    return result;
  }

  private static boolean deleteFile(String path) {
    FileConnection file = null;
    try {
      file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
      if (file.exists()) file.delete();
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (file != null) {
        try {
          file.close();
        } catch (Exception e) {
        }
      }
    }
  }

  private static Track trackFor(String path) {
    String name = fileName(path);
    int dot = name.lastIndexOf('.');
    if (dot > 0) name = name.substring(0, dot);
    return new Track("", name, path, 0, "", "");
  }

  private static String fileName(String path) {
    if (path == null) return "";
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }
}
