package musicapp.common;

import java.io.IOException;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import musicapp.model.Category;
import musicapp.model.Playlist;
import musicapp.model.Song;
import musicapp.network.URLProvider;

public class ParseData {

    public static String getDeviceInfor() {
        RestClient client = new RestClient();
        if (client != null) {
            try {
                return client.get(URLProvider.newVersion());
            } catch (Exception var2) {
                System.out.println(var2.getMessage());
            }
        }

        return "";
    }

    private static String getCate(int type) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.getCategory(type));
        } catch (IOException var3) {
            System.out.println(var3.getMessage());
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
                int total = jsonArray.length();

                for (int i = 0; i < total; ++i) {
                    String threadsJSON = jsonArray.getString(i);
                    Category cate = new Category();
                    cate.fromJSON(threadsJSON);
                    cateItems.addElement(cate);
                }
            } catch (JSONException var9) {
                System.out.println(var9.getMessage());
            }

            return cateItems;
        } else {
            return null;
        }
    }

    private static String getSearchPlaylists(String key, String keyword, int curpage, int pagesize) {
        RestClient client = new RestClient();

        try {
            String url = URLProvider.getSearchData(2, keyword, key, curpage, pagesize);
            return client.get(url);
        } catch (IOException var6) {
            System.out.println(var6.getMessage());
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
                more.setName("xem thêm ...");
                more.setId(key);
                playlistItems.addElement(more);
            }

            return playlistItems;
        } catch (JSONException var9) {
            System.out.println(var9.getMessage());
            return null;
        }
    }

    public static Vector parseSearch(String genrekey, String keyword, int curpage, int pagesize) {
        String result = getSearchPlaylists(genrekey, keyword, curpage, pagesize);
        if (result != null && !"".equals(result)) {
            Vector playlistItems = parsePlaylists(genrekey, result);
            return playlistItems;
        } else {
            return null;
        }
    }

    private static String getTopHotPlaylist(int curPare, int pageSize) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.getTopHotPlaylist(curPare, pageSize));
        } catch (IOException var4) {
            System.out.println(var4.getMessage());
            return "";
        }
    }

    public static Vector parseHotPlaylist(int curpage, int pagesize) {
        String result = getTopHotPlaylist(curpage, pagesize);
        if (result != null && !"".equals(result)) {
            Vector playlistItems = parsePlaylists("tophot", result);
            return playlistItems;
        } else {
            return null;
        }
    }

    private static String getTopNewPlaylist(int curPare, int pageSize) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.getTopNewsPlaylist(curPare, pageSize));
        } catch (IOException var4) {
            var4.printStackTrace();
            return "";
        }
    }

    public static Vector parseNewPlaylist(int curpage, int pagesize) {
        new Vector();
        String result = getTopNewPlaylist(curpage, pagesize);
        if (result != null && !"".equals(result)) {
            Vector playlistItems = parsePlaylists("topnew", result);
            return playlistItems;
        } else {
            return null;
        }
    }

    public static String getSongsInPlaylist(String listkey, String username, int curPare, int pageSize) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.getSongByPlaylist(listkey, username));
        } catch (IOException var6) {
            System.out.println(var6.getMessage());
            return "";
        }
    }

    public static Vector parseSongsInPlaylist(String listkey, String username, int curPare, int pageSize) {
        String result = getSongsInPlaylist(listkey, username, curPare, pageSize);
        if (result != null && !"".equals(result)) {
            Vector songItems = parseSongOfPlaylist(listkey, result);
            return songItems;
        } else {
            return null;
        }
    }

    private static Vector parseSongOfPlaylist(String key, String jsonResult) {
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
            System.out.println(var9.getMessage());
            return null;
        }
    }

    private static String getLoginResult(String username, String pass) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.login(username, pass));
        } catch (IOException var4) {
            var4.printStackTrace();
            return "";
        }
    }

    public static String parseLoginResult(String username, String pass) {
        String result = getLoginResult(username, pass);
        if (result != null && !"".equals(result)) {
            try {
                return result;
            } catch (Exception var4) {
                var4.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    private static String getUserPlaylistsByAction(String action, String username, int curPare, int pageSize) {
        RestClient client = new RestClient();

        try {
            if (action == "myPlaylist") {
                return client.get(URLProvider.getMyPlaylist(username, curPare, pageSize));
            }

            if (action == "playlistLiked") {
                return client.get(URLProvider.getPlaylistLiked(username, curPare, pageSize));
            }
        } catch (IOException var6) {
            var6.printStackTrace();
        }

        return "";
    }

    public static Vector parseUserPlaylistsByAction(String action, String username, int curPare, int pageSize) {
        new Vector();
        String result = getUserPlaylistsByAction(action, username, curPare, pageSize);
        if (result != null && !"".equals(result)) {
            Vector playlistItems = parsePlaylists(action, result);
            return playlistItems;
        } else {
            return null;
        }
    }

    public static String getDefaultPlaylistInfor(String username, int curPare, int pageSize) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.getMyPlaylist(username, curPare, pageSize));
        } catch (IOException var5) {
            var5.printStackTrace();
            return "";
        }
    }

    public static Vector parseDefaultPlaylistInfor(String username, int curPare, int pageSize) {
        new Vector();
        String result = getDefaultPlaylistInfor(username, curPare, pageSize);
        if (result != null && !"".equals(result)) {
            Vector songItems = parseSongOfPlaylist("", result);
            return songItems;
        } else {
            return null;
        }
    }

    public static Playlist getDefaultPlaylist(String username, int curPare, int pageSize) {
        String result = getDefaultPlaylistInfor(username, curPare, pageSize);
        if (result != null && !"".equals(result)) {
            try {
                Playlist playlist = new Playlist();
                playlist.fromJSON(result);
                return playlist;
            } catch (Exception var5) {
                var5.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

}
