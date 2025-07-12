package app.core.service;

import app.core.storage.RecordStoreManager;
import app.ui.FavoritesList.FavoriteItem;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class FavoritesService {

  private static FavoritesService instance;
  private static final String FAVORITES_STORE = "favorites";

  public static synchronized FavoritesService getInstance() {
    if (instance == null) {
      instance = new FavoritesService();
    }
    return instance;
  }

  private FavoritesService() {}

  public Vector getAllFavorites() throws Exception {
    Vector favorites = new Vector();
    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    RecordEnumeration re = null;

    try {
      recordStore.openRecordStore();
      re = recordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecordAsString(recordId);

        if (record.trim().length() == 0) {
          continue;
        }

        JSONObject favoriteJson = new JSONObject(record);
        if (favoriteJson.has("id") && favoriteJson.has("name")) {
          FavoriteItem item = new FavoriteItem(recordId, favoriteJson);
          favorites.addElement(item);
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      recordStore.closeRecordStore();
    }

    return favorites;
  }

  public Vector getCustomPlaylists() throws Exception {
    Vector customPlaylists = new Vector();
    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    RecordEnumeration re = null;

    try {
      recordStore.openRecordStore();
      re = recordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecordAsString(recordId);

        if (record.trim().length() == 0) {
          continue;
        }

        JSONObject favoriteJson = new JSONObject(record);
        if (favoriteJson.has("isCustom") && favoriteJson.getBoolean("isCustom")) {
          FavoriteItem item = new FavoriteItem(recordId, favoriteJson);
          customPlaylists.addElement(item);
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      recordStore.closeRecordStore();
    }

    return customPlaylists;
  }

  public boolean addToFavorites(String playlistId, String playlistName, String imageUrl)
      throws Exception {
    if (isAlreadyInFavorites(playlistId)) {
      return false;
    }

    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    try {
      JSONObject favorite = new JSONObject();
      favorite.put("id", playlistId);
      favorite.put("name", playlistName);
      favorite.put("imageUrl", imageUrl);
      recordStore.addRecord(favorite.toString());
      return true;
    } finally {
      recordStore.closeRecordStore();
    }
  }

  public boolean isAlreadyInFavorites(String playlistId) throws Exception {
    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    RecordEnumeration re = null;

    try {
      recordStore.openRecordStore();
      re = recordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = recordStore.getRecordAsString(recordId);
        JSONObject existingFavorite = new JSONObject(record);
        if (existingFavorite.has("id") && existingFavorite.getString("id").equals(playlistId)) {
          return true;
        }
      }
      return false;
    } finally {
      if (re != null) {
        re.destroy();
      }
      recordStore.closeRecordStore();
    }
  }

  public void removeFavorite(int recordId) throws Exception {
    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    try {
      recordStore.openRecordStore();
      recordStore.deleteRecord(recordId);
    } finally {
      recordStore.closeRecordStore();
    }
  }

  public FavoriteItem getFavoriteById(int recordId) throws Exception {
    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    try {
      recordStore.openRecordStore();
      String record = recordStore.getRecordAsString(recordId);
      JSONObject favoriteJson = new JSONObject(record);
      return new FavoriteItem(recordId, favoriteJson);
    } finally {
      recordStore.closeRecordStore();
    }
  }

  public void createCustomPlaylist(String name) throws Exception {
    String customId = "custom_" + System.currentTimeMillis();

    JSONObject playlistJson = new JSONObject();
    playlistJson.put("id", customId);
    playlistJson.put("name", name);
    playlistJson.put("isCustom", true);

    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    try {
      recordStore.openRecordStore();
      recordStore.addRecord(playlistJson.toString());
    } finally {
      recordStore.closeRecordStore();
    }
  }

  public void renameCustomPlaylist(int recordId, String newName) throws Exception {
    RecordStoreManager recordStore = new RecordStoreManager(FAVORITES_STORE);
    try {
      recordStore.openRecordStore();
      String record = recordStore.getRecordAsString(recordId);
      JSONObject playlistData = new JSONObject(record);

      if (!playlistData.has("isCustom") || !playlistData.getBoolean("isCustom")) {
        throw new IllegalArgumentException("Cannot rename non-custom playlist");
      }

      playlistData.put("name", newName);
      recordStore.setRecord(recordId, playlistData.toString());
    } finally {
      recordStore.closeRecordStore();
    }
  }
}
