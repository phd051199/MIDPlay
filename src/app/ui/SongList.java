package app.ui;

import app.MIDPlay;
import app.core.data.AsyncDataManager;
import app.core.data.DataLoader;
import app.core.data.DataParser;
import app.core.data.LoadDataListener;
import app.core.data.LoadDataObserver;
import app.core.service.FavoritesService;
import app.core.service.PlaylistSongService;
import app.models.Playlist;
import app.models.Song;
import app.ui.player.PlayerCanvas;
import app.utils.concurrent.ThreadManager;
import app.utils.text.LocalizationManager;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class SongList extends List implements CommandListener, LoadDataObserver {
  public static PlayerCanvas playerCanvas = null;

  protected Command nowPlayingCommand;
  protected Command exitCommand;
  private Command searchCommand;
  private Command addToPlaylistCommand;
  private Command removeFromPlaylistCommand;

  private final Vector images;
  protected final Vector songItems;
  protected MainObserver observer;
  int curPage = 1;
  int perPage = 10;
  private final Playlist playlist;
  private Thread removeSongFromPlaylistThread;
  private Thread addSongToPlaylistThread;
  protected Image defaultImage;

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
    this.nowPlayingCommand = new Command(LocalizationManager.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(LocalizationManager.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(LocalizationManager.tr("search"), Command.SCREEN, 3);
    this.addToPlaylistCommand =
        new Command(LocalizationManager.tr("add_to_playlist"), Command.ITEM, 4);
    this.removeFromPlaylistCommand =
        new Command(LocalizationManager.tr("remove_from_playlist"), Command.ITEM, 5);

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
        if (song.getSongName().equals(LocalizationManager.tr("load_more"))) {
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
      showAlert(LocalizationManager.tr("alert_no_custom_playlists"), AlertType.INFO);
      return;
    }

    final List playlistList = new List(LocalizationManager.tr("select_playlist"), List.IMPLICIT);

    for (int i = 0; i < customPlaylists.size(); i++) {
      FavoritesList.FavoriteItem item = (FavoritesList.FavoriteItem) customPlaylists.elementAt(i);
      try {
        playlistList.append(item.data.getString("name"), null);
      } catch (Exception e) {
        playlistList.append(LocalizationManager.tr("playlist") + " " + i, null);
      }
    }

    final Command cancelAddToPlaylistCommand =
        new Command(LocalizationManager.tr("cancel"), Command.BACK, 2);
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
                    ThreadManager.createThread(
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
                ThreadManager.safeStartThread(addSongToPlaylistThread);
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
      return FavoritesService.getInstance().getCustomPlaylists();
    } catch (Exception e) {
      showAlert(e.toString(), AlertType.ERROR);
      return new Vector();
    }
  }

  private void addSongToCustomPlaylist(final Song song, final FavoritesList.FavoriteItem playlist) {
    try {
      String playlistId = playlist.data.getString("id");

      PlaylistSongService.getInstance().addSongToPlaylist(playlistId, song);
      showAlert(LocalizationManager.tr("alert_song_added_to_playlist"), AlertType.CONFIRMATION);
    } catch (Exception e) {
      showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    if (type == AlertType.ERROR) {
      alert.setTimeout(Alert.FOREVER);
    } else {
      alert.setTimeout(2000);
    }
    MIDPlay.getInstance().getDisplay().setCurrent(alert, SongList.this);
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/MusicDoubleNote.png");
    } catch (Exception e) {
    }
  }

  private void loadMoreSongs(final String genKey, final int curPage, final int perPage) {
    AsyncDataManager.getInstance()
        .loadDataAsync(
            new DataLoader() {
              public Vector load() {
                return DataParser.parseSongsInPlaylist(genKey, "", curPage, perPage, "");
              }
            },
            new LoadDataListener() {
              public void loadDataCompleted(Vector listItems) {
                SongList.this.addMorePlaylists(listItems);
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        SongList.this.repaintList();
                      }
                    });
              }

              public void loadError() {}

              public void noData() {}
            });
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
      if (lastSong.getSongName().equals(LocalizationManager.tr("load_more"))) {
        this.songItems.removeElementAt(this.songItems.size() - 1);
      }
    }
    this.loadMoreSongs(this.playlist.getId(), this.curPage, this.perPage);
  }

  public void setObserver(MainObserver _observer) {
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
      showAlert(
          LocalizationManager.tr("alert_cannot_remove_from_non_custom_playlist"),
          AlertType.WARNING);
      return;
    }

    final int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= this.songItems.size()) {
      showAlert(LocalizationManager.tr("alert_no_song_selected"), AlertType.WARNING);
      return;
    }

    final Song selectedSong = (Song) this.songItems.elementAt(selectedIndex);

    if (selectedSong.getStreamUrl() == null || selectedSong.getStreamUrl().trim().length() == 0) {
      showAlert(LocalizationManager.tr("alert_cannot_remove_song_no_id"), AlertType.ERROR);
      return;
    }

    removeSongFromPlaylistThread =
        ThreadManager.createThread(
            new Runnable() {
              public void run() {
                try {
                  boolean success = removeSongFromPlaylist(selectedSong, playlist.getId());
                  if (success) {
                    ThreadManager.runOnUiThread(
                        new Runnable() {
                          public void run() {
                            songItems.removeElementAt(selectedIndex);
                            delete(selectedIndex);
                            showAlert(
                                LocalizationManager.tr("alert_song_removed_from_playlist"),
                                AlertType.CONFIRMATION);
                          }
                        });
                  } else {
                    ThreadManager.runOnUiThread(
                        new Runnable() {
                          public void run() {
                            showAlert(
                                LocalizationManager.tr("alert_song_not_found_in_playlist"),
                                AlertType.ERROR);
                          }
                        });
                  }
                } catch (final Exception e) {
                  ThreadManager.runOnUiThread(
                      new Runnable() {
                        public void run() {
                          showAlert(
                              LocalizationManager.tr("alert_error_removing_song")
                                  + ": "
                                  + e.getMessage(),
                              AlertType.ERROR);
                        }
                      });
                }
              }
            },
            "RemoveSongFromPlaylist");
    ThreadManager.safeStartThread(removeSongFromPlaylistThread);
  }

  private boolean removeSongFromPlaylist(Song song, String playlistId) throws Exception {

    String songId = song.getStreamUrl();
    if (songId == null || songId.trim().length() == 0) {

      songId = song.getSongId();
    }

    if (songId == null || songId.trim().length() == 0) {
      throw new Exception("Song has no valid identifier for removal");
    }

    if (playlistId == null || playlistId.trim().length() == 0) {
      throw new Exception("Invalid playlist ID");
    }

    return PlaylistSongService.getInstance().removeSongFromPlaylist(playlistId, songId);
  }
}
