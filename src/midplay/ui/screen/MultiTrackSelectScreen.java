package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import midplay.model.Playlist;
import midplay.model.Track;
import midplay.store.Configuration;
import midplay.store.DownloadManager;
import midplay.store.FavoritesManager;
import midplay.ui.Commands;
import midplay.ui.DownloadActions;
import midplay.ui.BaseList;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;
import midplay.util.Utils;

/** Multiple-selection actions shared by downloads, search results, and playlists. */
public final class MultiTrackSelectScreen extends BaseList {
  public static final int SOURCE_TRACKS = 0;
  public static final int SOURCE_DOWNLOADS = 1;
  public static final int SOURCE_PLAYLIST = 2;

  private final Track[] tracks;
  private final Playlist playlist;
  private final String title;
  private final int source;
  private final Command selectAllCommand = new Command(Lang.tr("note.select_all"), Command.SCREEN, 1);
  private final Command deselectAllCommand = new Command(Lang.tr("note.deselect_all"), Command.SCREEN, 2);
  private final Command addQueueCommand = new Command(Lang.tr("queue.add_to_queue"), Command.SCREEN, 3);
  private final Command addPlaylistCommand = new Command(Lang.tr("playlist.add_track"), Command.SCREEN, 4);
  private final Command downloadCommand = new Command(Lang.tr("download.action"), Command.SCREEN, 5);
  private final Command deleteCommand = new Command(Lang.tr("download.delete"), Command.SCREEN, 6);
  private final Command removeCommand = new Command(Lang.tr("playlist.remove_selected"), Command.SCREEN, 7);

  public MultiTrackSelectScreen(
      Navigator navigator, String title, Track[] tracks, int source, Playlist playlist) {
    this(navigator, title, tracks, source, playlist, null);
  }

  public MultiTrackSelectScreen(
      Navigator navigator, String title, Track[] tracks, int source, Playlist playlist,
      BaseList sourceList) {
    super(title + " - " + Lang.tr("action.select"), navigator, Choice.MULTIPLE);
    this.title = title;
    this.tracks = tracks == null ? new Track[0] : tracks;
    this.source = source;
    this.playlist = playlist;

    String[] artUrls = new String[this.tracks.length];
    for (int i = 0; i < this.tracks.length; i++) {
      Track track = this.tracks[i];
      String label = track == null ? "" : track.getDisplayTitle(Lang.tr("details.unknown_artist"));
      Image cached = sourceList != null && i < sourceList.size() ? sourceList.getImage(i) : null;
      if (cached != null && cached != Configuration.musicIcon) {
        append(label, cached);
      } else {
        append(label, Configuration.musicIcon);
        artUrls[i] = Utils.withArtType(track == null ? null : track.getImageUrl(), 1);
      }
    }

    addCommand(selectAllCommand);
    addCommand(deselectAllCommand);
    addCommand(addQueueCommand);
    addCommand(addPlaylistCommand);
    if (source != SOURCE_DOWNLOADS) addCommand(downloadCommand);
    if (source == SOURCE_DOWNLOADS) addCommand(deleteCommand);
    if (source == SOURCE_PLAYLIST && playlist != null && playlist.isCustom()) {
      addCommand(removeCommand);
    }
    loadArt(artUrls);
  }

  protected void handleCommand(Command command, Displayable displayable) {
    if (command == selectAllCommand) {
      setAllSelected(true);
    } else if (command == deselectAllCommand) {
      setAllSelected(false);
    } else if (command == addQueueCommand) {
      Track[] selected = selectedTracks();
      if (selected.length > 0) PlayerNavHelper.addToQueue(selected, title, navigator);
      else showEmptySelection();
    } else if (command == addPlaylistCommand) {
      Track[] selected = selectedTracks();
      if (selected.length == 0) showEmptySelection();
      else navigator.forward(new PlaylistPickerScreen(navigator, selected, this));
    } else if (command == downloadCommand) {
      Track[] selected = selectedTracks();
      if (selected.length == 0) showEmptySelection();
      else DownloadActions.downloadTracks(navigator, selected);
    } else if (command == deleteCommand) {
      deleteSelectedDownloads();
    } else if (command == removeCommand) {
      removeSelectedFromPlaylist();
    }
  }

  protected void handleSelection() {}

  protected void populateItems() {}

  protected int badgeAt(int row) {
    return isValidSelection(row, tracks.length) ? BADGE_MUSIC : BADGE_NONE;
  }

  private void setAllSelected(boolean selected) {
    boolean[] flags = new boolean[size()];
    for (int i = 0; i < flags.length; i++) flags[i] = selected;
    setSelectedFlags(flags);
  }

  private Track[] selectedTracks() {
    boolean[] flags = new boolean[tracks.length];
    getSelectedFlags(flags);
    int count = 0;
    for (int i = 0; i < flags.length; i++) if (flags[i]) count++;
    Track[] selected = new Track[count];
    int index = 0;
    for (int i = 0; i < flags.length; i++) if (flags[i]) selected[index++] = tracks[i];
    return selected;
  }

  private void showEmptySelection() {
    navigator.showAlert(Lang.tr("bulk.select_empty"), AlertType.INFO);
  }

  private void deleteSelectedDownloads() {
    final Track[] selected = selectedTracks();
    if (selected.length == 0) {
      showEmptySelection();
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("download.confirm.delete_selected"),
        new Runnable() {
          public void run() {
            int deleted = 0;
            DownloadManager manager = DownloadManager.getInstance();
            for (int i = 0; i < selected.length; i++) if (manager.removeDownload(selected[i])) deleted++;
            navigator.back();
            navigator.showAlert(Lang.tr("bulk.tracks_deleted", Integer.toString(deleted)), AlertType.CONFIRMATION);
          }
        },
        AlertType.WARNING);
  }

  private void removeSelectedFromPlaylist() {
    final Track[] selected = selectedTracks();
    if (selected.length == 0) {
      showEmptySelection();
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("playlist.confirm.remove_selected"),
        new Runnable() {
          public void run() {
            int removed = 0;
            FavoritesManager manager = FavoritesManager.getInstance();
            for (int i = 0; i < selected.length; i++) {
              if (manager.removeTrackFromPlaylist(playlist, selected[i])) removed++;
            }
            navigator.back();
            navigator.showAlert(Lang.tr("bulk.tracks_removed", Integer.toString(removed)), AlertType.CONFIRMATION);
          }
        },
        AlertType.WARNING);
  }
}
