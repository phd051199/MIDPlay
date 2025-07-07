package app.ui;

import app.MIDPlay;
import app.common.Common;
import app.common.ParseData;
import app.common.ReadWriteRecordStore;
import app.common.SettingManager;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.interfaces.MainObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.ImageUtils;
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

public class PlaylistList extends List implements CommandListener, LoadDataObserver {

  private Command exitCommand;
  private Command selectCommand;
  private Command nowPlayingCommand;
  private Command searchCommand;
  private Command addToFavoritesCommand;

  private Vector images;
  private Vector playlistItems;
  private MainObserver observer;
  private boolean showAddToFavorites;
  private Image defaultImage;

  private final int batchSize = 3;

  String type = "";
  int curPage = 1;
  int perPage = 10;
  String from = "";
  String keyWord = "";

  Thread mLoadDataThread;
  Thread imageLoaderThread;

  private boolean isDestroyed = false;

  public PlaylistList(String title, Vector items, String _from, String keySearh, String type) {
    this(title, items, _from, keySearh, type, true);
  }

  public PlaylistList(
      String title,
      Vector items,
      String _from,
      String keySearh,
      String type,
      boolean showAddToFavorites) {
    super(title, List.IMPLICIT);

    this.from = _from;
    this.keyWord = keySearh;
    this.curPage = 1;
    this.perPage = 10;
    this.showAddToFavorites = showAddToFavorites;
    this.loadDefaultImage();
    this.initCommands();
    this.images = new Vector();
    this.playlistItems = items;
    this.type = type;
    this.initComponents();
  }

  private void initCommands() {
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(I18N.tr("search"), Command.SCREEN, 3);
    this.addCommand(this.nowPlayingCommand);

    if (showAddToFavorites) {
      this.addToFavoritesCommand = new Command(I18N.tr("add_to_favorites"), Command.ITEM, 3);
      this.addCommand(this.addToFavoritesCommand);
    }

    this.addCommand(this.exitCommand);
    this.addCommand(this.searchCommand);
    this.setCommandListener(this);
  }

  private void initComponents() {
    this.deleteAll();
    for (int i = 0; i < this.playlistItems.size(); ++i) {
      Playlist playlist = (Playlist) this.playlistItems.elementAt(i);
      this.images.addElement(this.defaultImage);
      this.append(playlist.getName(), this.defaultImage);
    }
    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
    processImageBatchLoading(0);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.cancel();
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      int selectedItemIndex = getSelectedIndex();
      if (selectedItemIndex >= 0 && selectedItemIndex < this.playlistItems.size()) {
        Playlist playlist = (Playlist) this.playlistItems.elementAt(selectedItemIndex);
        if (playlist.getName().equals(I18N.tr("load_more"))) {
          this.doLoadMoreAction(playlist);
        } else {
          this.gotoSongByPlaylist();
        }
      }
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      MainList.gotoSearch(this.observer);
    } else if (this.showAddToFavorites && c == this.addToFavoritesCommand) {
      this.addToFavorites();
    }
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/FolderSound.png");
    } catch (Exception e) {
    }
  }

  private void loadMorePlaylists(
      final String keyword, final String genKey, final int curPage, final int perPage) {
    Common.loadDataAsync(
        new DataLoader() {
          public Vector load() {
            if (PlaylistList.this.from.equals("genre")) {
              return ParseData.parsePlaylist(curPage, perPage, "hot,new", genKey);
            } else if (PlaylistList.this.from.equals("search")) {
              return ParseData.parseSearch("", keyword, curPage, perPage, PlaylistList.this.type);
            } else if (PlaylistList.this.from.equals("hot")
                || PlaylistList.this.from.equals("new")) {
              return ParseData.parsePlaylist(curPage, perPage, PlaylistList.this.type, "0");
            } else {
              return null;
            }
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(final Vector var1) {
            if (var1 != null && !var1.isEmpty()) {
              final int startIndex = playlistItems.size();

              for (int i = 0; i < var1.size(); ++i) {
                playlistItems.addElement(var1.elementAt(i));
                images.addElement(defaultImage);
              }

              MIDPlay.getInstance()
                  .getDisplay()
                  .callSerially(
                      new Runnable() {
                        public void run() {
                          if (isDestroyed) {
                            return;
                          }
                          for (int i = 0; i < var1.size(); ++i) {
                            Playlist p = (Playlist) var1.elementAt(i);
                            append(p.getName(), defaultImage);
                          }
                        }
                      });

              processImageBatchLoading(startIndex);
            }
          }

          public void loadError() {
            displayAlert(I18N.tr("connection_error"), AlertType.ERROR);
          }

          public void noData() {
            displayAlert(I18N.tr("no_results"), AlertType.ERROR);
          }
        },
        this.mLoadDataThread);
  }

  private void processImageBatchLoading(final int startIndex) {
    if (this.imageLoaderThread != null && this.imageLoaderThread.isAlive()) {}

    final boolean shouldLoadImages = SettingManager.getInstance().isLoadPlaylistArtEnabled();

    if (!shouldLoadImages) {
      return;
    }

    this.imageLoaderThread =
        new Thread(
            new Runnable() {
              public void run() {
                int totalSize = playlistItems.size();
                for (int i = startIndex; i < totalSize; i += batchSize) {
                  if (isDestroyed) {
                    break;
                  }

                  int endIndex = Math.min(i + batchSize, totalSize);
                  final Vector batchImages = new Vector();
                  final Vector batchIndexes = new Vector();

                  for (int j = i; j < endIndex; j++) {
                    try {
                      Playlist playlist = (Playlist) playlistItems.elementAt(j);
                      if (playlist.getImageUrl() != null && playlist.getImageUrl().length() > 0) {
                        Image img = ImageUtils.getImage(playlist.getImageUrl(), 48);
                        if (img != null) {
                          batchImages.addElement(img);
                          batchIndexes.addElement(String.valueOf(j));
                        }
                      }
                    } catch (Exception e) {
                    }
                  }

                  if (!batchImages.isEmpty()) {
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

                                    int index =
                                        Integer.parseInt((String) batchIndexes.elementAt(k));
                                    Image img = (Image) batchImages.elementAt(k);
                                    if (index < images.size() && index < size()) {
                                      images.setElementAt(img, index);
                                      Playlist playlist = (Playlist) playlistItems.elementAt(index);
                                      set(index, playlist.getName(), img);
                                    }
                                  }
                                } catch (Exception e) {
                                }
                              }
                            });
                  }

                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                    break;
                  }
                }
              }
            });
    this.imageLoaderThread.start();
  }

  private void doLoadMoreAction(Playlist playlist) {
    this.curPage++;
    if (this.playlistItems.size() > 0) {
      int lastIndex = this.playlistItems.size() - 1;
      Playlist lastPlaylist = (Playlist) this.playlistItems.elementAt(lastIndex);
      if (lastPlaylist.getName().equals(I18N.tr("load_more"))) {
        this.playlistItems.removeElementAt(lastIndex);
        this.images.removeElementAt(lastIndex);
        this.delete(lastIndex);
      }
    }
    this.loadMorePlaylists(this.keyWord, playlist.getId(), this.curPage, this.perPage);
  }

  private void addToFavorites() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= this.playlistItems.size()) {
      return;
    }

    Playlist playlist = (Playlist) this.playlistItems.elementAt(selectedIndex);
    ReadWriteRecordStore recordStore = new ReadWriteRecordStore("favorites");
    RecordEnumeration re = null;
    boolean alreadyExists = false;

    try {
      recordStore.openRecStore();
      re = recordStore.enumerateRecords(null, null, false);
      String newFavoriteId = playlist.getId();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecord(recordId);
        JSONObject existingFavorite = new JSONObject(record);
        if (existingFavorite.has("id") && existingFavorite.getString("id").equals(newFavoriteId)) {
          alreadyExists = true;
          break;
        }
      }

      if (alreadyExists) {
        displayAlert(I18N.tr("alert_already_in_favorites"), AlertType.INFO);
      } else {
        JSONObject favorite = new JSONObject();
        favorite.put("id", playlist.getId());
        favorite.put("name", playlist.getName());
        favorite.put("imageUrl", playlist.getImageUrl());
        recordStore.writeRecord(favorite.toString());
        displayAlert(I18N.tr("alert_added_to_favorites"), AlertType.CONFIRMATION);
      }
    } catch (Exception e) {
      displayAlert(I18N.tr("alert_error_adding_to_favorites"), AlertType.ERROR);
    } finally {
      if (re != null) {
        re.destroy();
      }
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
      }
    }
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }

  private void gotoSongByPlaylist() {
    final Playlist playlist = (Playlist) this.playlistItems.elementAt(this.getSelectedIndex());
    this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");
    Common.loadDataAsync(
        new DataLoader() {
          public Vector load() {
            return ParseData.parseSongsInPlaylist(
                playlist.getId(), "", 1, 30, PlaylistList.this.type);
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            SongList songList = new SongList(playlist.getName(), data, playlist);
            songList.setObserver(PlaylistList.this.observer);
            PlaylistList.this.observer.replaceCurrent(songList);
          }

          public void loadError() {
            PlaylistList.this.displayMessage(
                playlist.getName(), I18N.tr("connection_error"), "error");
          }

          public void noData() {
            PlaylistList.this.displayMessage(playlist.getName(), I18N.tr("no_data"), "error");
          }
        },
        this.mLoadDataThread);
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
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

  public Image getImage(int index) {
    try {
      if (this.images != null && index >= 0 && index < this.images.size()) {
        return (Image) this.images.elementAt(index);
      }
    } catch (Exception e) {
    }
    return this.defaultImage;
  }

  private void displayAlert(String message, AlertType messageType) {
    Alert alert = new Alert("", message, null, messageType);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }
}
