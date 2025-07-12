package app.core.data;

import app.core.network.ApiEndpoints;
import app.core.network.RestClient;
import app.models.Category;
import app.models.Playlist;
import app.models.Song;
import app.utils.text.LocalizationManager;
import java.io.IOException;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class DataParser {

  private static final RestClient client = RestClient.getInstance();

  public static String sendChatMessage(String message, String sessionId) {
    try {
      String url = ApiEndpoints.getChatEndpoint(message, sessionId);
      if (url == null) {
        return LocalizationManager.tr("error_connect");
      }
      String response = client.get(url);
      if (response.length() == 0) {
        return LocalizationManager.tr("error_connect");
      }
      JSONObject json = new JSONObject(response);
      return json.optString("message", LocalizationManager.tr("error_occurred"));
    } catch (Exception e) {
      return LocalizationManager.tr("error_connect");
    }
  }

  private static String getCategory(int type) {
    try {
      return client.get(ApiEndpoints.getCategory(type));
    } catch (IOException exception) {
      return "";
    }
  }

  public static Vector parseCategories(int type) {
    Vector categoryItems = new Vector(20);
    String result = getCategory(type);
    if (result != null && !"".equals(result)) {
      try {
        JSONObject json = new JSONObject(result);
        JSONArray jsonArray = json.getJSONArray("Items");
        int totalGroups = jsonArray.length();

        for (int groupIndex = 0; groupIndex < totalGroups; ++groupIndex) {
          JSONObject groupObject = new JSONObject(jsonArray.getString(groupIndex));

          Vector subCategories = new Vector(10);
          if (groupObject.has("SubItems")) {
            JSONArray subItems = groupObject.getJSONArray("SubItems");
            int totalSubItems = subItems.length();
            for (int subIndex = 0; subIndex < totalSubItems; ++subIndex) {
              JSONObject subObject = subItems.getJSONObject(subIndex);
              Category subCategory = new Category();
              subCategory.setId(subObject.getString("Key"));
              subCategory.setName(subObject.getString("Name"));
              subCategories.addElement(subCategory);
            }
          }

          if (groupObject.has("Key") && groupObject.has("Name")) {
            Category groupCategory = new Category();
            groupCategory.setId(groupObject.getString("Key"));
            groupCategory.setName(groupObject.getString("Name"));
            groupCategory.setSubItems(subCategories);
            categoryItems.addElement(groupCategory);
          } else {
            for (int subIndex = 0; subIndex < subCategories.size(); subIndex++) {
              categoryItems.addElement(subCategories.elementAt(subIndex));
            }
          }
        }
      } catch (JSONException exception) {
        return null;
      }

      return categoryItems;
    } else {
      return null;
    }
  }

  private static String getSearchPlaylists(
      String key, String keyword, int curpage, int pagesize, String type) {
    try {
      String url = ApiEndpoints.getSearchData(type, keyword, key, curpage, pagesize);
      return client.get(url);
    } catch (IOException var6) {
      return "";
    }
  }

  private static Vector parsePlaylists(String key, String jsonResult) {
    Vector playlistItems = new Vector(50);

    try {
      JSONObject json = new JSONObject(jsonResult);
      JSONArray jsonArray = json.getJSONArray("Items");
      int total = jsonArray.length();

      for (int i = 0; i < total; ++i) {
        String threadsJSON = jsonArray.getString(i);
        Playlist playlist = new Playlist();
        playlist.fromJSON(threadsJSON);
        playlistItems.addElement(playlist);
      }

      if ("yes".equals(json.getString("GetMore"))) {
        Playlist more = new Playlist();
        more.setName(LocalizationManager.tr("load_more"));
        more.setId(key);
        playlistItems.addElement(more);
      }

      return playlistItems;
    } catch (JSONException var9) {
      return null;
    }
  }

  public static Vector parseSearch(
      String genrekey, String keyword, int curpage, int pagesize, String type) {
    String result = getSearchPlaylists(genrekey, keyword, curpage, pagesize, type);
    if (result != null && !"".equals(result)) {
      Vector playlistItems = parsePlaylists(genrekey, result);
      return playlistItems;
    } else {
      return null;
    }
  }

  private static String getSearchTracks(String keyword) {
    try {
      String url = ApiEndpoints.getSearchTracks(keyword);
      return client.get(url);
    } catch (IOException var6) {
      return "";
    }
  }

  public static Vector parseSearchTracks(String keyword) {
    String result = getSearchTracks(keyword);
    if (result != null && !"".equals(result)) {
      Vector items = parseSongOfPlaylist(result);
      return items;
    } else {
      return null;
    }
  }

  private static String getPlaylist(int curPare, int pageSize, String type, String genreKey) {
    try {
      return client.get(ApiEndpoints.getPlaylist(curPare, pageSize, type, genreKey));
    } catch (IOException var4) {
      return "";
    }
  }

  public static Vector parsePlaylist(int curpage, int pagesize, String type, String genreKey) {
    String result = getPlaylist(curpage, pagesize, type, genreKey);
    if (result != null && !"".equals(result)) {
      Vector playlistItems = parsePlaylists("tophot", result);
      return playlistItems;
    } else {
      return null;
    }
  }

  public static String getSongsInPlaylist(
      String listkey, String username, int curPare, int pageSize, String type) {
    try {
      return client.get(ApiEndpoints.getSongByPlaylist(listkey, username, type));
    } catch (IOException var6) {
      return "";
    }
  }

  public static Vector parseSongsInPlaylist(
      String listkey, String username, int curPare, int pageSize, String type) {
    String result = getSongsInPlaylist(listkey, username, curPare, pageSize, type);
    if (result != null && !"".equals(result)) {
      Vector songItems = parseSongOfPlaylist(result);
      return songItems;
    } else {
      return null;
    }
  }

  private static Vector parseSongOfPlaylist(String jsonResult) {
    Vector songItems = new Vector(30);

    try {
      JSONObject json = new JSONObject(jsonResult);
      JSONArray jsonArray = json.getJSONArray("Items");
      int total = jsonArray.length();

      for (int i = 0; i < total; ++i) {
        String threadsJSON = jsonArray.getString(i);
        Song song = new Song();
        song.fromJSON(threadsJSON);
        songItems.addElement(song);
      }

      return songItems;
    } catch (JSONException var9) {
      return null;
    }
  }

  public static String getBillboard(int curPage, int pageSize) {
    try {
      return client.get(ApiEndpoints.getBillboard(curPage, pageSize));
    } catch (IOException var6) {
      return "";
    }
  }

  public static Vector parseBillboard(int curPage, int pageSize) {
    String result = getBillboard(curPage, pageSize);
    if (result != null && !"".equals(result)) {
      Vector playlistItems = parsePlaylists("billboard", result);
      return playlistItems;
    } else {
      return null;
    }
  }

  public static String checkForUpdate() {
    try {
      return client.get(ApiEndpoints.checkForUpdate());
    } catch (IOException var6) {
      return "";
    }
  }

  private DataParser() {}
}
