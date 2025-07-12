package app.ui;

import app.MIDPlay;
import app.core.data.AsyncDataManager;
import app.core.data.DataLoader;
import app.core.data.DataParser;
import app.core.data.LoadDataListener;
import app.core.data.LoadDataObserver;
import app.core.service.FavoritesService;
import app.core.settings.SettingsManager;
import app.models.Playlist;
import app.utils.concurrent.ThreadManager;
import app.utils.image.ImageLoadCallback;
import app.utils.image.ImageLoadRequest;
import app.utils.image.ImageLoader;
import app.utils.text.LocalizationManager;
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
  private static final int SELECTION_MONITOR_INTERVAL_MS = 200;
  private static final int SELECTION_MONITOR_DELAY_MS = 100;

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
    this.showAddToFavorites = showAddToFavorites;
    this.imageLoader = ImageLoader.getInstance();
    this.loadDefaultImage();
    this.initCommands();
    this.images = new Vector();
    this.playlistItems = items;
    this.type = type;
    this.initComponents();
  }

  private void initCommands() {
    this.nowPlayingCommand = new Command(LocalizationManager.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(LocalizationManager.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(LocalizationManager.tr("search"), Command.SCREEN, 3);
    this.addCommand(this.nowPlayingCommand);

    if (showAddToFavorites) {
      this.addToFavoritesCommand =
          new Command(LocalizationManager.tr("add_to_favorites"), Command.ITEM, 3);
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
    processImageBatchLoading(0);
  }

  public void resumeImageLoading() {
    if (!SettingsManager.getInstance().isLoadPlaylistArtEnabled()) {
      return;
    }

    if (isDestroyed || isNavigating) {
      return;
    }

    int startIndex = findFirstDefaultImageIndex();
    if (startIndex >= 0) {
      processImageBatchLoading(startIndex);
    }
  }

  public void pauseImageLoading() {
    if (this.imageLoader != null) {
      this.imageLoader.cancelRequestsForCallback(this);
    }
  }

  private int findFirstDefaultImageIndex() {
    for (int i = 0; i < images.size(); i++) {
      try {
        Image currentImage = (Image) images.elementAt(i);
        if (currentImage == defaultImage || currentImage == null) {
          return i;
        }
      } catch (Exception e) {
        return i;
      }
    }
    return -1;
  }

  public void startSelectionMonitoring() {
    if (selectionMonitorTimer == null) {
      selectionMonitorTimer = new Timer();
    }

    selectionMonitorTask =
        new TimerTask() {
          public void run() {
            checkSelectionChange();
          }
        };

    selectionMonitorTimer.scheduleAtFixedRate(
        selectionMonitorTask, SELECTION_MONITOR_DELAY_MS, SELECTION_MONITOR_INTERVAL_MS);
  }

  private void stopSelectionMonitoring() {
    if (selectionMonitorTask != null) {
      selectionMonitorTask.cancel();
      selectionMonitorTask = null;
    }
  }

  private void checkSelectionChange() {
    if (isDestroyed) {
      return;
    }
    try {
      int currentSelectedIndex = getSelectedIndex();
      if (currentSelectedIndex != lastSelectedIndex && currentSelectedIndex >= 0) {
        handleNavigation();
        lastSelectedIndex = currentSelectedIndex;
      }
    } catch (Exception e) {
    }
  }

  private void handleNavigation() {
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
      isNavigating = false;
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      this.cancel();
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      this.pauseImageLoading();
      int selectedItemIndex = getSelectedIndex();
      if (selectedItemIndex >= 0 && selectedItemIndex < this.playlistItems.size()) {
        Playlist playlist = (Playlist) this.playlistItems.elementAt(selectedItemIndex);
        if (playlist.getName().equals(LocalizationManager.tr("load_more"))) {
          this.doLoadMoreAction(playlist);
        } else {
          this.stopSelectionMonitoring();
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
    AsyncDataManager.getInstance()
        .loadDataAsync(
            new DataLoader() {
              public Vector load() {
                if (PlaylistList.this.from.equals("genre")) {
                  return DataParser.parsePlaylist(curPage, perPage, "hot,new", genKey);
                } else if (PlaylistList.this.from.equals("search")) {
                  return DataParser.parseSearch(
                      "", keyword, curPage, perPage, PlaylistList.this.type);
                } else if (PlaylistList.this.from.equals("hot")
                    || PlaylistList.this.from.equals("new")) {
                  return DataParser.parsePlaylist(curPage, perPage, PlaylistList.this.type, "0");
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

                  ThreadManager.runOnUiThread(
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
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        displayAlert(LocalizationManager.tr("connection_error"), AlertType.ERROR);
                      }
                    });
              }

              public void noData() {
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        displayAlert(LocalizationManager.tr("no_results"), AlertType.ERROR);
                      }
                    });
              }
            });
  }

  private void processImageBatchLoading(final int startIndex) {
    if (isDestroyed || isNavigating) {
      return;
    }

    final boolean shouldLoadImages = SettingsManager.getInstance().isLoadPlaylistArtEnabled();

    if (!shouldLoadImages) {
      return;
    }

    loadImagesWithImageLoader(startIndex);
  }

  private void loadImagesWithImageLoader(int startIndex) {
    if (isDestroyed || isNavigating) {
      return;
    }

    int totalSize = playlistItems.size();

    for (int i = startIndex; i < totalSize; i++) {
      if (isDestroyed || isNavigating) {
        break;
      }

      try {
        Image currentImage = (Image) images.elementAt(i);
        if (currentImage == defaultImage || currentImage == null) {
          Playlist playlist = (Playlist) playlistItems.elementAt(i);
          if (playlist.getImageUrl() != null && playlist.getImageUrl().length() > 0) {
            if (!isDestroyed && !isNavigating) {
              ImageLoadRequest request = new ImageLoadRequest(playlist.getImageUrl(), 48, i, this);
              imageLoader.loadImage(request);
            }
          }
        }
      } catch (Exception e) {
      }
    }
  }

  private void updateSingleImage(final int index, final Image img) {
    ThreadManager.runOnUiThread(
        new Runnable() {
          public void run() {
            if (isDestroyed || isNavigating) {
              return;
            }
            try {
              int selectedIndex = getSelectedIndex();

              if (index < images.size() && index < playlistItems.size() && index < size()) {
                images.setElementAt(img, index);
                Playlist playlist = (Playlist) playlistItems.elementAt(index);
                set(index, playlist.getName(), img);

                if (selectedIndex >= 0 && selectedIndex < size()) {
                  setSelectedIndex(selectedIndex, false);
                }
              }
            } catch (Exception e) {
            }
          }
        });
  }

  private void doLoadMoreAction(Playlist playlist) {
    this.curPage++;
    if (this.playlistItems.size() > 0) {
      int lastIndex = this.playlistItems.size() - 1;
      Playlist lastPlaylist = (Playlist) this.playlistItems.elementAt(lastIndex);
      if (lastPlaylist.getName().equals(LocalizationManager.tr("load_more"))) {
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

    try {
      boolean alreadyExists = FavoritesService.getInstance().isAlreadyInFavorites(playlist.getId());

      if (alreadyExists) {
        displayAlert(LocalizationManager.tr("alert_already_in_favorites"), AlertType.INFO);
      } else {
        FavoritesService.getInstance()
            .addToFavorites(playlist.getId(), playlist.getName(), playlist.getImageUrl());
        displayAlert(LocalizationManager.tr("alert_added_to_favorites"), AlertType.CONFIRMATION);
      }
    } catch (Exception e) {
      displayAlert(LocalizationManager.tr("alert_error_adding_to_favorites"), AlertType.ERROR);
    }
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }

  private void gotoSongByPlaylist() {
    final Playlist playlist = (Playlist) this.playlistItems.elementAt(this.getSelectedIndex());
    this.displayMessage(playlist.getName(), LocalizationManager.tr("loading"), "loading");
    AsyncDataManager.getInstance()
        .loadDataAsync(
            new DataLoader() {
              public Vector load() {
                return DataParser.parseSongsInPlaylist(
                    playlist.getId(), "", 1, 30, PlaylistList.this.type);
              }
            },
            new LoadDataListener() {
              public void loadDataCompleted(final Vector data) {
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        SongList songList = new SongList(playlist.getName(), data, playlist);
                        songList.setObserver(PlaylistList.this.observer);
                        PlaylistList.this.observer.replaceCurrent(songList);
                      }
                    });
              }

              public void loadError() {
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        PlaylistList.this.displayMessage(
                            playlist.getName(),
                            LocalizationManager.tr("connection_error"),
                            "error");
                      }
                    });
              }

              public void noData() {
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        PlaylistList.this.displayMessage(
                            playlist.getName(), LocalizationManager.tr("no_data"), "error");
                      }
                    });
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

    if (images != null) {
      images.removeAllElements();
      images = null;
    }

    if (playlistItems != null) {
      playlistItems.removeAllElements();
      playlistItems = null;
    }

    defaultImage = null;
    imageLoader = null;
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
    if (isDestroyed || isNavigating) {
      return;
    }
    updateSingleImage(index, image);
  }

  public void onImageLoadFailed(int index, String url, String error, String requestId) {
    if (error != null && error.indexOf("Out of memory") >= 0) {
      if (imageLoader != null) {
        imageLoader.forceGarbageCollection();
      }
    }
  }

  public boolean shouldContinueLoading() {
    return !isDestroyed && !isNavigating;
  }
}
