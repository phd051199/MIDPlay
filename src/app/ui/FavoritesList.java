package app.ui;

import app.MIDPlay;
import app.core.data.AsyncDataManager;
import app.core.data.DataLoader;
import app.core.data.DataParser;
import app.core.data.LoadDataListener;
import app.core.data.LoadDataObserver;
import app.core.service.FavoritesService;
import app.core.service.PlaylistSongService;
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
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import org.json.me.JSONObject;

public class FavoritesList extends List
    implements CommandListener, LoadDataObserver, ImageLoadCallback {
  private static final int NAVIGATION_DELAY_MS = 300;
  private static final int SELECTION_MONITOR_INTERVAL_MS = 200;
  private static final int SELECTION_MONITOR_DELAY_MS = 100;

  private Command backCommand;
  private Command removeCommand;
  private Command createPlaylistCommand;
  private Command renamePlaylistCommand;
  private Vector favorites;
  private final MainObserver observer;
  private Vector images;
  private ImageLoader imageLoader;
  private Thread loadSongListThread;
  private final String type = "playlist";
  private Image defaultImage;
  private boolean isDestroyed = false;
  private boolean isNavigating = false;
  private Timer navigationTimer;
  private TimerTask navigationTask;
  private int lastSelectedIndex = -1;
  private Timer selectionMonitorTimer;
  private TimerTask selectionMonitorTask;

  public FavoritesList(MainObserver observer) {
    super(LocalizationManager.tr("favorites"), List.IMPLICIT);
    this.observer = observer;
    this.favorites = new Vector();
    this.images = new Vector();
    this.imageLoader = ImageLoader.getInstance();
    this.loadDefaultImage();
    initCommands();
    initComponents();
  }

  private void initCommands() {
    this.backCommand = new Command(LocalizationManager.tr("back"), Command.BACK, 0);
    this.removeCommand =
        new Command(LocalizationManager.tr("remove_from_favorites"), Command.ITEM, 2);
    this.createPlaylistCommand =
        new Command(LocalizationManager.tr("create_playlist"), Command.ITEM, 3);
    this.renamePlaylistCommand =
        new Command(LocalizationManager.tr("rename_playlist"), Command.ITEM, 4);

    this.addCommand(backCommand);
    this.addCommand(removeCommand);
    this.addCommand(createPlaylistCommand);
    this.addCommand(renamePlaylistCommand);
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

    try {
      Vector allFavorites = FavoritesService.getInstance().getAllFavorites();

      for (int i = 0; i < allFavorites.size(); i++) {
        FavoriteItem item = (FavoriteItem) allFavorites.elementAt(i);
        favorites.addElement(item);
        this.append(item.data.getString("name"), defaultImage);
        this.images.addElement(defaultImage);
      }
    } catch (Exception e) {
    }

    if (favorites.size() > 0) {
      loadFavoriteImages();
    }
  }

  private void loadFavoriteImages() {
    if (isDestroyed || isNavigating) {
      return;
    }

    if (!SettingsManager.getInstance().isLoadPlaylistArtEnabled()) {
      return;
    }

    loadImagesWithImageLoader(0);
  }

  public void resumeImageLoading() {
    if (isDestroyed || isNavigating) {
      return;
    }

    if (!SettingsManager.getInstance().isLoadPlaylistArtEnabled()) {
      return;
    }

    final int startIndex = findFirstDefaultImageIndex();
    if (startIndex >= 0) {
      loadImagesWithImageLoader(startIndex);
    }
  }

  public void pauseImageLoading() {
    if (this.imageLoader != null) {
      this.imageLoader.cancelRequestsForCallback(this);
    }
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
      isNavigating = false;
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

  private void loadImagesWithImageLoader(int startIndex) {
    if (isDestroyed || isNavigating) {
      return;
    }

    int totalSize = favorites.size();

    for (int i = startIndex; i < totalSize; i++) {
      if (isDestroyed || isNavigating) {
        break;
      }

      try {
        Image currentImage = (Image) images.elementAt(i);
        if (currentImage == defaultImage || currentImage == null) {
          FavoriteItem item = (FavoriteItem) this.favorites.elementAt(i);
          JSONObject favorite = item.data;
          if (favorite.has("imageUrl")) {
            String imageUrl = favorite.getString("imageUrl");
            if (imageUrl != null && imageUrl.length() > 0) {
              if (!isDestroyed && !isNavigating) {
                ImageLoadRequest request = new ImageLoadRequest(imageUrl, 48, i, this);
                imageLoader.loadImage(request);
              }
            }
          }
        }
      } catch (Exception e) {
      }
    }
  }

  private void updateSingleImage(final int index, final Image img, final FavoriteItem item) {
    ThreadManager.runOnUiThread(
        new Runnable() {
          public void run() {
            if (isDestroyed) {
              return;
            }
            try {
              int selectedIndex = getSelectedIndex();

              if (index < images.size() && index < favorites.size() && index < size()) {
                images.setElementAt(img, index);
                set(index, item.data.getString("name"), img);

                if (selectedIndex >= 0 && selectedIndex < size()) {
                  setSelectedIndex(selectedIndex, false);
                }
              }
            } catch (Exception e) {
            }
          }
        });
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
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      showCreatePlaylistForm();
    } else if (c == renamePlaylistCommand) {
      this.stopSelectionMonitoring();
      this.pauseImageLoading();
      showRenamePlaylistForm();
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
          this.displayMessage(
              LocalizationManager.tr("error"),
              LocalizationManager.tr("invalid_playlist_data"),
              "error");
          return;
        }

        final Playlist playlist = new Playlist();
        playlist.setId(selected.getString("id"));
        playlist.setName(selected.getString("name"));

        final boolean isCustomPlaylist =
            selected.has("isCustom") && selected.getBoolean("isCustom");

        if (isCustomPlaylist) {
          this.displayMessage(playlist.getName(), LocalizationManager.tr("loading"), "loading");

          loadSongListThread =
              ThreadManager.createThread(
                  new Runnable() {
                    public void run() {
                      try {
                        final Vector customSongs =
                            loadSongsFromCustomPlaylist(selected.getString("id"));

                        ThreadManager.runOnUiThread(
                            new Runnable() {
                              public void run() {
                                SongList songList =
                                    new SongList(playlist.getName(), customSongs, playlist);
                                songList.setObserver(FavoritesList.this.observer);
                                FavoritesList.this.observer.replaceCurrent(songList);
                              }
                            });
                      } catch (Exception e) {
                        displayMessage(
                            LocalizationManager.tr("error"),
                            LocalizationManager.tr("error_loading_playlist"),
                            "error");
                      }
                    }
                  },
                  "LoadCustomPlaylistSongs");
          ThreadManager.safeStartThread(loadSongListThread);

          return;
        }

        this.displayMessage(playlist.getName(), LocalizationManager.tr("loading"), "loading");

        AsyncDataManager.getInstance()
            .loadDataAsync(
                new DataLoader() {
                  public Vector load() throws Exception {
                    return DataParser.parseSongsInPlaylist(
                        playlist.getId(), "", 1, 30, FavoritesList.this.type);
                  }
                },
                new LoadDataListener() {
                  public void loadDataCompleted(final Vector data) {
                    ThreadManager.runOnUiThread(
                        new Runnable() {
                          public void run() {
                            SongList songList = new SongList(playlist.getName(), data, playlist);
                            songList.setObserver(FavoritesList.this.observer);
                            FavoritesList.this.observer.replaceCurrent(songList);
                          }
                        });
                  }

                  public void loadError() {
                    ThreadManager.runOnUiThread(
                        new Runnable() {
                          public void run() {
                            displayMessage(
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
                            displayMessage(
                                playlist.getName(), LocalizationManager.tr("no_data"), "error");
                          }
                        });
                  }
                });

      } catch (Exception e) {
        this.displayMessage(
            LocalizationManager.tr("error"),
            LocalizationManager.tr("error_loading_playlist"),
            "error");
      }
    }
  }

  private Vector loadSongsFromCustomPlaylist(String playlistId) {
    try {
      return PlaylistSongService.getInstance().loadSongsFromCustomPlaylist(playlistId);
    } catch (Exception e) {
      return new Vector();
    }
  }

  private void removeSelectedFavorite() {
    int selectedIndex = this.getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    try {
      FavoriteItem itemToRemove = (FavoriteItem) favorites.elementAt(selectedIndex);

      FavoritesService.getInstance().removeFavorite(itemToRemove.recordId);

      favorites.removeElementAt(selectedIndex);
      images.removeElementAt(selectedIndex);

      this.delete(selectedIndex);

      showAlert(LocalizationManager.tr("alert_removed_from_favorites"), AlertType.CONFIRMATION);

    } catch (Exception e) {
      showAlert(LocalizationManager.tr("alert_error_removing_from_favorites"), AlertType.ERROR);
    }
  }

  private void showCreatePlaylistForm() {
    final Form createForm = new Form(LocalizationManager.tr("create_playlist"));
    final TextField nameField =
        new TextField(LocalizationManager.tr("playlist_name"), "", 50, TextField.ANY);

    createForm.append(nameField);

    final Command okCommand = new Command(LocalizationManager.tr("ok"), Command.OK, 1);
    final Command cancelCommand = new Command(LocalizationManager.tr("cancel"), Command.BACK, 2);

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
    try {
      FavoritesService.getInstance().createCustomPlaylist(name);
      loadFavorites();
      showAlert(LocalizationManager.tr("alert_playlist_created"), AlertType.CONFIRMATION);
    } catch (Exception e) {
      showAlert(LocalizationManager.tr("alert_error_creating_playlist"), AlertType.ERROR);
    }
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
        showAlert(LocalizationManager.tr("alert_cannot_rename_system_playlist"), AlertType.WARNING);
        return;
      }

      final Form renameForm = new Form(LocalizationManager.tr("rename_playlist"));
      final TextField nameField =
          new TextField(
              LocalizationManager.tr("playlist_name"),
              selected.getString("name"),
              50,
              TextField.ANY);

      renameForm.append(nameField);

      final Command okCommand = new Command(LocalizationManager.tr("ok"), Command.OK, 1);
      final Command cancelCommand = new Command(LocalizationManager.tr("cancel"), Command.BACK, 2);

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
      showAlert(LocalizationManager.tr("alert_error_renaming_playlist"), AlertType.ERROR);
    }
  }

  private void renameCustomPlaylist(FavoriteItem item, String newName) {
    try {
      FavoritesService.getInstance().renameCustomPlaylist(item.recordId, newName);

      item.data.put("name", newName);
      int index = favorites.indexOf(item);
      if (index >= 0) {
        set(index, newName, (Image) images.elementAt(index));
      }

      showAlert(LocalizationManager.tr("alert_playlist_renamed"), AlertType.CONFIRMATION);
    } catch (Exception e) {
      showAlert(LocalizationManager.tr("alert_error_renaming_playlist"), AlertType.ERROR);
    }
  }

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    if (type == AlertType.ERROR) {
      alert.setTimeout(Alert.FOREVER);
    } else {
      alert.setTimeout(2000);
    }
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

    if (favorites != null) {
      favorites.removeAllElements();
      favorites = null;
    }

    defaultImage = null;
    imageLoader = null;
  }

  public void onImageLoaded(int index, Image image, String requestId) {
    if (isDestroyed || isNavigating) {
      return;
    }
    try {
      FavoriteItem item = (FavoriteItem) this.favorites.elementAt(index);
      updateSingleImage(index, image, item);
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
    return !isDestroyed && !isNavigating;
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
