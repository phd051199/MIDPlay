package midplay.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import midplay.net.SpriteSheetOperation;
import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.util.Utils;

public abstract class BaseList extends List implements CommandListener {
  protected final Navigator navigator;

  private static final int ART_CELL = 36;
  private static final int ART_COLS = 4;

  protected static final int BADGE_NONE = 0;
  protected static final int BADGE_FOLDER = 1;
  protected static final int BADGE_MUSIC = 2;

  private SpriteSheetOperation artOp;
  private int artKey = 0;
  private boolean artInFlight = false;
  private boolean artDone = false;
  private String[] artUrls;

  public BaseList(String title, Navigator navigator) {
    super(title, List.IMPLICIT);
    this.navigator = navigator;
    setupCommands();
    setCommandListener(this);
  }

  private void setupCommands() {
    addCommand(Commands.back());
    addCommand(Commands.playerNowPlaying());
  }

  public void commandAction(Command c, Displayable d) {
    try {
      if (c == List.SELECT_COMMAND) {
        handleSelection();
      } else if (c == Commands.back()) {
        navigator.back();
      } else if (c == Commands.playerNowPlaying()) {
        PlayerNavHelper.showNowPlaying(navigator);
      } else {
        handleCommand(c, d);
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  protected abstract void populateItems();

  protected static boolean isValidSelection(int index, int size) {
    return index >= 0 && index < size;
  }

  protected void refresh() {
    int index = getSelectedIndex();
    this.deleteAll();
    populateItems();
    Utils.clampAndSelect(this, index);
  }

  protected void loadArt(String[] urls) {
    cancelArt();
    if (SettingsManager.getInstance().getCurrentThumbnails() == Configuration.THUMBNAILS_OFF) {
      artUrls = null;
      artDone = true;
      return;
    }
    artUrls = urls;
    artDone = false;
    if (urls == null || urls.length == 0) {
      artDone = true;
      return;
    }
    String[] tmpUrls = new String[urls.length];
    int[] tmpIdx = new int[urls.length];
    int n = 0;
    for (int i = 0; i < urls.length; i++) {
      String u = urls[i];
      if (u != null && u.length() > 0) {
        tmpUrls[n] = u;
        tmpIdx[n] = i;
        n++;
      }
    }
    if (n == 0) {
      artDone = true;
      return;
    }
    final String[] reqUrls = new String[n];
    final int[] indices = new int[n];
    System.arraycopy(tmpUrls, 0, reqUrls, 0, n);
    System.arraycopy(tmpIdx, 0, indices, 0, n);
    artInFlight = true;
    fetchArt(reqUrls, indices, true);
  }

  protected void loadArtMore(int offset, String[] urls) {
    if (SettingsManager.getInstance().getCurrentThumbnails() == Configuration.THUMBNAILS_OFF) {
      return;
    }
    if (urls == null || urls.length == 0) {
      return;
    }
    String[] tmpUrls = new String[urls.length];
    int[] tmpIdx = new int[urls.length];
    int n = 0;
    for (int i = 0; i < urls.length; i++) {
      String u = urls[i];
      if (u != null && u.length() > 0) {
        tmpUrls[n] = u;
        tmpIdx[n] = offset + i;
        n++;
      }
    }
    if (n == 0) {
      return;
    }
    final String[] reqUrls = new String[n];
    final int[] indices = new int[n];
    System.arraycopy(tmpUrls, 0, reqUrls, 0, n);
    System.arraycopy(tmpIdx, 0, indices, 0, n);
    fetchArt(reqUrls, indices, false);
  }

  private void fetchArt(
      final String[] reqUrls, final int[] indices, final boolean trackCompletion) {
    if (artOp != null) {
      artOp.stop();
      artOp = null;
    }
    final int capturedKey = artKey;
    artOp =
        new SpriteSheetOperation(
            reqUrls,
            ART_CELL,
            ART_CELL,
            ART_COLS,
            new SpriteSheetOperation.Listener() {
              public void onSheet(final Image[] cells) {
                navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (capturedKey != artKey) {
                          return;
                        }
                        applyArt(indices, cells);
                        if (trackCompletion) {
                          artInFlight = false;
                          artDone = true;
                        }
                      }
                    });
              }

              public void onError(Exception e) {
                navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (capturedKey != artKey) {
                          return;
                        }
                        if (trackCompletion) {
                          artInFlight = false;
                          artDone = true;
                        }
                      }
                    });
              }
            });
    artOp.start();
  }

  private void applyArt(int[] indices, Image[] cells) {
    int n = Math.min(indices.length, cells == null ? 0 : cells.length);
    int size = size();
    for (int k = 0; k < n; k++) {
      int row = indices[k];
      if (row < 0 || row >= size) {
        continue;
      }
      Image img = cells[k];
      if (img == null) {
        continue;
      }
      Image badge = badgeImage(badgeAt(row));
      if (badge != null) {
        img = Utils.stampBadge(img, badge);
      }
      set(row, getString(row), img);
    }
  }

  /** Type of corner badge to stamp on a row's art; BADGE_NONE = leave art as-is. */
  protected int badgeAt(int row) {
    return BADGE_NONE;
  }

  private static Image badgeImage(int type) {
    if (type == BADGE_FOLDER) {
      return Configuration.folderBadgeIcon;
    }
    if (type == BADGE_MUSIC) {
      return Configuration.musicBadgeIcon;
    }
    return null;
  }

  protected void cancelArt() {
    if (artOp != null) {
      artOp.stop();
      artOp = null;
    }
    artInFlight = false;
    artKey++;
  }

  // Recover art only if a load was cancelled by hideNotify before it finished.
  protected void showNotify() {
    if (artUrls != null && !artDone && !artInFlight) {
      loadArt(artUrls);
    }
  }

  protected void hideNotify() {
    cancelArt();
  }

  protected abstract void handleSelection();

  protected abstract void handleCommand(Command c, Displayable d);
}
