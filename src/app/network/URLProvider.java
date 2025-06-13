package app.network;

import app.common.SettingManager;
import app.constants.Constants;
import app.utils.I18N;
import app.utils.TextUtil;

public class URLProvider {

  public static String getTopHotPlaylist(int pageIndex, int pageSize) {
    String client = Constants.SERVICE_URL + "/hot-playlist?";
    if (client != null) {
      try {
        client = client + "page=" + pageIndex;
        return client;
      } catch (Exception var6) {
      }
    }

    return null;
  }

  public static String resolvePlaylist(String playlistKey, int pageIndex) {
    String client = Constants.SERVICE_URL + "/resolve-playlist?";
    if (client != null) {
      try {
        client = client + "key=" + playlistKey;
        client = client + "&page=" + pageIndex;
        return client;
      } catch (Exception var6) {
      }
    }

    return null;
  }

  public static String getSongByPlaylist(String listKey, String userName) {
    String client = Constants.SERVICE_URL + "/tracks?";
    if (client != null) {
      try {
        client = client + "listkey=" + listKey;
        client = client + "&quality=" + SettingManager.getInstance().getCurrentAudioQuality();
        return client;
      } catch (Exception var6) {
      }
    }

    return null;
  }

  public static String getCategory(int type) {
    String client = Constants.SERVICE_URL + "/genre?";
    if (client != null) {
      try {
        client = client + "type=" + type;
        client = client + "&lang=" + I18N.getLanguage();
        return client;
      } catch (Exception var5) {
      }
    }

    return null;
  }

  public static String getSearchData(
      int type, String keyword, String genreKey, int pageIndex, int pageSize) {
    String client = Constants.SERVICE_URL + "/search?";
    if (client != null) {
      try {
        client = client + "q=" + TextUtil.urlEncodeUTF8(keyword);
        client = client + "&page=" + pageIndex;
        client = client + "&type=" + type;
        return client;
      } catch (Exception var9) {
      }
    }

    return null;
  }
}
