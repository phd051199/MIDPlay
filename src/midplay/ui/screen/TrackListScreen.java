package midplay.ui.screen;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.MIDPlay;
import midplay.model.Playlist;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.player.PlayerScreen;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.DownloadActions;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.ui.ListReorder;
import midplay.util.Lang;
import midplay.util.Utils;

public class TrackListScreen extends BaseList {
  private static final String NOW_PLAYING_MARKER = "[*] ";

  Tracks items;
  final String title;
  final Playlist playlist;
  Track[] tracks;
  String[] rowTexts;
  private int nowPlayingIndex = -1;
  private Timer markerTimer;
  private final Command reorderTracksCommand;
  private final Command selectMultipleCommand;
  private boolean reorderMode;
  private int reorderPick = -1;
  private Track[] reorderTracks;

  public TrackListScreen(String title, Tracks items, Navigator navigator) {
    this(title, items, navigator, null);
  }

  public TrackListScreen(String title, Tracks items, Navigator navigator, Playlist playlist) {
    super(title, navigator);
    this.items = items;
    this.title = title;
    this.playlist = playlist;
    reorderTracksCommand = new Command(Lang.tr("playlist.reorder_tracks"), Command.SCREEN, 5);
    selectMultipleCommand = new Command(Lang.tr("action.select"), Command.SCREEN, 9);
    addCommand(Commands.addToQueue());
    addCommand(Commands.playerAddToPlaylist());
    addCommand(Commands.download());
    addCommand(Commands.details());
    if (playlist != null && playlist.isCustom()) {
      addCommand(Commands.playlistRemove());
      addCommand(reorderTracksCommand);
    }
    addCommand(selectMultipleCommand);
    populateItems();
  }

  protected void populateItems() {
    tracks = items.getTracks();
    nowPlayingIndex = findNowPlayingIndex();
    recomputeRowTexts();
    for (int i = 0; i < tracks.length; i++) {
      this.append(rowTexts[i], Configuration.musicIcon);
    }
    String[] artUrls = new String[tracks.length];
    for (int i = 0; i < tracks.length; i++) {
      artUrls[i] = Utils.withArtType(tracks[i].getImageUrl(), 1);
    }
    loadArt(artUrls);
  }

  protected int badgeAt(int row) {
    return BADGE_MUSIC;
  }

  protected void refresh() {
    if (playlist == null || !playlist.isCustom()) {
      return;
    }
    items = FavoritesManager.getInstance().getCustomPlaylistTracks(playlist);
    if (items == null) {
      items = new Tracks();
    }
    super.refresh();
  }

  protected void showNotify() {
    super.showNotify();
    if (!reorderMode) {
      refreshView();
      startMarkerRefresh();
    }
  }

  protected void hideNotify() {
    super.hideNotify();
    stopMarkerRefresh();
  }

  private void startMarkerRefresh() {
    stopMarkerRefresh();
    markerTimer = new Timer();
    markerTimer.schedule(
        new TimerTask() {
          public void run() {
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    refreshView();
                  }
                });
          }
        },
        1500L,
        1500L);
  }

  private void stopMarkerRefresh() {
    if (markerTimer != null) {
      markerTimer.cancel();
      markerTimer = null;
    }
  }

  protected void handleSelection() {
    if (reorderMode) {
      reorderSelection(getSelectedIndex());
      return;
    }
    int index = getSelectedIndex();
    if (!isValidSelection(index, tracks.length)) {
      return;
    }
    PlayerNavHelper.playTrackFromList(title, items, index, 0L, navigator);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (reorderMode) {
      if (c == Commands.formSave()) saveReorder();
      else if (c == Commands.formCancel()) cancelReorder();
      return;
    }
    if (c == Commands.addToQueue()) {
      addToQueueSelected();
    } else if (c == Commands.playerAddToPlaylist()) {
      addToPlaylist();
    } else if (c == Commands.playlistRemove()) {
      removeFromPlaylist();
    } else if (c == Commands.details()) {
      showTrackDetails();
    } else if (c == Commands.download()) {
      downloadSelected();
    } else if (c == reorderTracksCommand) {
      startReorder();
    } else if (c == selectMultipleCommand) {
      int source = playlist != null && playlist.isCustom()
          ? MultiTrackSelectScreen.SOURCE_PLAYLIST : MultiTrackSelectScreen.SOURCE_TRACKS;
      navigator.forward(new MultiTrackSelectScreen(navigator, title, tracks, source, playlist, this));
    }
  }

  private void startReorder() {
    if (playlist == null || !playlist.isCustom() || tracks == null) return;
    reorderMode = true;
    reorderPick = -1;
    reorderTracks = new Track[tracks.length];
    System.arraycopy(tracks, 0, reorderTracks, 0, tracks.length);
    removeCommand(reorderTracksCommand);
    removeCommand(Commands.playlistRemove());
    removeCommand(Commands.back());
    removeCommand(Commands.playerNowPlaying());
    removeCommand(Commands.addToQueue());
    removeCommand(Commands.playerAddToPlaylist());
    removeCommand(Commands.download());
    removeCommand(Commands.details());
    removeCommand(selectMultipleCommand);
    addCommand(Commands.formSave());
    addCommand(Commands.formCancel());
    navigator.showAlert(Lang.tr("settings.reorder.instructions"), AlertType.INFO);
  }

  private void reorderSelection(int index) {
    if (reorderTracks == null || index < 0 || index >= reorderTracks.length) return;
    if (reorderPick < 0) {
      reorderPick = index;
      ListReorder.toggleMarker(this, index, Configuration.SORT_ICON, true);
    } else {
      if (reorderPick != index) {
        ListReorder.swapRows(this, reorderPick, index, Configuration.SORT_ICON);
        Track temp = reorderTracks[reorderPick];
        reorderTracks[reorderPick] = reorderTracks[index];
        reorderTracks[index] = temp;
      } else {
        ListReorder.toggleMarker(this, index, Configuration.SORT_ICON, false);
      }
      reorderPick = -1;
    }
  }

  private void saveReorder() {
    if (FavoritesManager.getInstance().reorderCustomPlaylistTracks(playlist, reorderTracks)) {
      endReorder();
      refresh();
      navigator.showAlert(Lang.tr("settings.reorder.saved"), AlertType.CONFIRMATION);
    } else {
      navigator.showAlert(Lang.tr("playlist.error.reorder_failed"), AlertType.ERROR);
    }
  }

  private void cancelReorder() {
    endReorder();
    refresh();
  }

  private void endReorder() {
    reorderMode = false;
    reorderPick = -1;
    reorderTracks = null;
    removeCommand(Commands.formSave());
    removeCommand(Commands.formCancel());
    addCommand(Commands.back());
    addCommand(Commands.playerNowPlaying());
    addCommand(Commands.addToQueue());
    addCommand(Commands.playerAddToPlaylist());
    addCommand(Commands.download());
    addCommand(Commands.details());
    addCommand(selectMultipleCommand);
    if (playlist != null && playlist.isCustom()) {
      addCommand(Commands.playlistRemove());
      addCommand(reorderTracksCommand);
    }
  }

  private void downloadSelected() {
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex, tracks.length)) {
      return;
    }
    DownloadActions.downloadTrack(navigator, tracks[selectedIndex]);
  }

  private void addToQueueSelected() {
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex, tracks.length)) {
      return;
    }
    Track selectedTrack = tracks[selectedIndex];
    PlayerNavHelper.addToQueue(new Track[] {selectedTrack}, title, navigator);
  }

  private void addToPlaylist() {
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex, tracks.length)) {
      return;
    }
    Track selectedTrack = tracks[selectedIndex];
    PlaylistPickerScreen selectionScreen = new PlaylistPickerScreen(navigator, selectedTrack, this);
    navigator.forward(selectionScreen);
  }

  private void removeFromPlaylist() {
    if (playlist == null || !playlist.isCustom()) {
      return;
    }
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex, tracks.length)) {
      return;
    }
    Track selectedTrack = tracks[selectedIndex];
    FavoritesManager favoritesManager = FavoritesManager.getInstance();
    if (favoritesManager.removeTrackFromPlaylist(playlist, selectedTrack)) {
      navigator.showAlert(Lang.tr("playlist.status.track_removed"), AlertType.CONFIRMATION);
      refresh();
    } else {
      navigator.showAlert(Lang.tr("playlist.error.remove_track_failed"), AlertType.ERROR);
    }
  }

  private void showTrackDetails() {
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex, tracks.length)) {
      return;
    }
    Track selectedTrack = tracks[selectedIndex];
    TrackDetailScreen detailScreen = new TrackDetailScreen(selectedTrack, navigator);
    navigator.forward(detailScreen);
  }

  private void refreshView() {
    int newIndex = findNowPlayingIndex();
    if (newIndex != nowPlayingIndex) {
      nowPlayingIndex = newIndex;
      recomputeRowTexts();
      applyAllRowTexts();
    }
  }

  private int findNowPlayingIndex() {
    Track current = null;
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      try {
        current = playerScreen.getPlayerGUI().getCurrentTrack();
      } catch (Exception e) {
      }
    }
    if (current == null || tracks == null) {
      return -1;
    }
    for (int i = 0; i < tracks.length; i++) {
      if (current.isSame(tracks[i])) {
        return i;
      }
    }
    return -1;
  }

  private void recomputeRowTexts() {
    if (tracks == null) {
      rowTexts = new String[0];
      return;
    }
    rowTexts = new String[tracks.length];
    for (int i = 0; i < tracks.length; i++) {
      String marker = (i == nowPlayingIndex) ? NOW_PLAYING_MARKER : "";
      rowTexts[i] = marker + buildTrackLabel(tracks[i]);
    }
  }

  private String buildTrackLabel(Track track) {
    String base = track.getDisplayTitle(Lang.tr("details.unknown_artist"));
    int duration = track.getDuration();
    if (duration > 0) {
      return base + "  " + Utils.formatTime(((long) duration) * 1000000L);
    }
    return base;
  }

  private void applyAllRowTexts() {
    if (rowTexts == null) {
      return;
    }
    int count = Math.min(rowTexts.length, this.size());
    for (int i = 0; i < count; i++) {
      this.set(i, rowTexts[i], this.getImage(i));
    }
  }
}
