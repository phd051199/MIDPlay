package app.core.data;

import app.core.storage.RecordStoreManager;
import app.core.threading.ThreadManagerIntegration;
import app.models.Playlist;
import app.models.Song;
import app.ui.FavoritesList;
import app.utils.SongPool;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class FavoritesManager {

  private static volatile FavoritesManager instance;
  private static final Object instanceLock = new Object();

  public static FavoritesManager getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new FavoritesManager();
        }
      }
    }
    return instance;
  }

  private FavoritesManager() {}

  public void loadFavorites(final FavoritesCallback callback) {
    FavoritesDataLoader loader =
        new FavoritesDataLoader(FavoritesDataLoader.OPERATION_LOAD_FAVORITES, null) {
          public Vector load() throws Exception {
            return loadFavoritesFromStorage();
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onFavoritesLoaded(data);
          }

          public void loadError() {
            callback.onError("Error loading favorites");
          }

          public void noData() {
            callback.onFavoritesLoaded(new Vector());
          }
        });
  }

  public void addFavorite(final Playlist playlist, final FavoritesCallback callback) {
    FavoritesDataLoader loader =
        new FavoritesDataLoader(FavoritesDataLoader.OPERATION_ADD_FAVORITE, playlist) {
          public Vector load() throws Exception {
            return addFavoriteToStorage(playlist);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            Boolean success = (Boolean) data.elementAt(0);
            if (success.booleanValue()) {
              callback.onFavoriteAdded();
            } else {
              callback.onError("Playlist already in favorites");
            }
          }

          public void loadError() {
            callback.onError("Error adding to favorites");
          }

          public void noData() {
            callback.onError("Error adding to favorites");
          }
        });
  }

  public void removeFavorite(
      final FavoritesList.FavoriteItem favoriteItem, final FavoritesCallback callback) {
    FavoritesDataLoader loader =
        new FavoritesDataLoader(FavoritesDataLoader.OPERATION_REMOVE_FAVORITE, favoriteItem) {
          public Vector load() throws Exception {
            return removeFavoriteFromStorage(favoriteItem);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onFavoriteRemoved();
          }

          public void loadError() {
            callback.onError("Error removing favorite");
          }

          public void noData() {
            callback.onError("Error removing favorite");
          }
        });
  }

  public void createCustomPlaylist(final String name, final FavoritesCallback callback) {
    FavoritesDataLoader loader =
        new FavoritesDataLoader(FavoritesDataLoader.OPERATION_CREATE_CUSTOM_PLAYLIST, name) {
          public Vector load() throws Exception {
            return createCustomPlaylistInStorage(name);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onCustomPlaylistCreated();
          }

          public void loadError() {
            callback.onError("Error creating playlist");
          }

          public void noData() {
            callback.onError("Error creating playlist");
          }
        });
  }

  public void renameCustomPlaylist(
      final FavoritesList.FavoriteItem item,
      final String newName,
      final FavoritesCallback callback) {
    Object[] params = new Object[] {item, newName};
    FavoritesDataLoader loader =
        new FavoritesDataLoader(FavoritesDataLoader.OPERATION_RENAME_CUSTOM_PLAYLIST, params) {
          public Vector load() throws Exception {
            return renameCustomPlaylistInStorage(item, newName);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onCustomPlaylistRenamed();
          }

          public void loadError() {
            callback.onError("Error renaming playlist");
          }

          public void noData() {
            callback.onError("Error renaming playlist");
          }
        });
  }

  public void loadCustomPlaylistSongs(final String playlistId, final FavoritesCallback callback) {
    FavoritesDataLoader loader =
        new FavoritesDataLoader(
            FavoritesDataLoader.OPERATION_LOAD_CUSTOM_PLAYLIST_SONGS, playlistId) {
          public Vector load() throws Exception {
            return loadCustomPlaylistSongsFromStorage(playlistId);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onCustomPlaylistSongsLoaded(data);
          }

          public void loadError() {
            callback.onError("Error loading playlist songs");
          }

          public void noData() {
            callback.onCustomPlaylistSongsLoaded(new Vector());
          }
        });
  }

  public void removeCustomPlaylistWithSongs(
      final FavoritesList.FavoriteItem favoriteItem, final FavoritesCallback callback) {
    FavoritesDataLoader loader =
        new FavoritesDataLoader(
            FavoritesDataLoader.OPERATION_REMOVE_CUSTOM_PLAYLIST_WITH_SONGS, favoriteItem) {
          public Vector load() throws Exception {
            return removeCustomPlaylistWithSongsFromStorage(favoriteItem);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onCustomPlaylistDeletedWithSongs();
          }

          public void loadError() {
            callback.onError("Error removing custom playlist");
          }

          public void noData() {
            callback.onError("Error removing custom playlist");
          }
        });
  }

  private Vector loadFavoritesFromStorage() throws Exception {
    Vector favorites = new Vector();
    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    RecordEnumeration re = null;

    try {
      re = recordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecordAsString(recordId);

        if (record == null || record.trim().length() == 0) {
          continue;
        }

        try {
          JSONObject favoriteJson = new JSONObject(record);
          if (favoriteJson.has("id") && favoriteJson.has("name")) {
            FavoritesList.FavoriteItem item =
                new FavoritesList.FavoriteItem(recordId, favoriteJson);
            favorites.addElement(item);
          }
        } catch (Exception jsonEx) {
        }
      }
    } catch (Exception e) {
      throw e;
    } finally {
      if (re != null) {
        try {
          re.destroy();
        } catch (Exception e) {
        }
      }
      try {
        recordStore.closeRecordStore();
      } catch (Exception e) {
      }
    }

    return favorites;
  }

  private Vector addFavoriteToStorage(Playlist playlist) throws Exception {
    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    RecordEnumeration re = null;
    boolean alreadyExists = false;
    Vector result = new Vector();

    try {
      re = recordStore.enumerateRecords();
      String newFavoriteId = playlist.getId();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecordAsString(recordId);
        JSONObject existingFavorite = new JSONObject(record);
        if (existingFavorite.has("id") && existingFavorite.getString("id").equals(newFavoriteId)) {
          alreadyExists = true;
          break;
        }
      }

      if (!alreadyExists) {
        JSONObject favorite = new JSONObject();
        favorite.put("id", playlist.getId());
        favorite.put("name", playlist.getName());
        favorite.put("imageUrl", playlist.getImageUrl());
        recordStore.addRecord(favorite.toString());
      }

      result.addElement((!alreadyExists ? Boolean.TRUE : Boolean.FALSE));
    } finally {
      if (re != null) {
        re.destroy();
      }
      recordStore.closeRecordStore();
    }

    return result;
  }

  private Vector removeFavoriteFromStorage(FavoritesList.FavoriteItem favoriteItem)
      throws Exception {
    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    Vector result = new Vector();

    try {
      recordStore.deleteRecord(favoriteItem.recordId);
      result.addElement((true ? Boolean.TRUE : Boolean.FALSE));
    } finally {
      recordStore.closeRecordStore();
    }

    return result;
  }

  private Vector createCustomPlaylistInStorage(String name) throws Exception {
    String customId = "custom_" + System.currentTimeMillis();
    JSONObject playlistJson = new JSONObject();
    playlistJson.put("id", customId);
    playlistJson.put("name", name);
    playlistJson.put("isCustom", true);

    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    Vector result = new Vector();

    try {
      recordStore.addRecord(playlistJson.toString());
      result.addElement((true ? Boolean.TRUE : Boolean.FALSE));
    } finally {
      recordStore.closeRecordStore();
    }

    return result;
  }

  private Vector renameCustomPlaylistInStorage(FavoritesList.FavoriteItem item, String newName)
      throws Exception {
    JSONObject playlistData = item.data;
    playlistData.put("name", newName);

    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    Vector result = new Vector();

    try {
      recordStore.setRecord(item.recordId, playlistData.toString());
      result.addElement((true ? Boolean.TRUE : Boolean.FALSE));
    } finally {
      recordStore.closeRecordStore();
    }

    return result;
  }

  private Vector loadCustomPlaylistSongsFromStorage(String playlistId) throws Exception {
    Vector customSongs = new Vector();
    RecordStoreManager songRecordStore = new RecordStoreManager("playlist_songs");
    RecordEnumeration re = null;

    try {
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        try {
          int recordId = re.nextRecordId();
          String record = songRecordStore.getRecordAsString(recordId);

          if (record != null && record.trim().length() > 0) {
            JSONObject relationJson = new JSONObject(record);

            if (relationJson.has("playlistId")
                && relationJson.getString("playlistId").equals(playlistId)) {
              Song song = SongPool.getInstance().borrowSong();
              song.setSongId(relationJson.getString("songId"));
              song.setSongName(relationJson.getString("name"));
              song.setArtistName(relationJson.getString("artist"));
              song.setImage(relationJson.getString("image"));
              song.setStreamUrl(relationJson.getString("streamUrl"));
              song.setDuration(relationJson.getInt("duration"));
              customSongs.addElement(song);
            }
          }
        } catch (Exception e) {
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      songRecordStore.closeRecordStore();
    }

    return customSongs;
  }

  public Vector getCustomPlaylists() throws Exception {
    Vector customPlaylists = new Vector();
    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    RecordEnumeration re = null;

    try {
      re = recordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecordAsString(recordId);

        if (record == null || record.trim().length() == 0) {
          continue;
        }

        try {
          JSONObject favoriteJson = new JSONObject(record);
          if (favoriteJson.has("isCustom") && favoriteJson.getBoolean("isCustom")) {
            FavoritesList.FavoriteItem item =
                new FavoritesList.FavoriteItem(recordId, favoriteJson);
            customPlaylists.addElement(item);
          }
        } catch (Exception jsonEx) {
        }
      }
    } catch (Exception e) {
      throw e;
    } finally {
      if (re != null) {
        try {
          re.destroy();
        } catch (Exception e) {
        }
      }
      try {
        recordStore.closeRecordStore();
      } catch (Exception e) {
      }
    }

    return customPlaylists;
  }

  public void addSongToCustomPlaylist(
      final Song song,
      final FavoritesList.FavoriteItem playlist,
      final FavoritesCallback callback) {
    Object[] params = new Object[] {song, playlist};
    FavoritesDataLoader loader =
        new FavoritesDataLoader(FavoritesDataLoader.OPERATION_ADD_FAVORITE, params) {
          public Vector load() throws Exception {
            return addSongToCustomPlaylistInStorage(song, playlist);
          }
        };

    ThreadManagerIntegration.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector data) {
            callback.onFavoriteAdded();
          }

          public void loadError() {
            callback.onError("Error adding song to playlist");
          }

          public void noData() {
            callback.onError("Error adding song to playlist");
          }
        });
  }

  private Vector addSongToCustomPlaylistInStorage(Song song, FavoritesList.FavoriteItem playlist)
      throws Exception {
    String playlistId = playlist.data.getString("id");

    String songId = song.getSongId();
    if (songId == null || songId.trim().length() == 0) {
      songId = song.getSongName() + "_" + String.valueOf(System.currentTimeMillis());
      song.setSongId(songId);
    }

    RecordStoreManager songRecordStore = new RecordStoreManager("playlist_songs");
    Vector result = new Vector();

    try {
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

      songRecordStore.addRecord(songJson.toString());
      result.addElement((true ? Boolean.TRUE : Boolean.FALSE));
    } finally {
      songRecordStore.closeRecordStore();
    }

    return result;
  }

  private Vector removeCustomPlaylistWithSongsFromStorage(FavoritesList.FavoriteItem favoriteItem)
      throws Exception {
    String playlistId = favoriteItem.data.getString("id");
    Vector result = new Vector();

    RecordStoreManager songRecordStore = new RecordStoreManager("playlist_songs");
    RecordEnumeration re = null;
    Vector songRecordIds = new Vector();

    try {
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        try {
          int recordId = re.nextRecordId();
          String record = songRecordStore.getRecordAsString(recordId);

          if (record != null && record.trim().length() > 0) {
            JSONObject relationJson = new JSONObject(record);

            if (relationJson.has("playlistId")
                && relationJson.getString("playlistId").equals(playlistId)) {
              songRecordIds.addElement(new Integer(recordId));
            }
          }
        } catch (Exception e) {
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
    }

    for (int i = 0; i < songRecordIds.size(); i++) {
      try {
        int recordId = ((Integer) songRecordIds.elementAt(i)).intValue();
        songRecordStore.deleteRecord(recordId);
      } catch (Exception e) {
      }
    }

    songRecordStore.closeRecordStore();

    RecordStoreManager recordStore = new RecordStoreManager("favorites");
    try {
      recordStore.deleteRecord(favoriteItem.recordId);
      result.addElement((true ? Boolean.TRUE : Boolean.FALSE));
    } finally {
      recordStore.closeRecordStore();
    }

    return result;
  }
}
