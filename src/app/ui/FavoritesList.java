package app.ui;

import app.MIDPlay;
import app.common.Common;
import app.common.ParseData;
import app.common.ReadWriteRecordStore;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.ImageUtils;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class FavoritesList extends List implements CommandListener, LoadDataObserver {

  private Command backCommand;
  private Command selectCommand;
  private Command removeCommand;
  private final Vector favorites;
  private final Utils.BreadCrumbTrail observer;
  private final Vector images;
  private Thread mLoadDataThread;
  private Thread imageLoaderThread;
  private final String type = "playlist";
  private Image defaultImage;
  private boolean isDestroyed = false;

  public FavoritesList(Utils.BreadCrumbTrail observer) {
    super(I18N.tr("favorites"), List.IMPLICIT);
    this.observer = observer;
    this.favorites = new Vector();
    this.images = new Vector();
    this.loadDefaultImage();
    initCommands();
    initComponents();
  }

  private void initCommands() {
    this.backCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.selectCommand = new Command(I18N.tr("select"), Command.OK, 1);
    this.removeCommand = new Command(I18N.tr("remove_from_favorites"), Command.ITEM, 2);

    this.addCommand(backCommand);
    this.addCommand(selectCommand);
    this.addCommand(removeCommand);
    this.setCommandListener(this);
  }

  private void initComponents() {
    this.loadFavorites();
    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/FolderSound.png");
    } catch (Exception e) {
    }
  }

  private void loadFavorites() {
    this.favorites.removeAllElements();
    this.images.removeAllElements();
    this.deleteAll();

    ReadWriteRecordStore recordStore = new ReadWriteRecordStore("favorites");
    RecordEnumeration re = null;
    try {
      recordStore.openRecStore();
      re = recordStore.enumerateRecords(null, null, false);

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecord(recordId);

        if (record.trim().length() == 0) {
          continue;
        }

        JSONObject favoriteJson = new JSONObject(record);
        if (favoriteJson.has("id") && favoriteJson.has("name")) {
          FavoriteItem item = new FavoriteItem(recordId, favoriteJson);
          favorites.addElement(item);
          this.append(favoriteJson.getString("name"), defaultImage);
          this.images.addElement(defaultImage);
        }
      }
    } catch (Exception e) {

    } finally {
      if (re != null) {
        re.destroy();
      }
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
      }
    }

    if (favorites.size() > 0) {
      loadFavoriteImages();
    }
  }

  private void loadFavoriteImages() {
    this.imageLoaderThread =
        new Thread(
            new Runnable() {
              public void run() {
                loadAndUpdateImages();
              }
            });
    this.imageLoaderThread.start();
  }

  private void loadAndUpdateImages() {
    int size = this.favorites.size();
    int batchSize = 3;

    try {
      for (int i = 0; i < size; i += batchSize) {
        if (isDestroyed) {
          break;
        }

        int endIndex = Math.min(i + batchSize, size);
        final Vector batchImages = new Vector();
        final Vector batchIndexes = new Vector();

        for (int j = i; j < endIndex; j++) {
          try {
            FavoriteItem item = (FavoriteItem) this.favorites.elementAt(j);
            JSONObject favorite = item.data;
            if (favorite.has("imageUrl")) {
              String imageUrl = favorite.getString("imageUrl");
              if (imageUrl != null && imageUrl.length() != 0) {
                Image img = ImageUtils.getImage(imageUrl, 48);
                if (img != null) {
                  batchImages.addElement(img);
                  batchIndexes.addElement(String.valueOf(j));
                }
              }
            }
          } catch (Exception e) {
          }
        }

        if (batchImages.size() > 0) {
          final int selected = getSelectedIndex();
          MIDPlay.getInstance()
              .getDisplay()
              .callSerially(
                  new Runnable() {
                    public void run() {
                      if (isDestroyed) {
                        return;
                      }
                      try {
                        for (int k = 0; k < batchImages.size(); k++) {
                          int index = Integer.parseInt((String) batchIndexes.elementAt(k));
                          Image img = (Image) batchImages.elementAt(k);
                          if (index < images.size() && index < favorites.size()) {
                            images.setElementAt(img, index);
                            FavoriteItem item = (FavoriteItem) favorites.elementAt(index);
                            set(index, item.data.getString("name"), img);
                          }
                        }
                        if (selected >= 0 && selected < size()) {
                          setSelectedIndex(selected, true);
                        }
                      } catch (Exception e) {
                      }
                    }
                  });
        }
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          break;
        }
      }
    } catch (Exception e) {
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (c == backCommand) {
      this.cancel();
      this.observer.goBack();
    } else if (c == selectCommand || c == List.SELECT_COMMAND) {
      openSelectedPlaylist();
    } else if (c == removeCommand) {
      removeSelectedFavorite();
    }
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
  }

  private void openSelectedPlaylist() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < favorites.size()) {
      try {
        FavoriteItem selectedItem = (FavoriteItem) favorites.elementAt(selectedIndex);
        JSONObject selected = selectedItem.data;

        if (!selected.has("id") || !selected.has("name")) {
          this.displayMessage(I18N.tr("error"), I18N.tr("invalid_playlist_data"), "error");
          return;
        }

        final Playlist playlist = new Playlist();
        playlist.setId(selected.getString("id"));
        playlist.setName(selected.getString("name"));

        this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");

        Common.loadDataAsync(
            new DataLoader() {
              public Vector load() throws Exception {
                return ParseData.parseSongsInPlaylist(
                    playlist.getId(), "", 1, 30, FavoritesList.this.type);
              }
            },
            new LoadDataListener() {
              public void loadDataCompleted(Vector data) {
                SongList songList = new SongList(playlist.getName(), data, playlist);
                songList.setObserver(FavoritesList.this.observer);
                FavoritesList.this.observer.replaceCurrent(songList);
              }

              public void loadError() {
                displayMessage(playlist.getName(), I18N.tr("connection_error"), "error");
              }

              public void noData() {
                displayMessage(playlist.getName(), I18N.tr("no_data"), "error");
              }
            },
            this.mLoadDataThread);

      } catch (Exception e) {
        this.displayMessage(I18N.tr("error"), I18N.tr("error_loading_playlist"), "error");
      }
    }
  }

  private void removeSelectedFavorite() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    try {
      FavoriteItem itemToRemove = (FavoriteItem) favorites.elementAt(selectedIndex);

      ReadWriteRecordStore recordStore = new ReadWriteRecordStore("favorites");
      try {
        recordStore.openRecStore();
        recordStore.deleteRecord(itemToRemove.recordId);
      } finally {
        try {
          recordStore.closeRecStore();
        } catch (Exception e) {
        }
      }

      favorites.removeElementAt(selectedIndex);
      images.removeElementAt(selectedIndex);

      this.delete(selectedIndex);

      displayAlert(I18N.tr("alert_removed_from_favorites"), AlertType.CONFIRMATION);

    } catch (Exception e) {
      displayAlert(I18N.tr("alert_error_adding_to_favorites"), AlertType.ERROR);
    }
  }

  private void displayAlert(String message, AlertType messageType) {
    Alert alert = new Alert("", message, null, messageType);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  public Image getImage(int index) {
    return (Image)
        (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    this.isDestroyed = true;
    try {
      if (this.mLoadDataThread != null && this.mLoadDataThread.isAlive()) {
        this.mLoadDataThread.join();
      }
      if (this.imageLoaderThread != null && this.imageLoaderThread.isAlive()) {
        this.imageLoaderThread.join();
      }
    } catch (InterruptedException var2) {
    }
  }

  private class FavoriteItem {
    public int recordId;
    public JSONObject data;

    FavoriteItem(int recordId, JSONObject data) {
      this.recordId = recordId;
      this.data = data;
    }
  }
}
