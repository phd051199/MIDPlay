package app.ui;

import app.MIDPlay;
import app.core.data.DataParser;
import app.core.data.FavoritesCallback;
import app.core.data.FavoritesManager;
import app.core.data.LoadDataObserver;
import app.core.storage.RecordStoreManager;
import app.core.threading.ThreadManagerIntegration;
import app.models.Playlist;
import app.models.Song;
import app.ui.player.PlayerCanvas;
import app.utils.I18N;
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
  private MainObserver observer;
  int curPage = 1;
  int perPage = 10;
  private final Playlist playlist;
  private Image defaultImage;
  private final FavoritesManager favoritesManager;

  public SongList(String title, Vector items, Playlist _playlist) {
    super(title, List.IMPLICIT);
    this.playlist = _playlist;
    this.favoritesManager = FavoritesManager.getInstance();
    this.curPage = 1;
    this.perPage = 10;
    this.loadDefaultImage();
    this.initializeCommands();
    this.images = new Vector();
    this.songItems = items;
    this.initComponents();
    this.setCommandListener(this);
  }

  private void initializeCommands() {
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
      showAlert(I18N.tr("alert_no_custom_playlists"), AlertType.INFO);
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

                ThreadManagerIntegration.executeBackgroundTask(
                    new Runnable() {
                      public void run() {
                        try {
                          addSongToCustomPlaylist(selectedSong, selectedPlaylist);
                        } catch (Exception e) {
                          showAlert(e.toString(), AlertType.ERROR);
                        }
                      }
                    },
                    "AddSongToPlaylist");
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
    try {
      return favoritesManager.getCustomPlaylists();
    } catch (Exception e) {
      showAlert(e.toString(), AlertType.ERROR);
      return new Vector();
    }
  }

  private void addSongToCustomPlaylist(final Song song, final FavoritesList.FavoriteItem playlist) {
    favoritesManager.addSongToCustomPlaylist(
        song,
        playlist,
        new FavoritesCallback() {
          public void onFavoritesLoaded(Vector favorites) {}

          public void onFavoriteRemoved() {}

          public void onCustomPlaylistCreated() {}

          public void onCustomPlaylistRenamed() {}

          public void onCustomPlaylistDeletedWithSongs() {}

          public void onCustomPlaylistSongsLoaded(Vector songs) {}

          public void onFavoriteAdded() {
            showAlert(I18N.tr("alert_song_added_to_playlist"), AlertType.CONFIRMATION);
          }

          public void onError(String message) {
            showAlert(message, AlertType.ERROR);
          }
        });
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
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
    ThreadManagerIntegration.executeBackgroundTask(
        new Runnable() {
          public void run() {
            Vector listItems = DataParser.parseSongsInPlaylist(genKey, "", curPage, perPage, "");
            if (listItems != null) {
              SongList.this.addMorePlaylists(listItems);
              SongList.this.repaintList();
            }
          }
        },
        "LoadMoreSongs");
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
    } catch (Exception e) {
    }
  }

  private void createImages() {
    try {
      this.images.removeAllElements();
      for (int i = 0; i < this.songItems.size(); ++i) {
        this.images.addElement(this.defaultImage);
      }
    } catch (Exception e) {
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

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }

  public void setCurrentlyPlayingIndex(int index) {
    if (index >= 0 && index < this.songItems.size()) {
      this.setSelectedIndex(index, true);
    }
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
    ThreadManagerIntegration.cancelPendingDataOperations();
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

    ThreadManagerIntegration.executeBackgroundTask(
        new Runnable() {
          public void run() {
            try {
              boolean success = removeSongFromPlaylist(selectedSong, playlist.getId());
              if (success) {
                MIDPlay.getInstance()
                    .getDisplay()
                    .callSerially(
                        new Runnable() {
                          public void run() {
                            songItems.removeElementAt(selectedIndex);
                            delete(selectedIndex);
                            showAlert(
                                I18N.tr("alert_song_removed_from_playlist"),
                                AlertType.CONFIRMATION);
                          }
                        });
              } else {
                showAlert(I18N.tr("alert_error_removing_song"), AlertType.ERROR);
              }
            } catch (Exception e) {
              showAlert(e.toString(), AlertType.ERROR);
            }
          }
        },
        "RemoveSongFromPlaylist");
  }

  private boolean removeSongFromPlaylist(Song song, String playlistId) {
    RecordStoreManager songRecordStore = new RecordStoreManager("playlist_songs");
    RecordEnumeration re = null;
    boolean success = false;

    try {
      re = songRecordStore.enumerateRecords();

      String songId = song.getSongId();

      while (re.hasNextElement()) {
        try {
          int recordId = re.nextRecordId();
          String record = songRecordStore.getRecordAsString(recordId);

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
      songRecordStore.closeRecordStore();
    }

    return success;
  }
}
