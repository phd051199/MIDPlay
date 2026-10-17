package midplay.ui.screen;

import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;
import midplay.util.Utils;

public final class FileBrowserScreen extends BaseList {
  public interface FileSelectionListener {
    void onFileSelected(String path);
  }

  public interface DirectorySelectionListener {
    void onDirectorySelected(String path);
  }

  private static final String ROOT_PATH = "file:///";
  private static String lastSessionPath;
  private static final int KIND_UP = 0;
  private static final int KIND_DIR = 1;
  private static final int KIND_AUDIO = 2;
  private static final int KIND_FILE = 3;

  private static final String[] AUDIO_EXTENSIONS = {
    ".mp3", ".aac", ".m4a", ".amr", ".awb", ".mid", ".midi", ".wav", ".ogg", ".mp4", ".3gp",
    ".imy", ".rtttl", ".rtt", ".xmf", ".mxmf"
  };

  private static final Utils.Comparator NAME_COMPARATOR =
      new Utils.Comparator() {
        public boolean shouldSwap(Object a, Object b) {
          return ((String) a).toUpperCase().compareTo(((String) b).toUpperCase()) > 0;
        }
      };

  private String currentPath;
  private String currentTitle;
  private String searchQuery = "";
  private String[] rowPaths = new String[0];
  private int[] rowKinds = new int[0];
  private Vector audioPaths = new Vector();
  private int loadKey = 0;
  private final FileSelectionListener fileSelectionListener;
  private final DirectorySelectionListener directorySelectionListener;
  private final Command selectDirectoryCommand;
  private final Command renameCommand;
  private final Command deleteCommand;
  private final Command searchCommand;
  private final Command clearSearchCommand;
  private final Command selectMultipleCommand;
  private final Command gotoPathCommand;

  public FileBrowserScreen(Navigator navigator) {
    this(navigator, sessionStartPath(), null, null);
  }

  public FileBrowserScreen(Navigator navigator, String path) {
    this(navigator, path, null, null);
  }

  public FileBrowserScreen(Navigator navigator, FileSelectionListener listener) {
    this(navigator, sessionStartPath(), listener, null);
  }

  public FileBrowserScreen(Navigator navigator, DirectorySelectionListener listener) {
    this(navigator, sessionStartPath(), null, listener);
  }

  private FileBrowserScreen(
      Navigator navigator,
      String path,
      FileSelectionListener listener,
      DirectorySelectionListener directoryListener) {
    super(Lang.tr("menu.files"), navigator);
    fileSelectionListener = listener;
    directorySelectionListener = directoryListener;
    selectDirectoryCommand =
        directoryListener == null ? null : new Command(Lang.tr("note.save_here"), Command.OK, 0);
    renameCommand = new Command(Lang.tr("files.rename"), Command.SCREEN, 6);
    deleteCommand = new Command(Lang.tr("files.delete"), Command.SCREEN, 7);
    searchCommand = new Command(Lang.tr("files.search"), Command.SCREEN, 5);
    clearSearchCommand = new Command(Lang.tr("files.clear_search"), Command.SCREEN, 8);
    selectMultipleCommand = new Command(Lang.tr("files.select_multiple"), Command.SCREEN, 9);
    gotoPathCommand = new Command(Lang.tr("files.goto_path"), Command.SCREEN, 10);
    this.currentPath = normalizePath(path);
    lastSessionPath = this.currentPath;
    this.currentTitle = Lang.tr("menu.files");
    addCommand(Commands.filesRoot());
    addCommand(Commands.addToQueue());
    addCommand(Commands.addAllToQueue());
    addCommand(Commands.playerAddToPlaylist());
    addCommand(searchCommand);
    addCommand(clearSearchCommand);
    addCommand(renameCommand);
    addCommand(deleteCommand);
    if (selectDirectoryCommand != null) {
      addCommand(selectDirectoryCommand);
    } else if (fileSelectionListener == null && directorySelectionListener == null) {
      addCommand(selectMultipleCommand);
    }
    addCommand(gotoPathCommand);
    populateItems();
  }

  protected void populateItems() {
    startLoad(getSelectedIndex());
  }

  protected void refresh() {
    startLoad(getSelectedIndex());
  }

  protected void showNotify() {
    super.showNotify();
    refresh();
  }

  private void startLoad(final int restoreIndex) {
    final int key = ++loadKey;
    final String path = currentPath;
    final String filter = searchQuery;
    this.deleteAll();
    this.append(Lang.tr("status.loading"), null);
    new Thread(
        new Runnable() {
          public void run() {
            final Vector dirs = new Vector();
            final Vector files = new Vector();
            FileConnection directory = null;
            try {
              if (isRoot(path)) {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements()) {
                  String root = (String) roots.nextElement();
                  if (root != null && root.length() > 0) {
                    dirs.addElement(root);
                  }
                }
              } else {
                directory = (FileConnection) Connector.open(path, Connector.READ);
                if (directory.exists() && directory.isDirectory()) {
                  Enumeration names = directory.list();
                  while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    if (name == null
                        || name.length() == 0
                        || name.equals(".")
                        || name.equals("..")
                        || name.startsWith(".")) {
                      continue;
                    }
                    if (name.endsWith("/") || name.endsWith(":")) {
                      dirs.addElement(name);
                    } else if (filter.length() == 0 || Utils.indexOfIgnoreCase(name, filter) >= 0) {
                      files.addElement(name);
                    }
                  }
                }
              }
            } catch (Throwable t) {
            } finally {
              closeDirectory(directory);
            }
            Utils.bubbleSort(dirs, NAME_COMPARATOR);
            Utils.bubbleSort(files, NAME_COMPARATOR);
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    if (key != loadKey || !path.equals(currentPath)) {
                      return;
                    }
                    applyRows(path, dirs, files, restoreIndex);
                  }
                });
          }
        })
        .start();
  }

  private void applyRows(String path, Vector dirs, Vector files, int restoreIndex) {
    String parent = isRoot(path) ? null : parentOf(path);
    int count = (parent != null ? 1 : 0) + dirs.size() + files.size();
    String[] paths = new String[count];
    int[] kinds = new int[count];
    Vector audio = new Vector();
    int i = 0;
    if (parent != null) {
      paths[i] = parent;
      kinds[i] = KIND_UP;
      i++;
    }
    for (int d = 0; d < dirs.size(); d++) {
      String dirName = (String) dirs.elementAt(d);
      paths[i] = path + (dirName.endsWith("/") ? dirName : dirName + "/");
      kinds[i] = KIND_DIR;
      i++;
    }
    for (int f = 0; f < files.size(); f++) {
      String name = (String) files.elementAt(f);
      paths[i] = path + name;
      if (isAudioFile(name)) {
        kinds[i] = KIND_AUDIO;
        audio.addElement(paths[i]);
      } else {
        kinds[i] = KIND_FILE;
      }
      i++;
    }

    currentPath = path;
    lastSessionPath = path;
    currentTitle = dirTitle(path);
    rowPaths = paths;
    rowKinds = kinds;
    audioPaths = audio;

    setTitle(currentTitle);
    this.deleteAll();
    for (int r = 0; r < rowPaths.length; r++) {
      this.append(rowLabel(rowPaths[r], rowKinds[r]), iconFor(rowKinds[r]));
    }
    if (rowPaths.length == 0) {
      this.append(Lang.tr("files.empty"), null);
    }
    Utils.clampAndSelect(this, restoreIndex);
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, rowPaths.length)) {
      return;
    }
    int kind = rowKinds[index];
    if (kind == KIND_UP || kind == KIND_DIR) {
      navigateTo(rowPaths[index]);
      return;
    }
    if (kind == KIND_AUDIO) {
      playFrom(index);
      return;
    }
    if (fileSelectionListener != null && isNoteFile(rowPaths[index])) {
      fileSelectionListener.onFileSelected(rowPaths[index]);
      return;
    }
    if (isImageFile(rowPaths[index])) {
      navigator.forward(new ImagePreviewCanvas(fileNameFor(rowPaths[index]), rowPaths[index], navigator));
      return;
    }
    if (isTextFile(rowPaths[index])) {
      navigator.forward(new FilePreviewScreen(fileNameFor(rowPaths[index]), rowPaths[index], navigator));
      return;
    }
    navigator.showAlert(Lang.tr("files.not_audio"), AlertType.INFO);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == selectDirectoryCommand) {
      directorySelectionListener.onDirectorySelected(currentPath);
    } else if (c == Commands.filesRoot()) {
      navigateTo(ROOT_PATH);
    } else if (c == Commands.addToQueue()) {
      queueSelected();
    } else if (c == Commands.addAllToQueue()) {
      queueAll();
    } else if (c == Commands.playerAddToPlaylist()) {
      addSelectedToPlaylist();
    } else if (c == searchCommand) {
      promptSearch();
    } else if (c == clearSearchCommand) {
      searchQuery = "";
      startLoad(0);
    } else if (c == selectMultipleCommand) {
      openMultiSelect();
    } else if (c == renameCommand) {
      promptRename();
    } else if (c == deleteCommand) {
      deleteSelected();
    }
  }

  private void navigateTo(String path) {
    currentPath = normalizePath(path);
    lastSessionPath = currentPath;
    searchQuery = "";
    startLoad(0);
  }

  private void promptSearch() {
    final TextBox box = new TextBox(Lang.tr("files.search"), searchQuery, 100, TextField.ANY);
    box.addCommand(Commands.ok());
    box.addCommand(Commands.cancel());
    box.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              searchQuery = box.getString().trim();
              navigator.back();
              startLoad(0);
            } else if (command == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(box);
  }

  private void openMultiSelect() {
    Vector paths = new Vector();
    Vector audio = new Vector();
    for (int i = 0; i < rowPaths.length; i++) {
      if (rowKinds[i] == KIND_AUDIO || rowKinds[i] == KIND_FILE) {
        paths.addElement(rowPaths[i]);
        audio.addElement(rowKinds[i] == KIND_AUDIO ? Boolean.TRUE : Boolean.FALSE);
      }
    }
    String[] selectedPaths = new String[paths.size()];
    boolean[] selectedAudio = new boolean[paths.size()];
    for (int i = 0; i < selectedPaths.length; i++) {
      selectedPaths[i] = (String) paths.elementAt(i);
      selectedAudio[i] = ((Boolean) audio.elementAt(i)).booleanValue();
    }
    if (selectedPaths.length == 0) {
      navigator.showAlert(Lang.tr("bulk.select_empty"), AlertType.INFO);
      return;
    }
    navigator.forward(new MultiFileSelectScreen(navigator, currentTitle, selectedPaths, selectedAudio, this));
  }

  private static String sessionStartPath() {
    return lastSessionPath != null
        ? lastSessionPath
        : SettingsManager.getInstance().getCurrentDownloadPath();
  }

  private boolean isSelectedFile() {
    int index = getSelectedIndex();
    return isValidSelection(index, rowPaths.length)
        && (rowKinds[index] == KIND_FILE || rowKinds[index] == KIND_AUDIO);
  }

  private void promptRename() {
    if (!isSelectedFile()) {
      return;
    }
    final String path = rowPaths[getSelectedIndex()];
    final Form form = new Form(Lang.tr("files.rename"));
    final TextField name = new TextField(Lang.tr("files.name"), fileNameFor(path), 100, TextField.ANY);
    form.append(name);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              try {
                String newName = name.getString().trim();
                if (newName.length() == 0 || newName.indexOf('/') != -1) {
                  return;
                }
                renameFile(path, newName);
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

  private void deleteSelected() {
    if (!isSelectedFile()) {
      return;
    }
    final String path = rowPaths[getSelectedIndex()];
    navigator.showConfirmationAlert(
        Lang.tr("files.confirm.delete"),
        new Runnable() {
          public void run() {
            try {
              FileConnection file = null;
              try {
                file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                file.delete();
              } finally {
                closeDirectory(file);
              }
              refresh();
              navigator.dismissAlert();
            } catch (Exception e) {
              navigator.showAlert(e.toString(), AlertType.ERROR);
            }
          }
        },
        AlertType.WARNING);
  }

  private static void renameFile(String path, String name) throws Exception {
    FileConnection file = null;
    try {
      file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
      file.rename(name);
    } finally {
      closeDirectory(file);
    }
  }

  private void playFrom(int rowIndex) {
    int audioIndex = indexOfAudio(rowPaths[rowIndex]);
    if (audioIndex < 0) {
      return;
    }
    Tracks all = new Tracks();
    all.setTracks(buildAudioTracks());
    PlayerNavHelper.playTrackFromList(currentTitle, all, audioIndex, 0L, navigator);
  }

  private void queueSelected() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, rowPaths.length)) {
      return;
    }
    if (rowKinds[index] != KIND_AUDIO) {
      navigator.showAlert(Lang.tr("files.not_audio"), AlertType.INFO);
      return;
    }
    Track track = trackFor(rowPaths[index]);
    PlayerNavHelper.addToQueue(new Track[] {track}, currentTitle, navigator);
  }

  private void queueAll() {
    Track[] tracks = buildAudioTracks();
    if (tracks.length == 0) {
      navigator.showAlert(Lang.tr("files.empty"), AlertType.INFO);
      return;
    }
    PlayerNavHelper.addToQueue(tracks, currentTitle, navigator);
  }

  private void addSelectedToPlaylist() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, rowPaths.length)) {
      return;
    }
    if (rowKinds[index] != KIND_AUDIO) {
      navigator.showAlert(Lang.tr("files.not_audio"), AlertType.INFO);
      return;
    }
    navigator.forward(new PlaylistPickerScreen(navigator, trackFor(rowPaths[index]), this));
  }

  private Track[] buildAudioTracks() {
    Track[] tracks = new Track[audioPaths.size()];
    for (int i = 0; i < tracks.length; i++) {
      tracks[i] = trackFor((String) audioPaths.elementAt(i));
    }
    return tracks;
  }

  private int indexOfAudio(String path) {
    for (int i = 0; i < audioPaths.size(); i++) {
      if (path != null && path.equals(audioPaths.elementAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private static Track trackFor(String path) {
    String name = fileNameFor(path);
    String base = baseNameOf(name);
    if (base.length() == 0) {
      base = name;
    }
    return new Track("", base, path, 0, "", "");
  }

  protected int badgeAt(int row) {
    if (!isValidSelection(row, rowKinds.length)) {
      return BADGE_NONE;
    }
    int kind = rowKinds[row];
    if (kind == KIND_UP || kind == KIND_DIR) {
      return BADGE_FOLDER;
    }
    if (kind == KIND_AUDIO) {
      return BADGE_MUSIC;
    }
    return BADGE_NONE;
  }

  private static Image iconFor(int kind) {
    if (kind == KIND_UP || kind == KIND_DIR) {
      return Configuration.folderIcon;
    }
    if (kind == KIND_AUDIO) {
      return Configuration.musicIcon;
    }
    return Configuration.infoIcon;
  }

  private static String rowLabel(String path, int kind) {
    if (kind == KIND_UP) {
      return "..";
    }
    return fileNameFor(path);
  }

  private static String normalizePath(String path) {
    if (path == null || path.length() == 0) {
      path = Configuration.DEFAULT_DOWNLOAD_PATH;
    }
    return path.endsWith("/") ? path : path + "/";
  }

  private static boolean isRoot(String path) {
    return path == null || path.length() <= ROOT_PATH.length();
  }

  private static String parentOf(String path) {
    if (isRoot(path)) {
      return null;
    }
    String trimmed = path.substring(0, path.length() - 1);
    int slash = trimmed.lastIndexOf('/');
    if (slash < 0) {
      return ROOT_PATH;
    }
    String parent = trimmed.substring(0, slash + 1);
    return parent.length() < ROOT_PATH.length() ? ROOT_PATH : parent;
  }

  private static String dirTitle(String path) {
    if (isRoot(path)) {
      return path;
    }
    return fileNameFor(path);
  }

  private static String fileNameFor(String path) {
    if (path == null) {
      return "";
    }
    String trimmed = path;
    while (trimmed.length() > 1 && trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    int slash = trimmed.lastIndexOf('/');
    String name = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    return name.length() > 0 ? name : path;
  }

  private static String baseNameOf(String fileName) {
    int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }

  private static boolean isAudioFile(String name) {
    String lower = name.toLowerCase();
    for (int i = 0; i < AUDIO_EXTENSIONS.length; i++) {
      if (lower.endsWith(AUDIO_EXTENSIONS[i])) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNoteFile(String path) {
    String lower = fileNameFor(path).toLowerCase();
    return lower.endsWith(".vnt") || lower.endsWith(".txt");
  }

  private static boolean isTextFile(String path) {
    String lower = fileNameFor(path).toLowerCase();
    return lower.endsWith(".txt")
        || lower.endsWith(".vnt")
        || lower.endsWith(".log")
        || lower.endsWith(".csv")
        || lower.endsWith(".json")
        || lower.endsWith(".xml")
        || lower.endsWith(".m3u")
        || lower.endsWith(".pls");
  }

  private static boolean isImageFile(String path) {
    String lower = fileNameFor(path).toLowerCase();
    return lower.endsWith(".png")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".gif");
  }

  private static void closeDirectory(FileConnection directory) {
    if (directory != null) {
      try {
        directory.close();
      } catch (Exception e) {
      }
    }
  }

  private void promptGotoPath() {
    final TextBox box = new TextBox(Lang.tr("files.open_path"), currentPath, 1024, TextField.ANY);
    box.addCommand(Commands.ok());
    box.addCommand(Commands.cancel());
    box.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              String value = box.getString();
              value = value == null ? "" : value.trim();
              if (value.length() == 0) {
                navigator.showAlert(Lang.tr("files.open_path_required"), AlertType.ERROR);
                return;
              }
              String path = normalizePath(value);
              navigator.back();
              navigateTo(path);
            } else if (c == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(box);
  }

}