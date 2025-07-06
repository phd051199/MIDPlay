package app.ui;

import app.MIDPlay;
import app.common.ParseData;
import app.common.ReadWriteRecordStore;
import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.model.Song;
import app.ui.player.PlayerCanvas;
import app.utils.I18N;
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

public class SongList extends List implements CommandListener, LoadDataObserver {
  public static PlayerCanvas playerCanvas = null;

  private Command nowPlayingCommand;
  private Command exitCommand;
  private Command searchCommand;
  private Command addToPlaylistCommand;
  private Command removeFromPlaylistCommand;

  private final Vector images;
  private final Vector songItems;
  private Utils.BreadCrumbTrail observer;
  int curPage = 1;
  int perPage = 10;
  private final Playlist playlist;
  Thread mLoadDataThread;
  private Thread removeSongFromPlaylistThread;
  private Thread addSongToPlaylistThread;
  private Image defaultImage;

  public SongList(String title, Vector items, Playlist _playlist) {
    super(title, List.IMPLICIT);
    this.playlist = _playlist;
    this.curPage = 1;
    this.perPage = 10;
    this.loadDefaultImage();
    this.initCommands();
    this.images = new Vector();
    this.songItems = items;
    this.initComponents();
    this.setCommandListener(this);
  }

  private void initCommands() {
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(I18N.tr("search"), Command.SCREEN, 3);
    this.addToPlaylistCommand = new Command(I18N.tr("add_to_playlist"), Command.ITEM, 4);
    this.removeFromPlaylistCommand = new Command(I18N.tr("remove_from_playlist"), Command.ITEM, 5);

    this.addCommand(this.exitCommand);
    this.addCommand(this.searchCommand);
    this.addCommand(this.nowPlayingCommand);
    this.addCommand(this.addToPlaylistCommand);

    if (playlist != null && playlist.getId() != null && playlist.getId().startsWith("custom_")) {
      this.addCommand(this.removeFromPlaylistCommand);
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      int selectedItemIndex = getSelectedIndex();
      if (selectedItemIndex >= 0 && selectedItemIndex < this.songItems.size()) {
        Song song = (Song) this.songItems.elementAt(selectedItemIndex);
        if (song.getSongName().equals(I18N.tr("load_more"))) {
          this.doLoadMoreAction(song);
        } else {
          this.gotoPlaySong();
        }
      }
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      MainList.gotoSearch(this.observer);
    } else if (c == this.addToPlaylistCommand) {
      showAddToPlaylistDialog();
    } else if (c == this.removeFromPlaylistCommand) {
      removeFromCurrentPlaylist();
    }
  }

  private void showAddToPlaylistDialog() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= this.songItems.size()) {
      return;
    }

    final Song selectedSong = (Song) this.songItems.elementAt(selectedIndex);
    final Vector customPlaylists = getCustomPlaylists();

    if (customPlaylists.isEmpty()) {
      showAlert("", I18N.tr("alert_no_custom_playlists"), AlertType.INFO);
      return;
    }

    final List playlistList = new List(I18N.tr("select_playlist"), List.IMPLICIT);

    for (int i = 0; i < customPlaylists.size(); i++) {
      FavoritesList.FavoriteItem item = (FavoritesList.FavoriteItem) customPlaylists.elementAt(i);
      try {
        playlistList.append(item.data.getString("name"), null);
      } catch (Exception e) {
        playlistList.append(I18N.tr("playlist") + " " + i, null);
      }
    }

    final Command cancelAddToPlaylistCommand = new Command(I18N.tr("cancel"), Command.BACK, 2);
    playlistList.addCommand(cancelAddToPlaylistCommand);

    playlistList.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == List.SELECT_COMMAND) {
              final int selectedIndex = playlistList.getSelectedIndex();
              if (selectedIndex >= 0 && selectedIndex < customPlaylists.size()) {
                final FavoritesList.FavoriteItem selectedPlaylist =
                    (FavoritesList.FavoriteItem) customPlaylists.elementAt(selectedIndex);

                MIDPlay.getInstance().getDisplay().setCurrent(SongList.this);

                addSongToPlaylistThread =
                    new Thread(
                        new Runnable() {
                          public void run() {
                            try {
                              addSongToCustomPlaylist(selectedSong, selectedPlaylist);
                            } catch (Exception e) {
                              showAlert("", e.toString(), AlertType.ERROR);
                            }
                          }
                        });
                addSongToPlaylistThread.start();
              } else {
                MIDPlay.getInstance().getDisplay().setCurrent(SongList.this);
              }
            } else if (c == cancelAddToPlaylistCommand) {
              MIDPlay.getInstance().getDisplay().setCurrent(SongList.this);
            }
          }
        });

    MIDPlay.getInstance().getDisplay().setCurrent(playlistList);
  }

  private Vector getCustomPlaylists() {
    Vector customPlaylists = new Vector();
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
        if (favoriteJson.has("isCustom") && favoriteJson.getBoolean("isCustom")) {
          FavoritesList.FavoriteItem item = new FavoritesList.FavoriteItem(recordId, favoriteJson);
          customPlaylists.addElement(item);
        }
      }
    } catch (Exception e) {
      showAlert("", e.toString(), AlertType.ERROR);
    } finally {
      if (re != null) {
        re.destroy();
      }
      try {
        recordStore.closeRecStore();
      } catch (Exception e) {
        showAlert("", e.toString(), AlertType.ERROR);
      }
    }

    return customPlaylists;
  }

  private void addSongToCustomPlaylist(final Song song, final FavoritesList.FavoriteItem playlist) {
    try {
      String playlistId = playlist.data.getString("id");

      String songId = song.getSongId();
      if (songId == null || songId.trim().length() == 0) {
        songId = song.getSongName() + "_" + String.valueOf(System.currentTimeMillis());
        song.setSongId(songId);
      }

      ReadWriteRecordStore songRecordStore = null;
      try {
        songRecordStore = new ReadWriteRecordStore("playlist_songs");
        songRecordStore.openRecStore();

        String relationId = playlistId + "_" + songId;

        JSONObject songJson = new JSONObject();
        songJson.put("relationId", relationId);
        songJson.put("playlistId", playlistId);
        songJson.put("songId", songId);
        songJson.put("name", song.getSongName());
        songJson.put("artist", song.getArtistName());
        songJson.put("image", song.getImage());
        songJson.put("streamUrl", song.getStreamUrl());
        songJson.put("duration", new Integer(song.getDuration()));

        songRecordStore.writeRecord(songJson.toString());
        showAlert("", I18N.tr("alert_song_added_to_playlist"), AlertType.CONFIRMATION);
      } finally {
        try {
          if (songRecordStore != null) {
            songRecordStore.closeRecStore();
          }
        } catch (Exception e) {
        }
      }
    } catch (Exception e) {
      showAlert("", e.toString(), AlertType.ERROR);
    }
  }

  private void showAlert(String title, String message, AlertType type) {
    Alert alert = new Alert(title, message, null, type);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, SongList.this);
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/MusicDoubleNote.png");
    } catch (Exception e) {
    }
  }

  private void loadMoreSongs(final String genKey, final int curPage, final int perPage) {
    this.mLoadDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector listItems = ParseData.parseSongsInPlaylist(genKey, "", curPage, perPage, "");
                if (listItems != null) {
                  SongList.this.addMorePlaylists(listItems);
                  SongList.this.repaintList();
                }
              }
            });
    this.mLoadDataThread.start();
  }

  private void initComponents() {
    this.createImages();
    this.deleteAll();
    for (int i = 0; i < this.songItems.size(); ++i) {
      Song song = (Song) this.songItems.elementAt(i);
      if (song != null) {
        Image imagePart = this.getImage(i);
        this.append(song.getSongName() + " \n" + song.getArtistName(), imagePart);
      }
    }

    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void repaintList() {
    int currentIndex = this.getSelectedIndex();

    try {
      this.deleteAll();

      if (this.songItems != null && this.songItems.size() > 0) {
        for (int i = 0; i < this.songItems.size(); ++i) {
          Song song = (Song) this.songItems.elementAt(i);
          this.append(song.getSongName() + " \n" + song.getArtistName(), this.defaultImage);
        }

        if (currentIndex >= 0 && currentIndex < this.songItems.size()) {
          this.setSelectedIndex(currentIndex, true);
        } else {
          this.setSelectedIndex(0, true);
        }
      }
    } catch (Exception var3) {
    }
  }

  private void createImages() {
    try {
      this.images.removeAllElements();
      for (int i = 0; i < this.songItems.size(); ++i) {
        this.images.addElement(this.defaultImage);
      }
    } catch (Exception var3) {
    }
  }

  private void addMorePlaylists(Vector playlists) {
    for (int i = 0; i < playlists.size(); ++i) {
      this.songItems.addElement(playlists.elementAt(i));
    }
  }

  private void doLoadMoreAction(Song song) {
    this.curPage++;
    if (this.songItems.size() > 0) {
      Song lastSong = (Song) this.songItems.elementAt(this.songItems.size() - 1);
      if (lastSong.getSongName().equals(I18N.tr("load_more"))) {
        this.songItems.removeElementAt(this.songItems.size() - 1);
      }
    }
    this.loadMoreSongs(this.playlist.getId(), this.curPage, this.perPage);
  }

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  private void gotoPlaySong() {
    if (playerCanvas == null) {
      playerCanvas =
          new PlayerCanvas(
              this.playlist.getName(), this.songItems, this.getSelectedIndex(), this.playlist);
    } else {
      playerCanvas.change(
          this.playlist.getName(), this.songItems, this.getSelectedIndex(), this.playlist);
    }
    this.play();
  }

  void play() {
    playerCanvas.setObserver(this.observer);
    this.observer.go(playerCanvas);
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoadDataThread != null && this.mLoadDataThread.isAlive()) {
        this.mLoadDataThread.join();
      }
      if (this.removeSongFromPlaylistThread != null
          && this.removeSongFromPlaylistThread.isAlive()) {
        this.removeSongFromPlaylistThread.join();
      }
      if (this.addSongToPlaylistThread != null && this.addSongToPlaylistThread.isAlive()) {
        this.addSongToPlaylistThread.join();
      }
    } catch (InterruptedException var2) {
    }
  }

  public Image getImage(int index) {
    return (Image)
        (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
  }

  private void removeFromCurrentPlaylist() {
    if (playlist == null || !playlist.getId().startsWith("custom_")) {
      return;
    }

    final int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= this.songItems.size()) {
      return;
    }

    final Song selectedSong = (Song) this.songItems.elementAt(selectedIndex);

    Alert confirmAlert =
        new Alert("", I18N.tr("confirm_remove_song_from_playlist"), null, AlertType.CONFIRMATION);

    confirmAlert.setTimeout(Alert.FOREVER);

    final Command yesCommand = new Command(I18N.tr("yes"), Command.OK, 1);
    final Command noCommand = new Command(I18N.tr("no"), Command.CANCEL, 2);

    confirmAlert.addCommand(yesCommand);
    confirmAlert.addCommand(noCommand);

    confirmAlert.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == yesCommand) {
              removeSongFromPlaylistThread =
                  new Thread(
                      new Runnable() {
                        public void run() {
                          try {
                            boolean success =
                                removeSongFromPlaylist(selectedSong, playlist.getId());
                            if (success) {
                              MIDPlay.getInstance()
                                  .getDisplay()
                                  .callSerially(
                                      new Runnable() {
                                        public void run() {
                                          songItems.removeElementAt(selectedIndex);
                                          delete(selectedIndex);
                                          showAlert(
                                              "",
                                              I18N.tr("alert_song_removed_from_playlist"),
                                              AlertType.CONFIRMATION);
                                        }
                                      });
                            } else {
                              showAlert("", I18N.tr("alert_error_removing_song"), AlertType.ERROR);
                            }
                          } catch (Exception e) {
                            showAlert("", e.toString(), AlertType.ERROR);
                          }
                        }
                      });
              removeSongFromPlaylistThread.start();
            }
            MIDPlay.getInstance().getDisplay().setCurrent(SongList.this);
          }
        });

    MIDPlay.getInstance().getDisplay().setCurrent(confirmAlert);
  }

  private boolean removeSongFromPlaylist(Song song, String playlistId) {
    ReadWriteRecordStore songRecordStore = new ReadWriteRecordStore("playlist_songs");
    RecordEnumeration re = null;
    boolean success = false;

    try {
      songRecordStore.openRecStore();
      re = songRecordStore.enumerateRecords(null, null, false);

      String songId = song.getSongId();

      while (re.hasNextElement()) {
        try {
          int recordId = re.nextRecordId();
          String record = songRecordStore.getRecord(recordId);

          if (record != null && record.trim().length() > 0) {
            JSONObject relationJson = new JSONObject(record);

            if (relationJson.has("playlistId")
                && relationJson.getString("playlistId").equals(playlistId)
                && relationJson.has("songId")
                && relationJson.getString("songId").equals(songId)) {
              songRecordStore.deleteRecord(recordId);
              success = true;
              break;
            }
          }
        } catch (Exception e) {
        }
      }
    } catch (Exception e) {
      return false;
    } finally {
      try {
        if (re != null) {
          re.destroy();
        }
      } catch (Exception e) {
      }

      try {
        if (songRecordStore != null) {
          songRecordStore.closeRecStore();
        }
      } catch (Exception e) {
      }
    }

    return success;
  }
}
