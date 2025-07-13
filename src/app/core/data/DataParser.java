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
    try {
      String url = ApiEndpoints.getChatEndpoint(message, sessionId);
      if (url == null) {
        return I18N.tr("error_connect");
      }
      String response = client.get(url);
      if (response.length() == 0) {
        return I18N.tr("error_connect");
      }
      JSONObject json = new JSONObject(response);
      return json.optString("message", I18N.tr("error_occurred"));
    } catch (Exception e) {
      return I18N.tr("error_connect");
    }
  }

  private static String getCate(int type) {
    try {
      return client.get(ApiEndpoints.getCategory(type));
    } catch (IOException var3) {
      return "";
    }
  }

  public static Vector parseCate(int type) {
    Vector cateItems = new Vector();
    String result = getCate(type);
    if (result != null && !"".equals(result)) {
      try {
        JSONObject json = new JSONObject(result);
        JSONArray jsonArray = json.getJSONArray("Items");
        int totalGroups = jsonArray.length();

        for (int gi = 0; gi < totalGroups; ++gi) {
          JSONObject groupObj = new JSONObject(jsonArray.getString(gi));

          Vector subVec = new Vector();
          if (groupObj.has("SubItems")) {
            JSONArray subs = groupObj.getJSONArray("SubItems");
            int totalSubs = subs.length();
            for (int si = 0; si < totalSubs; ++si) {
              JSONObject subObj = subs.getJSONObject(si);
              Category subCate = new Category();
              subCate.setId(subObj.getString("Key"));
              subCate.setName(subObj.getString("Name"));
              subVec.addElement(subCate);
            }
          }

          if (groupObj.has("Key") && groupObj.has("Name")) {
            Category groupCate = new Category();
            groupCate.setId(groupObj.getString("Key"));
            groupCate.setName(groupObj.getString("Name"));
            groupCate.setSubItems(subVec);
            cateItems.addElement(groupCate);
          } else {
            for (int si = 0; si < subVec.size(); si++) {
              cateItems.addElement(subVec.elementAt(si));
            }
          }
        }
      } catch (JSONException var9) {
        return null;
      }

      return cateItems;
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
    Vector playlistItems = new Vector();

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
        more.setName(I18N.tr("load_more"));
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
    Vector songItems = new Vector();

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
