package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Playlist;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.player.NowPlaying;
import midplay.player.PlayerNavHelper;
import midplay.store.Configuration;
import midplay.store.FavoritesManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public class TrackListScreen extends BaseList {
  // ASCII marker (font-glyph safe on every J2ME handset) prefixing the row that
  // matches the player's current track.
  private static final String NOW_PLAYING_MARKER = "> ";

  // Package-private so the QueueTrackListScreen subclass (sort / save-as-playlist)
  // can reorder tracks and read the backing list without re-exposing them publicly.
  Tracks items;
  final String title;
  final Playlist playlist;
  Track[] tracks;
  String[] rowTexts;
  private int nowPlayingIndex = -1;
  private final boolean playSingleTrack;

  public TrackListScreen(String title, Tracks items, Navigator navigator) {
    this(title, items, navigator, null, false);
  }

  public TrackListScreen(String title, Tracks items, Navigator navigator, Playlist playlist) {
    this(title, items, navigator, playlist, false);
  }

  public TrackListScreen(
      String title, Tracks items, Navigator navigator, Playlist playlist, boolean playSingleTrack) {
    super(title, navigator);
    this.items = items;
    this.title = title;
    this.playlist = playlist;
    this.playSingleTrack = playSingleTrack;
    addCommand(Commands.addToQueue());
    addCommand(Commands.playerAddToPlaylist());
    addCommand(Commands.details());
    if (playlist != null && playlist.isCustom()) {
      addCommand(Commands.playlistRemove());
    }
    populateItems();
  }

  protected void populateItems() {
    tracks = items.getTracks();
    nowPlayingIndex = findNowPlayingIndex();
    recomputeRowTexts();
    for (int i = 0; i < tracks.length; i++) {
      this.append(rowTexts[i], Configuration.musicIcon);
    }
  }

  protected void refresh() {
    if (playlist == null || !playlist.isCustom()) {
      return;
    }
    items = FavoritesManager.getInstance().getCustomPlaylistTracks(playlist);
    if (items == null) {
      items = new Tracks();
    }
    super.refresh(); // deleteAll + populateItems (rebuilds tracks/rowTexts)
  }

  // The now-playing track may have changed since this list was built, so
  // recompute markers on every show — but only touch rows when something
  // actually changed, to avoid a per-show O(n) set() loop on slow devices.
  protected void showNotify() {
    refreshView();
  }

  private boolean isValidSelection(int index) {
    return index >= 0 && index < tracks.length;
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index)) {
      return;
    }
    // playSingleTrack (search results): play only the tapped track so the search
    // list does not become the queue. Records it as a recent standalone play.
    if (playSingleTrack) {
      PlayerNavHelper.playSingleTrack(title, tracks[index], navigator);
    } else {
      PlayerNavHelper.playTrackFromList(title, items, index, 0L, navigator);
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.addToQueue()) {
      addToQueueSelected();
    } else if (c == Commands.playerAddToPlaylist()) {
      addToPlaylist();
    } else if (c == Commands.playlistRemove()) {
      removeFromPlaylist();
    } else if (c == Commands.details()) {
      showTrackDetails();
    }
  }

  private void addToQueueSelected() {
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex)) {
      return;
    }
    Track selectedTrack = tracks[selectedIndex];
    PlayerNavHelper.addToQueue(new Track[] {selectedTrack}, title, navigator);
  }

  private void addToPlaylist() {
    int selectedIndex = getSelectedIndex();
    if (!isValidSelection(selectedIndex)) {
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
    if (!isValidSelection(selectedIndex)) {
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
    if (!isValidSelection(selectedIndex)) {
      return;
    }
    Track selectedTrack = tracks[selectedIndex];
    TrackDetailScreen detailScreen = new TrackDetailScreen(selectedTrack, navigator);
    navigator.forward(detailScreen);
  }

  // --- View refresh: now-playing markers -----------------------------------

  private void refreshView() {
    int newIndex = findNowPlayingIndex();
    if (newIndex != nowPlayingIndex) {
      nowPlayingIndex = newIndex;
      recomputeRowTexts();
      applyAllRowTexts();
    }
  }

  private int findNowPlayingIndex() {
    Track current = NowPlaying.currentTrack();
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

  // Re-applies row text while preserving the row's icon.
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
