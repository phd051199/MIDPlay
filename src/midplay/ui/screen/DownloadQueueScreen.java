package midplay.ui.screen;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.model.Track;
import midplay.store.Configuration;
import midplay.store.DownloadQueueManager;
import midplay.ui.BaseList;
import midplay.ui.Navigator;
import midplay.util.Lang;

/** Shows the active download and tracks waiting in the persistent bulk queue. */
public final class DownloadQueueScreen extends BaseList {
  private final Command refreshCommand = new Command(Lang.tr("download.refresh"), Command.SCREEN, 2);
  private Timer refreshTimer;

  public DownloadQueueScreen(Navigator navigator) {
    super(Lang.tr("download.queue"), navigator);
    addCommand(refreshCommand);
    populateItems();
  }

  protected void populateItems() {
    DownloadQueueManager.Entry[] entries = DownloadQueueManager.getInstance().getQueueSnapshot();
    if (entries.length == 0) {
      append(Lang.tr("download.queue_empty"), null);
      return;
    }
    for (int i = 0; i < entries.length; i++) {
      Track track = entries[i].track;
      String name = track == null ? "" : track.getDisplayTitle(Lang.tr("details.unknown_artist"));
      String label;
      if (entries[i].active) {
        label = Lang.tr("download.queue_active", name) + " (" + entries[i].progress + "%)";
      } else {
        label = Lang.tr("download.queue_pending", name);
      }
      append(label, Configuration.musicIcon);
    }
  }

  protected void handleSelection() {}

  protected void handleCommand(Command command, Displayable displayable) {
    if (command == refreshCommand) refresh();
  }

  protected void showNotify() {
    super.showNotify();
    refresh();
    startRefreshTimer();
  }

  protected void hideNotify() {
    stopRefreshTimer();
    super.hideNotify();
  }

  private void startRefreshTimer() {
    stopRefreshTimer();
    refreshTimer = new Timer();
    refreshTimer.schedule(
        new TimerTask() {
          public void run() {
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    refresh();
                  }
                });
          }
        },
        1000L,
        1000L);
  }

  private void stopRefreshTimer() {
    if (refreshTimer != null) {
      refreshTimer.cancel();
      refreshTimer = null;
    }
  }

}
