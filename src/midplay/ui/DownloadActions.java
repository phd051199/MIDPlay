package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.model.Track;
import midplay.store.DownloadManager;
import midplay.store.DownloadQueueManager;
import midplay.util.Lang;

public final class DownloadActions {
  public static void downloadTrack(final Navigator navigator, final Track track) {
    if (track == null) {
      navigator.showAlert(Lang.tr("download.error"), AlertType.ERROR);
      return;
    }
    final DownloadManager manager = DownloadManager.getInstance();

    if (manager.isDownloaded(track)) {
      navigator.showAlert(Lang.tr("download.already_downloaded"), AlertType.INFO);
      return;
    }
    if (manager.isDownloadActive(track)) {
      navigator.showAlert(Lang.tr("download.in_progress"), AlertType.INFO);
      return;
    }

    navigator.showLoadingAlert(Lang.tr("download.downloading"));

    manager.downloadAsync(
        track,
        new DownloadManager.DownloadListener() {
          public void onStart() {}

          public void onProgress(int percent) {}

          public void onDone(final String path) {
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    navigator.dismissAlert();
                    navigator.showAlert(Lang.tr("download.completed"), AlertType.CONFIRMATION);
                  }
                });
          }

          public void onError(final String message) {
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    navigator.dismissAlert();
                    navigator.showAlert(Lang.tr("download.error", message), AlertType.ERROR);
                  }
                });
          }
        });
  }

  public static void downloadTracks(final Navigator navigator, final Track[] tracks) {
    if (tracks == null || tracks.length == 0) {
      navigator.showAlert(Lang.tr("bulk.select_empty"), AlertType.INFO);
      return;
    }
    DownloadQueueManager.getInstance().enqueue(navigator, tracks);
  }

  private DownloadActions() {}
}
