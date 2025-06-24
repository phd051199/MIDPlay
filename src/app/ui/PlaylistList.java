package app.ui;

import app.MIDPlay;
import app.common.ParseData;
import app.common.ReadWriteRecordStore;
import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.ImageUtils;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import org.json.me.JSONObject;

public class PlaylistList extends List implements CommandListener, LoadDataObserver {

  private Command exitCommand;
  private Command selectCommand;
  private Command nowPlayingCommand;
  private Command searchCommand;
  private Command addToFavoritesCommand;

  private Vector images;
  private Vector playlistItems;
  private Utils.BreadCrumbTrail observer;
  private boolean showAddToFavorites;
  private Image defaultImage;

  String type = "";
  int curPage = 1;
  int perPage = 10;
  String from = "";
  String keyWord = "";
  Thread mLoaDataThread;
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
    this.selectCommand = new Command(I18N.tr("select"), Command.OK, 1);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(I18N.tr("search"), Command.SCREEN, 3);

    this.addCommand(this.selectCommand);
    this.addCommand(this.nowPlayingCommand);

    if (showAddToFavorites) {
      this.addToFavoritesCommand = new Command(I18N.tr("add_to_favorites"), Command.ITEM, 3);
      this.addCommand(this.addToFavoritesCommand);
    }

    this.addCommand(this.exitCommand);
    this.addCommand(this.searchCommand);
    this.setCommandListener(this);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == this.selectCommand || c == List.SELECT_COMMAND) {
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
      e.printStackTrace();
    }
  }

  private void loadMorePlaylists(
      final String keyword, final String genKey, final int curPage, final int perPage) {
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector listItems = null;
                if (PlaylistList.this.from.equals("genre")) {
                  listItems = ParseData.parsePlaylist(curPage, perPage, "hot,new", genKey);

                } else if (PlaylistList.this.from.equals("search")) {
                  listItems =
                      ParseData.parseSearch("", keyword, curPage, perPage, PlaylistList.this.type);

                } else if (PlaylistList.this.from.equals("hot")
                    || PlaylistList.this.from.equals("new")) {
                  listItems =
                      ParseData.parsePlaylist(curPage, perPage, PlaylistList.this.type, "0");
                }

                if (listItems != null) {
                  PlaylistList.this.addMorePlaylists(listItems);
                  PlaylistList.this.repaintList();
                }
              }
            });
    this.mLoaDataThread.start();
  }

  private void initComponents() {
    this.createImages();
    this.deleteAll();
    for (int i = 0; i < this.playlistItems.size(); ++i) {
      Playlist playlist = (Playlist) this.playlistItems.elementAt(i);
      Image imagePart = this.getImage(i);
      this.append(playlist.getName(), imagePart);
    }
    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void repaintList() {
    Display.getDisplay(MIDPlay.getInstance())
        .callSerially(
            new Runnable() {
              public void run() {
                if (isDestroyed) return;

                try {
                  if (size() == 0) {
                    int currentIndex = getSelectedIndex();

                    for (int i = 0; i < playlistItems.size(); ++i) {
                      Playlist playlist = (Playlist) playlistItems.elementAt(i);
                      Image imagePart = getImage(i);
                      append(playlist.getName(), imagePart);
                    }

                    if (currentIndex >= 0 && currentIndex < playlistItems.size()) {
                      setSelectedIndex(currentIndex, true);
                    } else if (playlistItems.size() > 0) {
                      setSelectedIndex(0, true);
                    }
                  }
                } catch (Exception e) {
                  System.err.println("Error repainting list: " + e.getMessage());
                }
              }
            });
  }

  private void createImages() {
    try {
      this.images.removeAllElements();
      for (int i = 0; i < this.playlistItems.size(); ++i) {
        this.images.addElement(this.defaultImage);
      }
      new Thread(
              new Runnable() {
                public void run() {
                  PlaylistList.this.loadAndUpdateImage();
                }
              })
          .start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadAndUpdateImage() {
    int size = this.playlistItems.size();
    int batchSize = 3;

    try {
      for (int i = 0; i < size; i += batchSize) {
        if (isDestroyed) break;

        int endIndex = Math.min(i + batchSize, size);

        final Vector batchImages = new Vector();
        final Vector batchIndexes = new Vector();

        for (int j = i; j < endIndex; j++) {
          try {
            Playlist playlist = (Playlist) this.playlistItems.elementAt(j);
            if (playlist.getImageUrl() != null && !playlist.getImageUrl().equals("")) {
              Image img = ImageUtils.getImage(playlist.getImageUrl(), 48);
              if (img != null) {
                batchImages.addElement(img);
                batchIndexes.addElement(String.valueOf(j));
              }
            }
          } catch (Exception e) {

            continue;
          }
        }

        if (batchImages.size() > 0) {
          final int selected = getSelectedIndex();

          Display.getDisplay(MIDPlay.getInstance())
              .callSerially(
                  new Runnable() {
                    public void run() {
                      if (isDestroyed) return;

                      try {
                        for (int k = 0; k < batchImages.size(); k++) {
                          int index = Integer.parseInt((String) batchIndexes.elementAt(k));
                          Image img = (Image) batchImages.elementAt(k);

                          if (index < images.size() && index < playlistItems.size()) {
                            images.setElementAt(img, index);
                            Playlist playlist = (Playlist) playlistItems.elementAt(index);
                            set(index, playlist.getName(), img);
                          }
                        }

                        if (selected >= 0 && selected < size()) {
                          setSelectedIndex(selected, true);
                        }
                      } catch (Exception e) {
                        System.err.println("Error updating batch images: " + e.getMessage());
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
      e.printStackTrace();
    }
  }

  private void addMorePlaylists(Vector playlists) {
    try {
      int startIndex = this.playlistItems.size();

      for (int i = 0; i < playlists.size(); ++i) {
        Playlist playlist = (Playlist) playlists.elementAt(i);
        this.playlistItems.addElement(playlist);
        this.images.addElement(this.defaultImage);
        this.append(playlist.getName(), this.defaultImage);
      }

      loadMoreImages(startIndex);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadMoreImages(final int startIndex) {
    new Thread(
            new Runnable() {
              public void run() {
                int batchSize = 3;
                int totalSize = playlistItems.size();

                for (int i = startIndex; i < totalSize; i += batchSize) {
                  if (isDestroyed) break;

                  int endIndex = Math.min(i + batchSize, totalSize);
                  final Vector batchImages = new Vector();
                  final Vector batchIndexes = new Vector();

                  for (int j = i; j < endIndex; j++) {
                    try {
                      Playlist playlist = (Playlist) playlistItems.elementAt(j);
                      if (playlist.getImageUrl() != null && !playlist.getImageUrl().equals("")) {
                        Image img = ImageUtils.getImage(playlist.getImageUrl(), 48);
                        if (img != null) {
                          batchImages.addElement(img);
                          batchIndexes.addElement(String.valueOf(j));
                        }
                      }
                    } catch (Exception e) {
                      continue;
                    }
                  }

                  if (batchImages.size() > 0) {
                    final int selected = getSelectedIndex();

                    Display.getDisplay(MIDPlay.getInstance())
                        .callSerially(
                            new Runnable() {
                              public void run() {
                                if (isDestroyed) return;

                                try {
                                  for (int k = 0; k < batchImages.size(); k++) {
                                    int index =
                                        Integer.parseInt((String) batchIndexes.elementAt(k));
                                    Image img = (Image) batchImages.elementAt(k);

                                    if (index < images.size()) {
                                      images.setElementAt(img, index);
                                      Playlist playlist = (Playlist) playlistItems.elementAt(index);
                                      set(index, playlist.getName(), img);
                                    }
                                  }

                                  if (selected >= 0 && selected < size()) {
                                    setSelectedIndex(selected, true);
                                  }
                                } catch (Exception e) {
                                  System.err.println(
                                      "Error updating more images: " + e.getMessage());
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
              }
            })
        .start();
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

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  private void gotoSongByPlaylist() {
    final Playlist playlist = (Playlist) this.playlistItems.elementAt(this.getSelectedIndex());
    this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector songItems =
                    ParseData.parseSongsInPlaylist(
                        playlist.getId(), "", 1, 30, PlaylistList.this.type);
                if (songItems == null) {
                  PlaylistList.this.displayMessage(
                      playlist.getName(), I18N.tr("connection_error"), "error");
                } else if (songItems.size() == 0) {
                  PlaylistList.this.displayMessage(playlist.getName(), I18N.tr("no_data"), "error");
                } else {
                  SongList songList = new SongList(playlist.getName(), songItems, playlist);
                  songList.setObserver(PlaylistList.this.observer);
                  PlaylistList.this.observer.replaceCurrent(songList);
                }
              }
            });
    this.mLoaDataThread.start();
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
      if (this.mLoaDataThread != null && this.mLoaDataThread.isAlive()) {
        this.mLoaDataThread.join();
      }
    } catch (InterruptedException var2) {
      var2.printStackTrace();
    }
  }

  public Image getImage(int index) {
    try {
      if (this.images != null && index >= 0 && index < this.images.size()) {
        return (Image) this.images.elementAt(index);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      return this.defaultImage;
    } catch (Exception e) {
      return null;
    }
  }

  private void addToFavorites() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < this.playlistItems.size()) {
      Playlist playlist = (Playlist) this.playlistItems.elementAt(selectedIndex);
      ReadWriteRecordStore recordStore = new ReadWriteRecordStore("favorites");

      try {
        recordStore.openRecStore();

        Vector records = recordStore.readRecords();
        String newFavoriteId = playlist.getId();
        boolean alreadyExists = false;

        for (int i = 0; i < records.size(); i++) {
          try {
            String record = (String) records.elementAt(i);
            JSONObject existingFavorite = new JSONObject(record);
            if (existingFavorite.getString("id").equals(newFavoriteId)) {
              alreadyExists = true;
              break;
            }
          } catch (Exception e) {

          }
        }

        if (alreadyExists) {
          return;
        }

        JSONObject favorite = new JSONObject();
        favorite.put("id", playlist.getId());
        favorite.put("name", playlist.getName());
        favorite.put("imageUrl", playlist.getImageUrl());

        recordStore.writeRecord(favorite.toString());

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
  }
}
