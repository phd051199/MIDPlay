package app.network;

import app.common.SettingManager;
import app.constants.Constants;
import app.utils.I18N;
import app.utils.TextUtil;

public class URLProvider {
  public static String getBillboard(int pageIndex, int pageSize) {
    String client = Constants.SERVICE_URL + "/charts?";
    if (client != null) {
      try {
        client = client + "page=" + pageIndex;
        client = client + "&pageSize=" + pageSize;
        client = client + "&lang=" + I18N.getLanguage();
        return client;
      } catch (Exception var6) {
      }
    }

    return null;
  }

  public static String getPlaylist(int pageIndex, int pageSize, String type, String genreKey) {
    String client = Constants.SERVICE_URL + "/playlist?";
    if (client != null) {
      try {
        client = client + "page=" + pageIndex;
        client = client + "&pageSize=" + pageSize;
        client = client + "&type=" + type;
        client = client + "&key=" + genreKey;
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingManager.getInstance().getCurrentService();
        return client;
      } catch (Exception var6) {
      }
    }

    return null;
  }

  public static String getChatEndpoint(String message, String sessionId) {
    String client = Constants.SERVICE_URL + "/chat?";
    if (client != null) {
      try {
        client = client + "lang=" + I18N.getLanguage();
        client = client + "&m=" + TextUtil.urlEncodeUTF8(message);
        client = client + "&sessionId=" + sessionId;
        return client;
      } catch (Exception var5) {
      }
    }

    return null;
  }

  public static String getSongByPlaylist(String listKey, String userName, String type) {
    String client = Constants.SERVICE_URL + "/tracks?";
    if (client != null) {
      try {
        client = client + "listkey=" + listKey;
        client = client + "&quality=" + SettingManager.getInstance().getCurrentAudioQuality();
        client = client + "&type=" + type;
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingManager.getInstance().getCurrentService();
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
        client = client + "lang=" + I18N.getLanguage();
        return client;
      } catch (Exception var5) {
      }
    }

    return null;
  }

  public static String getSearchData(
      String type, String keyword, String genreKey, int pageIndex, int pageSize) {
    String client = Constants.SERVICE_URL + "/search?";
    if (client != null) {
      try {
        client = client + "q=" + TextUtil.urlEncodeUTF8(keyword);
        client = client + "&page=" + pageIndex;
        client = client + "&type=" + type;
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingManager.getInstance().getCurrentService();
        return client;
      } catch (Exception var9) {
      }
    }

    return null;
  }

  public static String getSearchTracks(String keyword) {
    String client = Constants.SERVICE_URL + "/tracks/search?";
    if (client != null) {
      try {
        client = client + "q=" + TextUtil.urlEncodeUTF8(keyword);
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingManager.getInstance().getCurrentService();
        client = client + "&quality=" + SettingManager.getInstance().getCurrentAudioQuality();
        return client;
      } catch (Exception var9) {
      }
    }

    return null;
  }

  private URLProvider() {}
}
