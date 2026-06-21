package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.MIDPlay;
import midplay.model.Playlist;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.player.PlayerGUI;
import midplay.player.PlayerScreen;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.ui.Commands;
import midplay.ui.FormHelpers;
import midplay.ui.ListReorder;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

// The "now playing" queue view: a TrackListScreen over PlayerGUI's current
// tracks, plus queue-only actions — manual Sort (mirrors the main-menu two-tap
// reorder mode) and Save-as-Playlist (persist the current queue as a custom
// favorite). Sort swaps both the visible rows and a track buffer so Save can
// reorder the live queue without a fragile display-string reverse lookup.
public final class QueueTrackListScreen extends TrackListScreen {
  private static final String SORT_MARKER = Configuration.SORT_ICON;

  private boolean isSortMode = false;
  private int sortPickIndex = -1;
  private Track[] sortTracks;

  public QueueTrackListScreen(String title, Tracks items, Navigator navigator) {
    super(title, items, navigator);
    addCommand(Commands.queueSort());
    addCommand(Commands.queueSaveAsPlaylist());
  }

  protected void showNotify() {
    // Don't let the now-playing marker refresh clobber in-progress sort edits.
    if (!isSortMode) {
      super.showNotify();
    }
  }

  protected void handleSelection() {
    if (isSortMode) {
      handleSortSelection(getSelectedIndex());
    } else {
      super.handleSelection();
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (isSortMode) {
      if (c == Commands.formSave()) {
        saveSort();
      } else if (c == Commands.formCancel()) {
        cancelSort();
      }
      return;
    }
    if (c == Commands.queueSort()) {
      startSort();
    } else if (c == Commands.queueSaveAsPlaylist()) {
      showSaveAsPlaylistForm();
    } else {
      super.handleCommand(c, d);
    }
  }

  // --- Sort mode (mirrors MainMenuScreen two-tap swap) ----------------------

  private void startSort() {
    isSortMode = true;
    sortPickIndex = -1;
    sortTracks = new Track[tracks.length];
    System.arraycopy(tracks, 0, sortTracks, 0, tracks.length);
    switchToEditCommands();
    navigator.showAlert(Lang.tr("settings.reorder.instructions"), AlertType.INFO);
  }

  private void handleSortSelection(int index) {
    if (index < 0 || index >= sortTracks.length) {
      return;
    }
    if (sortPickIndex == -1) {
      sortPickIndex = index;
      markSortRow(index, true);
    } else {
      if (sortPickIndex != index) {
        swapRows(sortPickIndex, index);
      } else {
        markSortRow(index, false);
      }
      sortPickIndex = -1;
    }
  }

  private void swapRows(int a, int b) {
    ListReorder.swapRows(this, a, b, SORT_MARKER);
    Track tmp = sortTracks[a];
    sortTracks[a] = sortTracks[b];
    sortTracks[b] = tmp;
  }

  private void markSortRow(int index, boolean on) {
    ListReorder.toggleMarker(this, index, SORT_MARKER, on);
  }

  private void saveSort() {
    PlayerGUI gui = currentGUI();
    if (gui != null && sortTracks != null) {
      gui.reorderQueue(sortTracks);
    }
    exitSort();
    repopulateFromQueue();
    navigator.showAlert(Lang.tr("settings.reorder.saved"), AlertType.CONFIRMATION);
  }

  private void cancelSort() {
    exitSort();
    repopulateFromQueue();
  }

  private void exitSort() {
    isSortMode = false;
    sortPickIndex = -1;
    sortTracks = null;
    switchToNormalCommands();
  }

  // The base refresh() only repopulates custom playlists; the queue view (no
  // playlist) must rebuild rows itself from the live PlayerGUI tracks.
  private void repopulateFromQueue() {
    PlayerGUI gui = currentGUI();
    Tracks live = gui != null ? gui.getCurrentTracks() : null;
    if (live == null) {
      live = items;
    }
    items = live;
    int sel = getSelectedIndex();
    deleteAll();
    populateItems();
    Utils.clampAndSelect(this, sel);
  }

  private void switchToEditCommands() {
    removeCommand(Commands.queueSort());
    removeCommand(Commands.queueSaveAsPlaylist());
    removeCommand(Commands.addToQueue());
    removeCommand(Commands.playerAddToPlaylist());
    removeCommand(Commands.details());
    removeCommand(Commands.back());
    removeCommand(Commands.playerNowPlaying());
    addCommand(Commands.formSave());
    addCommand(Commands.formCancel());
  }

  private void switchToNormalCommands() {
    removeCommand(Commands.formSave());
    removeCommand(Commands.formCancel());
    addCommand(Commands.queueSort());
    addCommand(Commands.queueSaveAsPlaylist());
    addCommand(Commands.addToQueue());
    addCommand(Commands.playerAddToPlaylist());
    addCommand(Commands.details());
    addCommand(Commands.back());
    addCommand(Commands.playerNowPlaying());
  }

  // --- Save current queue as a custom favorite ------------------------------

  private void showSaveAsPlaylistForm() {
    FormHelpers.promptName(
        navigator,
        Lang.tr("playlist.create"),
        "",
        new FormHelpers.NameSubmitHandler() {
          public void onSubmit(String name) {
            saveQueueAsPlaylist(name);
          }
        });
  }

  private void saveQueueAsPlaylist(String name) {
    Track[] queue = tracks;
    if (queue == null || queue.length == 0) {
      navigator.back();
      return;
    }
    Playlist custom = new Playlist("custom_" + System.currentTimeMillis(), name, "");
    if (FavoritesManager.getInstance().addPlaylist(custom) != FavoritesManager.SUCCESS) {
      navigator.showAlert(Lang.tr("playlist.error.create_failed"), AlertType.ERROR);
      return;
    }
    // ponytail: loops the existing single-track add (local RMS, fast). A bulk
    // method is overkill until a queue routinely exceeds the 200-track cap.
    FavoritesManager fm = FavoritesManager.getInstance();
    for (int i = 0; i < queue.length; i++) {
      if (queue[i] != null) {
        fm.addTrackToPlaylist(custom, queue[i]);
      }
    }
    navigator.back();
    navigator.showAlert(Lang.tr("playlist.status.created"), AlertType.CONFIRMATION, this);
  }

  private PlayerGUI currentGUI() {
    PlayerScreen ps = MIDPlay.getPlayerScreen();
    return ps != null ? ps.getPlayerGUI() : null;
  }
}
