package app.core.data;

import app.core.network.ApiEndpoints;
import app.core.network.RestClient;
import app.models.Category;
import app.models.Playlist;
import app.models.Song;
import app.utils.I18N;
import java.io.IOException;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class DataParser {

  private static final RestClient client = RestClient.getInstance();

  public static String sendChatMessage(String message, String sessionId) {
    if (message == null || message.trim().length() == 0) {
      return I18N.tr("error_occurred");
    }

    try {
      String url = ApiEndpoints.getChatEndpoint(message, sessionId);
      if (url == null) {
        return I18N.tr("error_connect");
      }

      String response = client.get(url);
      if (response == null || response.length() == 0) {
        return I18N.tr("error_connect");
      }

      JSONObject json = new JSONObject(response);
      return json.optString("message", I18N.tr("error_occurred"));
    } catch (Exception e) {
      return I18N.tr("error_connect");
    }
  }

  private static String getCategories(int type) {
    try {
      return client.get(ApiEndpoints.getCategory(type));
    } catch (IOException e) {
      return "";
    }
  }

  public static Vector parseCategories(int type) {
    Vector categoryItems = new Vector();
    String result = getCategories(type);
    if (result != null && !"".equals(result)) {
      try {
        JSONObject json = new JSONObject(result);
        JSONArray jsonArray = json.getJSONArray("Items");
        int totalGroups = jsonArray.length();

        for (int i = 0; i < totalGroups; ++i) {
          JSONObject groupObj = new JSONObject(jsonArray.getString(i));

          Vector subCategories = new Vector();
          if (groupObj.has("SubItems")) {
            JSONArray subs = groupObj.getJSONArray("SubItems");
            int totalSubs = subs.length();
            for (int j = 0; j < totalSubs; ++j) {
              JSONObject subObj = subs.getJSONObject(j);
              Category subCategory = new Category();
              subCategory.setId(subObj.getString("Key"));
              subCategory.setName(subObj.getString("Name"));
              subCategories.addElement(subCategory);
            }
          }

          if (groupObj.has("Key") && groupObj.has("Name")) {
            Category groupCategory = new Category();
            groupCategory.setId(groupObj.getString("Key"));
            groupCategory.setName(groupObj.getString("Name"));
            groupCategory.setSubItems(subCategories);
            categoryItems.addElement(groupCategory);
          } else {
            for (int j = 0; j < subCategories.size(); j++) {
              categoryItems.addElement(subCategories.elementAt(j));
            }
          }
        }
      } catch (JSONException e) {
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
    } catch (IOException e) {
      return "";
    }
  }

  private static Vector parsePlaylists(String key, String jsonResult) {
    Vector playlistItems = new Vector();

    try {
      JSONObject json = new JSONObject(jsonResult);
      JSONArray jsonArray = json.getJSONArray("Items");
      int total = jsonArray.length();

      for (int i = 0; i < total; ++i) {
        String playlistJSON = jsonArray.getString(i);
        Playlist playlist = new Playlist();
        playlist.fromJSON(playlistJSON);
        playlistItems.addElement(playlist);
      }

      if ("yes".equals(json.getString("GetMore"))) {
        Playlist more = new Playlist();
        more.setName(I18N.tr("load_more"));
        more.setId(key);
        playlistItems.addElement(more);
      }

      return playlistItems;
    } catch (JSONException e) {
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
    } catch (IOException e) {
      return "";
    }
  }

  public static Vector parseSearchTracks(String keyword) {
    String result = getSearchTracks(keyword);
    if (result != null && !"".equals(result)) {
      Vector items = parsePlaylistSongs(result);
      return items;
    } else {
      return null;
    }
  }

  private static String getPlaylist(int currentPage, int pageSize, String type, String genreKey) {
    try {
      return client.get(ApiEndpoints.getPlaylist(currentPage, pageSize, type, genreKey));
    } catch (IOException e) {
      return "";
    }
  }

  public static Vector parsePlaylist(int currentPage, int pageSize, String type, String genreKey) {
    String result = getPlaylist(currentPage, pageSize, type, genreKey);
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
    } catch (IOException e) {
      return "";
    }
  }

  public static Vector parseSongsInPlaylist(
      String listkey, String username, int curPare, int pageSize, String type) {
    String result = getSongsInPlaylist(listkey, username, curPare, pageSize, type);
    if (result != null && !"".equals(result)) {
      Vector songItems = parsePlaylistSongs(result);
      return songItems;
    } else {
      return null;
    }
  }

  private static Vector parsePlaylistSongs(String jsonResult) {
    Vector songItems = new Vector();

    if (jsonResult == null || jsonResult.trim().length() == 0) {
      return songItems;
    }

    try {
      JSONObject json = new JSONObject(jsonResult);

      if (!json.has("Items")) {
        return songItems;
      }

      JSONArray jsonArray = json.getJSONArray("Items");
      int total = jsonArray.length();

      if (total > 2000) {
        total = 2000;
      }

      for (int i = 0; i < total; ++i) {
        try {
          String songJSON = jsonArray.getString(i);
          if (songJSON != null && songJSON.trim().length() > 0) {
            Song song = new Song();
            song.fromJSON(songJSON);
            songItems.addElement(song);
          }
        } catch (Exception songEx) {
        }
      }

      return songItems;
    } catch (Exception e) {
      return songItems;
    }
  }

  public static String getBillboard(int curPage, int pageSize) {
    try {
      return client.get(ApiEndpoints.getBillboard(curPage, pageSize));
    } catch (IOException e) {
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
    } catch (IOException e) {
      return "";
    }
  }

  private DataParser() {}
}
