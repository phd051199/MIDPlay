package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.store.Configuration;
import midplay.store.DownloadManager;
import midplay.ui.BaseList;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.util.Lang;
import midplay.util.Utils;

public final class DownloadsScreen extends BaseList {
  private final DownloadManager downloadManager = DownloadManager.getInstance();
  private Track[] tracks = new Track[0];
  private int loadKey = 0;
  private final Command selectMultipleCommand =
      new Command(Lang.tr("action.select"), Command.SCREEN, 1);
  private final Command inProgressCommand =
      new Command(Lang.tr("download.queue"), Command.SCREEN, 7);

  public DownloadsScreen(Navigator navigator) {
    super(Lang.tr("menu.downloads"), navigator);
    addCommand(Commands.downloadPlay());
    addCommand(Commands.downloadDelete());
    addCommand(Commands.downloadDeleteAll());
    addCommand(Commands.downloadRefresh());
    addCommand(Commands.downloadLink());
    addCommand(selectMultipleCommand);
    addCommand(inProgressCommand);
    populateItems();
  }

  protected void populateItems() {
    startLoad(getSelectedIndex());
  }

  protected void refresh() {
    startLoad(getSelectedIndex());
  }

  private void startLoad(final int restoreIndex) {
    final int key = ++loadKey;
    this.deleteAll();
    this.append(Lang.tr("status.loading"), null);
    new Thread(
        new Runnable() {
          public void run() {
            Track[] data = new Track[0];
            boolean failed = false;
            try {
              data = downloadManager.refreshDownloadedTracks();
            } catch (Throwable t) {
              failed = true;
            }
            final Track[] result = data;
            final boolean failure = failed;
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    if (key != loadKey) {
                      return;
                    }
                    tracks = result;
                    DownloadsScreen.this.deleteAll();
                    populateTrackItems();
                    if (failure) {
                      DownloadsScreen.this.append(Lang.tr("status.error"), null);
                    }
                    Utils.clampAndSelect(DownloadsScreen.this, restoreIndex);
                  }
                });
          }
        })
        .start();
  }

  private String buildTrackLabel(Track track) {
    String base = track.getName() != null && track.getName().length() > 0
        ? track.getName()
        : Lang.tr("details.unknown_artist");
    String artist = track.getArtist();
    if (artist != null && artist.length() > 0) {
      base = base + "\n" + artist;
    }
    return base;
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, tracks.length)) {
      return;
    }
    playFrom(index);
  }

  protected void handleCommand(Command c, Displayable d) {
    int selected = getSelectedIndex();
    if (c == Commands.downloadPlay()) {
      if (isValidSelection(selected, tracks.length)) {
        playFrom(selected);
      }
    } else if (c == Commands.downloadDelete()) {
      if (isValidSelection(selected, tracks.length)) {
        deleteSelected(selected);
      }
    } else if (c == Commands.downloadDeleteAll()) {
      confirmClearAll();
    } else if (c == Commands.downloadRefresh()) {
      refresh();
    } else if (c == Commands.downloadLink()) {
      navigator.forward(new LocalFileLinkScreen(navigator));
    } else if (c == selectMultipleCommand) {
      navigator.forward(
          new MultiTrackSelectScreen(
              navigator, Lang.tr("menu.downloads"), tracks,
              MultiTrackSelectScreen.SOURCE_DOWNLOADS, null, this));
    } else if (c == inProgressCommand) {
      navigator.forward(new DownloadQueueScreen(navigator));
    }
  }

  private void playFrom(int index) {
    Tracks all = new Tracks();
    all.setTracks(tracks);
    PlayerNavHelper.playTrackFromList(
        Lang.tr("menu.downloads"), all, index, 0L, navigator);
  }

  private void deleteSelected(final int index) {
    navigator.showConfirmationAlert(
        Lang.tr("download.confirm.delete"),
        new Runnable() {
          public void run() {
            performDelete(index);
          }
        },
        AlertType.WARNING);
  }

  private void performDelete(int index) {
    Track track = tracks[index];
    if (downloadManager.removeDownload(track)) {
      navigator.showAlert(Lang.tr("download.status.deleted"), AlertType.CONFIRMATION);
      refresh();
    } else {
      navigator.showAlert(Lang.tr("download.error"), AlertType.ERROR);
    }
  }

  private void confirmClearAll() {
    if (tracks == null || tracks.length == 0) {
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("download.confirm.clear"),
        new Runnable() {
          public void run() {
            downloadManager.clearAllDownloads();
            navigator.showAlert(Lang.tr("download.status.cleared"), AlertType.CONFIRMATION);
            refresh();
          }
        },
        AlertType.WARNING);
  }

  private void populateTrackItems() {
    if (tracks == null) {
      tracks = new Track[0];
    }
    if (tracks.length == 0) {
      this.append(Lang.tr("status.no_data"), null);
      return;
    }
    String[] artUrls = new String[tracks.length];
    for (int i = 0; i < tracks.length; i++) {
      this.append(buildTrackLabel(tracks[i]), Configuration.musicIcon);
      artUrls[i] = Utils.withArtType(tracks[i].getImageUrl(), 1);
    }
    loadArt(artUrls);
  }

  protected int badgeAt(int row) {
    return BADGE_MUSIC;
  }

  protected void showNotify() {
    super.showNotify();
    refresh();
  }
}
