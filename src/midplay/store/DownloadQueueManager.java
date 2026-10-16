package midplay.store;

import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Gauge;
import midplay.model.Track;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;

/** Serializes bulk downloads and keeps the active/pending queue visible. */
public final class DownloadQueueManager implements CommandListener {
  public static final class Entry {
    public final Track track;
    public final boolean active;
    public final int progress;

    Entry(Track track, boolean active, int progress) {
      this.track = track;
      this.active = active;
      this.progress = progress;
    }
  }

  private static DownloadQueueManager instance;
  private static final Command MINIMIZE_COMMAND =
      new Command(Lang.tr("download.minimize"), Command.SCREEN, 1);

  private final Vector pending = new Vector();
  private Navigator navigator;
  private Track current;
  private int progress;
  private int completed;
  private int failed;
  private boolean running;
  private boolean minimized;
  private Alert progressAlert;
  private Gauge progressGauge;

  public static synchronized DownloadQueueManager getInstance() {
    if (instance == null) instance = new DownloadQueueManager();
    return instance;
  }

  private DownloadQueueManager() {}

  public void enqueue(Navigator navigator, Track[] tracks) {
    if (navigator == null || tracks == null || tracks.length == 0) return;
    boolean start;
    int added = 0;
    synchronized (this) {
      this.navigator = navigator;
      if (!running) {
        completed = failed = 0;
        minimized = false;
      }
      DownloadManager manager = DownloadManager.getInstance();
      for (int i = 0; i < tracks.length; i++) {
        Track track = tracks[i];
        if (track == null || manager.isDownloaded(track) || manager.isDownloadActive(track)
            || contains(track)) continue;
        pending.addElement(track);
        added++;
      }
      start = !running && pending.size() > 0;
      if (start) running = true;
    }
    if (added == 0) {
      navigator.showAlert(Lang.tr("bulk.download_none"), AlertType.INFO);
      return;
    }
    showProgress();
    if (start) startNext();
  }

  public synchronized Entry[] getQueueSnapshot() {
    int count = pending.size() + (current == null ? 0 : 1);
    Entry[] result = new Entry[count];
    int index = 0;
    if (current != null) result[index++] = new Entry(current, true, progress);
    for (int i = 0; i < pending.size(); i++) {
      result[index++] = new Entry((Track) pending.elementAt(i), false, 0);
    }
    return result;
  }

  public synchronized boolean isRunning() {
    return running;
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == MINIMIZE_COMMAND) {
      minimize();
    }
  }

  private boolean contains(Track track) {
    if (current != null && current.isSame(track)) return true;
    for (int i = 0; i < pending.size(); i++) {
      if (((Track) pending.elementAt(i)).isSame(track)) return true;
    }
    return false;
  }

  private void startNext() {
    final Track track;
    synchronized (this) {
      if (pending.size() == 0) {
        current = null;
        running = false;
        finishQueue();
        return;
      }
      current = track = (Track) pending.elementAt(0);
      pending.removeElementAt(0);
      progress = 0;
    }
    showProgress();
    DownloadManager.getInstance().downloadAsync(
        track,
        new DownloadManager.DownloadListener() {
          public void onStart() {
            showProgress();
          }

          public void onProgress(int percent) {
            synchronized (DownloadQueueManager.this) {
              if (current != track) return;
              progress = percent < 0 ? 0 : percent;
            }
            showProgress();
          }

          public void onDone(String path) {
            synchronized (DownloadQueueManager.this) {
              if (current != track) return;
              completed++;
              current = null;
            }
            dispatchNext();
          }

          public void onError(String message) {
            synchronized (DownloadQueueManager.this) {
              if (current != track) return;
              failed++;
              current = null;
            }
            dispatchNext();
          }
        });
  }

  private void dispatchNext() {
    Navigator nav;
    synchronized (this) {
      nav = navigator;
    }
    if (nav != null) {
      nav.callSerially(
          new Runnable() {
            public void run() {
              startNext();
            }
          });
    }
  }

  private void showProgress() {
    final Navigator nav;
    final String message;
    final int value;
    final boolean create;
    synchronized (this) {
      nav = navigator;
      if (nav == null || minimized || !running) return;
      message = progressMessage();
      value = progress;
      create = progressAlert == null;
      if (create) {
        progressAlert = new Alert(null, message, null, AlertType.INFO);
        progressAlert.setTimeout(Alert.FOREVER);
        progressAlert.addCommand(MINIMIZE_COMMAND);
        progressAlert.setCommandListener(this);
        progressGauge = new Gauge(null, false, 100, value);
        progressAlert.setIndicator(progressGauge);
      }
    }
    if (create) {
      nav.forward(progressAlert);
    } else {
      nav.callSerially(
          new Runnable() {
            public void run() {
              Alert alert;
              Gauge gauge;
              synchronized (DownloadQueueManager.this) {
                if (minimized || !running) return;
                alert = progressAlert;
                gauge = progressGauge;
              }
              if (alert != null) alert.setString(message);
              if (gauge != null) gauge.setValue(value);
            }
          });
    }
  }

  private synchronized String progressMessage() {
    if (current == null) return Lang.tr("download.queue_waiting", Integer.toString(pending.size()));
    String name = current.getName() == null ? "" : current.getName();
    return Lang.tr("download.queue_active", name) + " (" + progress + "%); "
        + Lang.tr("download.queue_remaining", Integer.toString(pending.size()));
  }

  private void minimize() {
    Navigator nav;
    synchronized (this) {
      minimized = true;
      progressAlert = null;
      progressGauge = null;
      nav = navigator;
    }
    if (nav != null) nav.dismissAlert();
  }

  private void finishQueue() {
    Navigator nav = navigator;
    boolean show = !minimized && progressAlert != null;
    progressAlert = null;
    progressGauge = null;
    if (nav == null || !show) return;
    nav.dismissAlert();
    nav.showAlert(
        Lang.tr("download.queue_finished", completed + "; " + failed),
        failed == 0 ? AlertType.CONFIRMATION : AlertType.WARNING);
  }
}
