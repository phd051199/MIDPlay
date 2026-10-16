package midplay.ui.screen;

import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import midplay.store.Configuration;
import midplay.store.DownloadManager;
import midplay.ui.BaseList;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public final class LocalFileLinkScreen extends BaseList {
  private final DownloadManager downloadManager = DownloadManager.getInstance();
  private String[] filePaths = new String[0];
  private int loadKey = 0;

  public LocalFileLinkScreen(Navigator navigator) {
    super(Lang.tr("download.link"), navigator);
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
            String[] all = new String[0];
            try {
              all = downloadManager.getLocalFilePaths();
              Hashtable linked = downloadManager.getLinkedPaths();
              Vector unlinked = new Vector();
              for (int i = 0; i < all.length; i++) {
                if (!linked.containsKey(all[i])) {
                  unlinked.addElement(all[i]);
                }
              }
              String[] result = new String[unlinked.size()];
              unlinked.copyInto(result);
              all = result;
            } catch (Throwable t) {
              all = new String[0];
            }
            final String[] paths = all;
            navigator.callSerially(
                new Runnable() {
                  public void run() {
                    if (key != loadKey) {
                      return;
                    }
                    filePaths = paths;
                    LocalFileLinkScreen.this.deleteAll();
                    for (int i = 0; i < filePaths.length; i++) {
                      LocalFileLinkScreen.this.append(
                          downloadManager.fileNameForPath(filePaths[i]), Configuration.musicIcon);
                    }
                    if (filePaths.length == 0) {
                      LocalFileLinkScreen.this.append(Lang.tr("download.link_no_files"), null);
                    }
                    Utils.clampAndSelect(LocalFileLinkScreen.this, restoreIndex);
                  }
                });
          }
        })
        .start();
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (!isValidSelection(index, filePaths.length)) {
      return;
    }
    String path = filePaths[index];
    navigator.forward(
        new LinkSearchScreen(
            navigator, this, path, downloadManager.suggestedKeyword(path)));
  }

  protected void handleCommand(Command c, Displayable d) {}

  public void onFileLinked() {
    downloadManager.invalidateListCaches();
    refresh();
  }

  protected void showNotify() {
    super.showNotify();
    refresh();
  }

  protected int badgeAt(int row) {
    return BADGE_MUSIC;
  }
}
