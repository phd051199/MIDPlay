package app.ui;

import app.common.ParseData;
import app.common.ReadWriteRecordStore;
import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import org.json.me.JSONObject;

public class FavoritesList extends List implements CommandListener, LoadDataObserver {

  private Command backCommand;
  private Command selectCommand;
  private Command removeCommand;
  private Vector favorites;
  private final Utils.BreadCrumbTrail observer;
  private Vector images;
  private Thread mLoaDataThread;
  private String type = "playlist";

  public FavoritesList(Utils.BreadCrumbTrail observer) {
    super(I18N.tr("favorites"), List.IMPLICIT);
    this.observer = observer;
    this.images = new Vector();
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
    this.deleteAll();
    this.loadFavorites();

    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void loadFavorites() {
    this.favorites = new Vector();
    this.images.removeAllElements();
    this.deleteAll();

    ReadWriteRecordStore recordStore = new ReadWriteRecordStore("favorites");
    try {
      recordStore.openRecStore();
      Vector records = recordStore.readRecords();
      Image defaultImage = Image.createImage("/images/FolderSound.png");

      for (int i = 0; i < records.size(); i++) {
        try {
          String record = (String) records.elementAt(i);
          if (record == null || record.trim().length() == 0) {
            continue;
          }

          JSONObject favorite = new JSONObject(record);

          if (favorite.has("id") && favorite.has("name")) {
            favorites.addElement(favorite);
            this.append(favorite.getString("name"), defaultImage);
            this.images.addElement(defaultImage);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (c == backCommand) {
      observer.goBack();
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
        JSONObject selected = (JSONObject) favorites.elementAt(selectedIndex);

        if (!selected.has("id") || !selected.has("name")) {
          this.displayMessage(I18N.tr("error"), I18N.tr("invalid_playlist_data"), "error");
          return;
        }

        final Playlist playlist = new Playlist();
        playlist.setId(selected.getString("id"));
        playlist.setName(selected.getString("name"));

        this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");
        this.mLoaDataThread =
            new Thread(
                new Runnable() {
                  public void run() {
                    try {
                      Vector songItems =
                          ParseData.parseSongsInPlaylist(
                              playlist.getId(), "", 1, 30, FavoritesList.this.type);
                      if (songItems == null) {
                        displayMessage(playlist.getName(), I18N.tr("connection_error"), "error");
                      } else if (songItems.size() == 0) {
                        displayMessage(playlist.getName(), I18N.tr("no_data"), "error");
                      } else {
                        SongList songList = new SongList(playlist.getName(), songItems, playlist);
                        songList.setObserver(FavoritesList.this.observer);
                        FavoritesList.this.observer.replaceCurrent(songList);
                      }
                    } catch (Exception e) {
                      e.printStackTrace();
                      displayMessage(I18N.tr("error"), I18N.tr("error_loading_playlist"), "error");
                    }
                  }
                });
        this.mLoaDataThread.start();
      } catch (Exception e) {
        e.printStackTrace();
        this.displayMessage(I18N.tr("error"), I18N.tr("error_loading_playlist"), "error");
      }
    }
  }

  private void removeSelectedFavorite() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < favorites.size()) {
      try {
        JSONObject selected = (JSONObject) favorites.elementAt(selectedIndex);
        String idToRemove = selected.getString("id");

        removeFromFavorites(idToRemove);

        for (int i = 0; i < favorites.size(); i++) {
          JSONObject fav = (JSONObject) favorites.elementAt(i);
          if (fav.getString("id").equals(idToRemove)) {
            favorites.removeElementAt(i);
            break;
          }
        }

        this.deleteAll();
        this.images.removeAllElements();
        Image defaultImage = Image.createImage("/images/FolderSound.png");

        for (int i = 0; i < favorites.size(); i++) {
          JSONObject fav = (JSONObject) favorites.elementAt(i);
          try {
            this.append(fav.getString("name"), defaultImage);
            this.images.addElement(defaultImage);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void removeFromFavorites(String id) {

    int scrollPosition = this.getSelectedIndex();
    if (scrollPosition < 0) scrollPosition = 0;

    ReadWriteRecordStore recordStore = new ReadWriteRecordStore("favorites");
    try {
      recordStore.openRecStore();
      Vector allRecords = recordStore.readRecords();
      boolean found = false;

      for (int i = 0; i < allRecords.size(); i++) {
        try {
          String record = (String) allRecords.elementAt(i);
          if (record == null || record.trim().length() == 0) {
            continue;
          }

          JSONObject favorite = new JSONObject(record);
          if (favorite.has("id") && favorite.getString("id").equals(id)) {
            found = true;
            allRecords.removeElementAt(i);

            if (i < scrollPosition) {
              scrollPosition--;
            }
            break;
          }
        } catch (Exception e) {
          continue;
        }
      }

      if (found) {

        recordStore.closeRecStore();
        recordStore.deleteRecStore();

        recordStore.openRecStore();
        for (int i = 0; i < allRecords.size(); i++) {
          String record = (String) allRecords.elementAt(i);
          if (record != null && record.trim().length() > 0) {
            recordStore.writeRecord(record);
          }
        }

        favorites.removeAllElements();
        this.deleteAll();
        this.images.removeAllElements();

        Vector records = recordStore.readRecords();
        Image defaultImage = Image.createImage("/images/FolderSound.png");

        for (int i = 0; i < records.size(); i++) {
          try {
            String record = (String) records.elementAt(i);
            if (record == null || record.trim().length() == 0) {
              continue;
            }

            JSONObject favorite = new JSONObject(record);
            if (favorite.has("name")) {
              favorites.addElement(favorite);
              this.append(favorite.getString("name"), defaultImage);
              this.images.addElement(defaultImage);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        if (scrollPosition >= 0 && scrollPosition < this.size()) {
          this.setSelectedIndex(scrollPosition, true);
        } else if (this.size() > 0) {

          this.setSelectedIndex(this.size() - 1, true);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public Image getImage(int index) {
    return (Image)
        (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoaDataThread != null && this.mLoaDataThread.isAlive()) {
        this.mLoaDataThread.join();
      }
    } catch (InterruptedException var2) {
      var2.printStackTrace();
    }
  }
}
