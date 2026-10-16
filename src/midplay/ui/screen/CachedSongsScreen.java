package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Track;
import midplay.store.CacheManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;

public final class CachedSongsScreen extends BaseList {
  private Track[] cachedTracks;

  public CachedSongsScreen(Navigator navigator) {
    super(Lang.tr("cache.title"), navigator);
    addCommand(Commands.cacheListen());
    addCommand(Commands.cacheDelete());
    addCommand(Commands.cacheDeleteAll());
    populateItems();
  }

  protected void populateItems() {
    cachedTracks = CacheManager.getInstance().getAllCached();
    for (int i = 0; i < cachedTracks.length; i++) {
      Track track = cachedTracks[i];
      append(displayName(track), null);
    }
  }

  private String displayName(Track track) {
    String name = track.getName();
    String artist = track.getArtist();
    if (name == null || name.length() == 0) {
      return Lang.tr("cache.unknown");
    }
    if (artist != null && artist.length() > 0) {
      return artist + " - " + name;
    }
    return name;
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, cachedTracks.length)) {
      return;
    }
    listen(cachedTracks[index]);
  }

  private void listen(Track track) {
    if (track == null) {
      return;
    }
    PlayerNavHelper.playSingleTrack(track.getName(), track, navigator);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.cacheListen()) {
      listenCurrent();
    } else if (c == Commands.cacheDelete()) {
      deleteCurrent();
    } else if (c == Commands.cacheDeleteAll()) {
      confirmDeleteAll();
    }
  }

  private void listenCurrent() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, cachedTracks.length)) {
      return;
    }
    listen(cachedTracks[index]);
  }

  private void deleteCurrent() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, cachedTracks.length)) {
      return;
    }
    Track track = cachedTracks[index];
    CacheManager.getInstance().removeTrack(track);
    navigator.showAlert(Lang.tr("cache.status.removed"), AlertType.CONFIRMATION);
    refresh();
  }

  private void confirmDeleteAll() {
    if (cachedTracks.length == 0) {
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("cache.confirm.delete_all"),
        new Runnable() {
          public void run() {
            CacheManager.getInstance().clearAll();
            navigator.showAlert(Lang.tr("cache.status.cleared"), AlertType.CONFIRMATION);
            refresh();
          }
        },
        AlertType.WARNING);
  }
}
