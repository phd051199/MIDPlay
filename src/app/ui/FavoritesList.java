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
import app.utils.PlaylistPool;
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
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import org.json.me.JSONObject;

public class FavoritesList extends List
    implements CommandListener, LoadDataObserver, ImageLoadCallback {
  private static final int NAVIGATION_DELAY_MS = 300;
  private static final int SELECTION_MONITOR_INTERVAL_MS = 100;
  private static final int SELECTION_MONITOR_DELAY_MS = 50;

  private Command backCommand;
  private Command removeCommand;
  private Command createPlaylistCommand;
  private Command renamePlaylistCommand;
  private Command nowPlayingCommand;
  private final Vector favorites;
  private final MainObserver observer;
  private final Vector images;
  private final String type = "playlist";
  private Image defaultImage;
  private boolean isDestroyed = false;
  private final FavoritesManager favoritesManager;
  private final ImageLoader imageLoader;

  private boolean isNavigating = false;
  private Timer navigationTimer;
  private TimerTask navigationTask;
  private int lastSelectedIndex = -1;
  private Timer selectionMonitorTimer;
  private TimerTask selectionMonitorTask;

  public FavoritesList(MainObserver observer) {
    super(I18N.tr("favorites"), List.IMPLICIT);
    this.observer = observer;
    this.favorites = new Vector();
    this.images = new Vector();
    this.favoritesManager = FavoritesManager.getInstance();
    this.imageLoader = ImageLoader.getInstance();
    this.loadDefaultImage();
    initializeCommands();
    initComponents();
  }

  private void initializeCommands() {
    this.createPlaylistCommand = new Command(I18N.tr("create_playlist"), Command.SCREEN, 1);
    this.removeCommand = new Command(I18N.tr("remove_from_favorites"), Command.ITEM, 2);
    this.renamePlaylistCommand = new Command(I18N.tr("rename_playlist"), Command.ITEM, 3);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 4);
    this.backCommand = new Command(I18N.tr("back"), Command.BACK, 5);

    this.addCommand(backCommand);
    this.addCommand(removeCommand);
    this.addCommand(createPlaylistCommand);
    this.addCommand(renamePlaylistCommand);
    this.addCommand(nowPlayingCommand);
    this.setCommandListener(this);
  }

  private void initComponents() {
    this.loadFavorites();
    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
      this.lastSelectedIndex = 0;
    }
    startSelectionMonitoring();
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

    favoritesManager.loadFavorites(
        new FavoritesCallback() {
          public void onFavoritesLoaded(Vector loadedFavorites) {
            if (isDestroyed) {
              return;
            }

            for (int i = 0; i < loadedFavorites.size(); i++) {
              try {
                FavoriteItem item = (FavoriteItem) loadedFavorites.elementAt(i);
                favorites.addElement(item);
                append(item.data.getString("name"), defaultImage);
                images.addElement(defaultImage);
              } catch (Exception e) {
              }
            }

            if (favorites.size() > 0) {
              resumeImageLoading();
            }
          }

          public void onFavoriteAdded() {}

          public void onFavoriteRemoved() {}

          public void onCustomPlaylistCreated() {}

          public void onCustomPlaylistRenamed() {}

          public void onCustomPlaylistDeletedWithSongs() {}

          public void onCustomPlaylistSongsLoaded(Vector songs) {}

          public void onError(String message) {
            if (!isDestroyed) {
              displayMessage(I18N.tr("error"), message, "error");
            }
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
    int end = Math.min(favorites.size(), selectedIndex + 3);

    for (int i = start; i < end; i++) {
      loadImageAtIndex(i);
    }
  }

  private void loadImageAtIndex(int index) {
    if (index < 0 || index >= favorites.size() || index >= images.size() || isDestroyed) {
      return;
    }

    try {
      Image currentImage = (Image) images.elementAt(index);
      if (currentImage != defaultImage && currentImage != null) {
        return;
      }

      FavoriteItem item = (FavoriteItem) favorites.elementAt(index);
      JSONObject favorite = item.data;
      if (favorite.has("imageUrl")) {
        String imageUrl = favorite.getString("imageUrl");
        if (imageUrl != null && imageUrl.length() > 0) {
          ImageLoadRequest request = new ImageLoadRequest(imageUrl, 48, index, this);
          imageLoader.loadImage(request);
        }
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
              synchronized (FavoritesList.this) {
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
                synchronized (FavoritesList.this) {
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

  public void commandAction(Command c, Displayable d) {
    if (c == backCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      this.cancel();
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      openSelectedPlaylist();
    } else if (c == removeCommand) {
      removeSelectedFavorite();
    } else if (c == createPlaylistCommand) {
      showCreatePlaylistForm();
    } else if (c == renamePlaylistCommand) {
      showRenamePlaylistForm();
    } else if (c == nowPlayingCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      MainList.gotoNowPlaying(this.observer);
    }
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
  }

  private void openSelectedPlaylist() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < favorites.size()) {
      try {
        final FavoriteItem selectedItem = (FavoriteItem) favorites.elementAt(selectedIndex);
        final JSONObject selected = selectedItem.data;

        if (!selected.has("id") || !selected.has("name")) {
          this.displayMessage(I18N.tr("error"), I18N.tr("invalid_playlist_data"), "error");
          return;
        }

        final Playlist playlist = PlaylistPool.getInstance().borrowPlaylist();
        playlist.setId(selected.getString("id"));
        playlist.setName(selected.getString("name"));

        final boolean isCustomPlaylist =
            selected.has("isCustom") && selected.getBoolean("isCustom");

        if (isCustomPlaylist) {
          this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");

          favoritesManager.loadCustomPlaylistSongs(
              selected.getString("id"),
              new FavoritesCallback() {
                public void onFavoritesLoaded(Vector favorites) {}

                public void onFavoriteAdded() {}

                public void onFavoriteRemoved() {}

                public void onCustomPlaylistCreated() {}

                public void onCustomPlaylistRenamed() {}

                public void onCustomPlaylistDeletedWithSongs() {}

                public void onCustomPlaylistSongsLoaded(Vector customSongs) {
                  if (!isDestroyed) {
                    SongList songList = new SongList(playlist.getName(), customSongs, playlist);
                    songList.setObserver(FavoritesList.this.observer);
                    FavoritesList.this.observer.replaceCurrent(songList);
                  }
                }

                public void onError(String message) {
                  if (!isDestroyed) {
                    displayMessage(I18N.tr("error"), I18N.tr("error_loading_playlist"), "error");
                  }
                }
              });

          return;
        }

        this.displayMessage(playlist.getName(), I18N.tr("loading"), "loading");

        ThreadManagerIntegration.loadDataAsync(
            new DataLoader() {
              public Vector load() throws Exception {
                return DataParser.parseSongsInPlaylist(
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
            });

      } catch (Exception e) {
        this.displayMessage(I18N.tr("error"), I18N.tr("error_loading_playlist"), "error");
      }
    }
  }

  private void removeSelectedFavorite() {
    final int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    try {
      final FavoriteItem itemToRemove = (FavoriteItem) favorites.elementAt(selectedIndex);

      boolean isCustomPlaylist = false;
      try {
        isCustomPlaylist =
            itemToRemove.data.has("isCustom") && itemToRemove.data.getBoolean("isCustom");
      } catch (Exception e) {
      }

      FavoritesCallback callback =
          new FavoritesCallback() {
            public void onFavoritesLoaded(Vector favorites) {}

            public void onFavoriteAdded() {}

            public void onCustomPlaylistCreated() {}

            public void onCustomPlaylistRenamed() {}

            public void onCustomPlaylistSongsLoaded(Vector songs) {}

            public void onFavoriteRemoved() {
              if (!isDestroyed) {
                favorites.removeElementAt(selectedIndex);
                images.removeElementAt(selectedIndex);
                delete(selectedIndex);
                showAlert(I18N.tr("alert_removed_from_favorites"), AlertType.CONFIRMATION);
              }
            }

            public void onCustomPlaylistDeletedWithSongs() {
              if (!isDestroyed) {
                favorites.removeElementAt(selectedIndex);
                images.removeElementAt(selectedIndex);
                delete(selectedIndex);
                showAlert(I18N.tr("alert_removed_from_favorites"), AlertType.CONFIRMATION);
              }
            }

            public void onError(String message) {
              if (!isDestroyed) {
                showAlert(I18N.tr("alert_error_removing_from_favorites"), AlertType.ERROR);
              }
            }
          };

      if (isCustomPlaylist) {
        favoritesManager.removeCustomPlaylistWithSongs(itemToRemove, callback);
      } else {
        favoritesManager.removeFavorite(itemToRemove, callback);
      }

    } catch (Exception e) {
      showAlert(I18N.tr("alert_error_removing_from_favorites"), AlertType.ERROR);
    }
  }

  private void showCreatePlaylistForm() {
    final Form createForm = new Form(I18N.tr("create_playlist"));
    final TextField nameField = new TextField(I18N.tr("playlist_name"), "", 50, TextField.ANY);

    createForm.append(nameField);

    final Command okCommand = new Command(I18N.tr("ok"), Command.OK, 1);
    final Command cancelCommand = new Command(I18N.tr("cancel"), Command.BACK, 2);

    createForm.addCommand(okCommand);
    createForm.addCommand(cancelCommand);

    createForm.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == okCommand) {
              String playlistName = nameField.getString().trim();
              if (playlistName.length() > 0) {
                createCustomPlaylist(playlistName);
              }
            } else if (c == cancelCommand) {
              MIDPlay.getInstance().getDisplay().setCurrent(FavoritesList.this);
            }
          }
        });

    MIDPlay.getInstance().getDisplay().setCurrent(createForm);
  }

  private void createCustomPlaylist(String name) {
    favoritesManager.createCustomPlaylist(
        name,
        new FavoritesCallback() {
          public void onFavoritesLoaded(Vector favorites) {}

          public void onFavoriteAdded() {}

          public void onFavoriteRemoved() {}

          public void onCustomPlaylistRenamed() {}

          public void onCustomPlaylistDeletedWithSongs() {}

          public void onCustomPlaylistSongsLoaded(Vector songs) {}

          public void onCustomPlaylistCreated() {
            if (!isDestroyed) {
              loadFavorites();
              showAlert(I18N.tr("alert_playlist_created"), AlertType.CONFIRMATION);
            }
          }

          public void onError(String message) {
            if (!isDestroyed) {
              showAlert(I18N.tr("alert_error_creating_playlist"), AlertType.ERROR);
            }
          }
        });
  }

  private void showRenamePlaylistForm() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    try {
      final FavoriteItem selectedItem = (FavoriteItem) favorites.elementAt(selectedIndex);
      final JSONObject selected = selectedItem.data;

      boolean isCustomPlaylist = selected.has("isCustom") && selected.getBoolean("isCustom");
      if (!isCustomPlaylist) {
        showAlert(I18N.tr("alert_cannot_rename_system_playlist"), AlertType.WARNING);
        return;
      }

      final Form renameForm = new Form(I18N.tr("rename_playlist"));
      final TextField nameField =
          new TextField(I18N.tr("playlist_name"), selected.getString("name"), 50, TextField.ANY);

      renameForm.append(nameField);

      final Command okCommand = new Command(I18N.tr("ok"), Command.OK, 1);
      final Command cancelCommand = new Command(I18N.tr("cancel"), Command.BACK, 2);

      renameForm.addCommand(okCommand);
      renameForm.addCommand(cancelCommand);

      renameForm.setCommandListener(
          new CommandListener() {
            public void commandAction(Command c, Displayable d) {
              if (c == okCommand) {
                String newName = nameField.getString().trim();
                if (newName.length() > 0) {
                  renameCustomPlaylist(selectedItem, newName);
                }
              } else if (c == cancelCommand) {
                MIDPlay.getInstance().getDisplay().setCurrent(FavoritesList.this);
              }
            }
          });

      MIDPlay.getInstance().getDisplay().setCurrent(renameForm);

    } catch (Exception e) {
      showAlert(I18N.tr("alert_error_renaming_playlist"), AlertType.ERROR);
    }
  }

  private void renameCustomPlaylist(final FavoriteItem item, final String newName) {
    favoritesManager.renameCustomPlaylist(
        item,
        newName,
        new FavoritesCallback() {
          public void onFavoritesLoaded(Vector favorites) {}

          public void onFavoriteAdded() {}

          public void onFavoriteRemoved() {}

          public void onCustomPlaylistCreated() {}

          public void onCustomPlaylistDeletedWithSongs() {}

          public void onCustomPlaylistSongsLoaded(Vector songs) {}

          public void onCustomPlaylistRenamed() {
            if (!isDestroyed) {
              try {
                item.data.put("name", newName);
                int index = favorites.indexOf(item);
                if (index >= 0) {
                  set(index, newName, (Image) images.elementAt(index));
                }
                showAlert(I18N.tr("alert_playlist_renamed"), AlertType.CONFIRMATION);
              } catch (Exception e) {
                showAlert(I18N.tr("alert_error_renaming_playlist"), AlertType.ERROR);
              }
            }
          }

          public void onError(String message) {
            if (!isDestroyed) {
              showAlert(I18N.tr("alert_error_renaming_playlist"), AlertType.ERROR);
            }
          }
        });
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, FavoritesList.this);
  }

  public Image getImage(int index) {
    return (Image)
        (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
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

  public void onImageLoaded(int index, Image image, String requestId) {
    if (isDestroyed || index < 0 || index >= images.size() || index >= size()) {
      return;
    }

    try {
      images.setElementAt(image, index);
      FavoriteItem item = (FavoriteItem) favorites.elementAt(index);
      set(index, item.data.getString("name"), image);
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

  public static class FavoriteItem {
    public int recordId;
    public JSONObject data;

    public FavoriteItem(int recordId, JSONObject data) {
      this.recordId = recordId;
      this.data = data;
    }
  }
}
