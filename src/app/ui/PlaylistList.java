package app.ui;

import app.MIDPlay;
import app.core.data.DataLoader;
import app.core.data.DataParser;
import app.core.data.FavoritesCallback;
import app.core.data.FavoritesManager;
import app.core.data.LoadDataListener;
import app.core.data.LoadDataObserver;
import app.core.settings.SettingsManager;
import app.core.threading.ThreadManagerIntegration;
import app.models.Playlist;
import app.utils.I18N;
import app.utils.image.ImageLoadCallback;
import app.utils.image.ImageLoadRequest;
import app.utils.image.ImageLoader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class PlaylistList extends List
    implements CommandListener, LoadDataObserver, ImageLoadCallback {
  private static final int NAVIGATION_DELAY_MS = 300;
  private static final int SELECTION_MONITOR_INTERVAL_MS = 100;
  private static final int SELECTION_MONITOR_DELAY_MS = 50;

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
  private final FavoritesManager favoritesManager;

  String type = "";
  int curPage = 1;
  int perPage = 10;
  String from = "";
  String keyWord = "";

  private ImageLoader imageLoader;

  private boolean isDestroyed = false;
  private boolean isNavigating = false;
  private Timer navigationTimer;
  private TimerTask navigationTask;
  private int lastSelectedIndex = -1;
  private Timer selectionMonitorTimer;
  private TimerTask selectionMonitorTask;

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
    this.favoritesManager = FavoritesManager.getInstance();
    this.imageLoader = ImageLoader.getInstance();
    this.loadDefaultImage();
    this.initializeCommands();
    this.images = new Vector();
    this.playlistItems = items;
    this.type = type;
    this.initComponents();
  }

  private void initializeCommands() {
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
      this.lastSelectedIndex = 0;
    }
    startSelectionMonitoring();
    resumeImageLoading();
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      this.cancel();
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      int selectedItemIndex = getSelectedIndex();
      if (selectedItemIndex >= 0 && selectedItemIndex < this.playlistItems.size()) {
        Playlist playlist = (Playlist) this.playlistItems.elementAt(selectedItemIndex);
        if (playlist.getName().equals(I18N.tr("load_more"))) {
          this.doLoadMoreAction(playlist);
        } else {
          this.stopSelectionMonitoring();
          this.pauseImageLoading();
          this.gotoSongByPlaylist();
        }
      }
    } else if (c == this.nowPlayingCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
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
    ThreadManagerIntegration.loadDataAsync(
        new DataLoader() {
          public Vector load() {
            if (PlaylistList.this.from.equals("genre")) {
              return DataParser.parsePlaylist(curPage, perPage, "hot,new", genKey);
            } else if (PlaylistList.this.from.equals("search")) {
              return DataParser.parseSearch("", keyword, curPage, perPage, PlaylistList.this.type);
            } else if (PlaylistList.this.from.equals("hot")
                || PlaylistList.this.from.equals("new")) {
              return DataParser.parsePlaylist(curPage, perPage, PlaylistList.this.type, "0");
            } else {
              return null;
            }
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(final Vector e) {
            if (e != null && !e.isEmpty()) {
              final int startIndex = playlistItems.size();

              for (int i = 0; i < e.size(); ++i) {
                playlistItems.addElement(e.elementAt(i));
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
                          for (int i = 0; i < e.size(); ++i) {
                            Playlist p = (Playlist) e.elementAt(i);
                            append(p.getName(), defaultImage);
                          }
                        }
                      });

              restartSelectionMonitoring();
              if (!isNavigating) {
                resumeImageLoading();
              }
            }
          }

          public void loadError() {
            displayAlert(I18N.tr("connection_error"), AlertType.ERROR);
          }

          public void noData() {
            displayAlert(I18N.tr("no_results"), AlertType.ERROR);
          }
        });
  }

  public void resumeImageLoading() {
    if (!SettingsManager.getInstance().isLoadPlaylistArtEnabled()) {
      return;
    }

    synchronized (this) {
      if (isDestroyed || isNavigating) {
        return;
      }

      validateTimerState();
      loadVisibleImages();
    }
  }

  private void loadVisibleImages() {
    if (!SettingsManager.getInstance().isLoadPlaylistArtEnabled() || isDestroyed) {
      return;
    }

    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0) {
      selectedIndex = 0;
    }

    int start = Math.max(0, selectedIndex - 2);
    int end = Math.min(playlistItems.size(), selectedIndex + 3);

    for (int i = start; i < end; i++) {
      loadImageAtIndex(i);
    }
  }

  private void loadImageAtIndex(int index) {
    if (index < 0 || index >= playlistItems.size() || index >= images.size() || isDestroyed) {
      return;
    }

    try {
      Image currentImage = (Image) images.elementAt(index);
      if (currentImage != defaultImage && currentImage != null) {
        return;
      }

      Playlist playlist = (Playlist) playlistItems.elementAt(index);
      if (playlist.getImageUrl() != null && playlist.getImageUrl().length() > 0) {
        ImageLoadRequest request = new ImageLoadRequest(playlist.getImageUrl(), 48, index, this);
        imageLoader.loadImage(request);
      }
    } catch (Exception e) {
    }
  }

  public void pauseImageLoading() {
    if (this.imageLoader != null) {
      this.imageLoader.cancelRequestsForCallback(this);
    }
  }

  public void startSelectionMonitoring() {
    synchronized (this) {
      if (selectionMonitorTask != null) {
        selectionMonitorTask.cancel();
        selectionMonitorTask = null;
      }

      if (selectionMonitorTimer == null) {
        selectionMonitorTimer = new Timer();
      }

      selectionMonitorTask =
          new TimerTask() {
            public void run() {
              checkSelectionChange();
            }
          };

      try {
        selectionMonitorTimer.scheduleAtFixedRate(
            selectionMonitorTask, SELECTION_MONITOR_DELAY_MS, SELECTION_MONITOR_INTERVAL_MS);
      } catch (Exception e) {
        selectionMonitorTimer = new Timer();
        selectionMonitorTask =
            new TimerTask() {
              public void run() {
                checkSelectionChange();
              }
            };
        selectionMonitorTimer.scheduleAtFixedRate(
            selectionMonitorTask, SELECTION_MONITOR_DELAY_MS, SELECTION_MONITOR_INTERVAL_MS);
      }
    }
  }

  private void stopSelectionMonitoring() {
    synchronized (this) {
      if (selectionMonitorTask != null) {
        selectionMonitorTask.cancel();
        selectionMonitorTask = null;
      }
    }
  }

  private void restartSelectionMonitoring() {
    synchronized (this) {
      stopSelectionMonitoring();

      if (navigationTask != null) {
        navigationTask.cancel();
        navigationTask = null;
      }

      isNavigating = false;

      try {
        int currentSelection = getSelectedIndex();
        if (currentSelection >= 0 && currentSelection < size()) {
          lastSelectedIndex = currentSelection;
        } else {
          lastSelectedIndex = -1;
        }
      } catch (Exception e) {
        lastSelectedIndex = -1;
      }

      startSelectionMonitoring();
    }
  }

  private void validateTimerState() {
    synchronized (this) {
      if (isDestroyed) {
        return;
      }

      if (selectionMonitorTask == null && !isDestroyed) {
        startSelectionMonitoring();
      }
    }
  }

  private void checkSelectionChange() {
    if (isDestroyed) {
      return;
    }
    try {
      synchronized (this) {
        validateTimerState();

        int currentSelectedIndex = getSelectedIndex();

        if (currentSelectedIndex >= 0
            && currentSelectedIndex < size()
            && currentSelectedIndex != lastSelectedIndex) {
          handleNavigation();
          lastSelectedIndex = currentSelectedIndex;
        }
      }
    } catch (Exception e) {
    }
  }

  private void handleNavigation() {
    synchronized (this) {
      if (!isNavigating) {
        isNavigating = true;
        pauseImageLoading();
      }

      if (navigationTask != null) {
        navigationTask.cancel();
        navigationTask = null;
      }

      if (navigationTimer == null) {
        navigationTimer = new Timer();
      }

      navigationTask =
          new TimerTask() {
            public void run() {
              synchronized (PlaylistList.this) {
                if (!isDestroyed) {
                  isNavigating = false;
                  resumeImageLoading();
                }
              }
            }
          };

      try {
        navigationTimer.schedule(navigationTask, NAVIGATION_DELAY_MS);
      } catch (Exception e) {
        navigationTimer = new Timer();
        navigationTask =
            new TimerTask() {
              public void run() {
                synchronized (PlaylistList.this) {
                  if (!isDestroyed) {
                    isNavigating = false;
                    resumeImageLoading();
                  }
                }
              }
            };
        try {
          navigationTimer.schedule(navigationTask, NAVIGATION_DELAY_MS);
        } catch (Exception ex) {
          isNavigating = false;
        }
      }
    }
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

    favoritesManager.addFavorite(
        playlist,
        new FavoritesCallback() {
          public void onFavoritesLoaded(Vector favorites) {}

          public void onFavoriteRemoved() {}

          public void onCustomPlaylistCreated() {}

          public void onCustomPlaylistRenamed() {}

          public void onCustomPlaylistDeletedWithSongs() {}

          public void onCustomPlaylistSongsLoaded(Vector songs) {}

          public void onFavoriteAdded() {
            if (!isDestroyed) {
              displayAlert(I18N.tr("alert_added_to_favorites"), AlertType.CONFIRMATION);
            }
          }

          public void onError(String message) {
            if (!isDestroyed) {
              if (message.indexOf("already") != -1) {
                displayAlert(I18N.tr("alert_already_in_favorites"), AlertType.INFO);
              } else {
                displayAlert(I18N.tr("alert_error_adding_to_favorites"), AlertType.ERROR);
              }
            }
          }
        });
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }

  private void gotoSongByPlaylist() {
    final Playlist playlist = (Playlist) this.playlistItems.elementAt(this.getSelectedIndex());
    this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");
    ThreadManagerIntegration.loadDataAsync(
        new DataLoader() {
          public Vector load() {
            return DataParser.parseSongsInPlaylist(
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
        });
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    synchronized (this) {
      this.isDestroyed = true;
      this.pauseImageLoading();

      if (selectionMonitorTask != null) {
        selectionMonitorTask.cancel();
        selectionMonitorTask = null;
      }

      if (selectionMonitorTimer != null) {
        selectionMonitorTimer.cancel();
        selectionMonitorTimer = null;
      }

      if (navigationTask != null) {
        navigationTask.cancel();
        navigationTask = null;
      }

      if (navigationTimer != null) {
        navigationTimer.cancel();
        navigationTimer = null;
      }
    }
    ThreadManagerIntegration.cancelPendingDataOperations();
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
    Alert alert = new Alert(null, message, null, messageType);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  public void onImageLoaded(int index, Image image, String requestId) {
    if (isDestroyed || index < 0 || index >= images.size() || index >= size()) {
      return;
    }

    try {
      images.setElementAt(image, index);
      Playlist playlist = (Playlist) playlistItems.elementAt(index);
      set(index, playlist.getName(), image);
    } catch (Exception e) {
    }
  }

  public void onImageLoadFailed(int index, String url, String error, String requestId) {
    if (error != null && error.indexOf("Out of memory") >= 0) {
      if (imageLoader != null) {
        imageLoader.forceGarbageCollection();
      }
    }
  }

  public boolean shouldContinueLoading() {
    return !isDestroyed;
  }
}
