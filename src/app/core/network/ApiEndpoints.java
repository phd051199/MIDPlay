package app.core.network;

import app.MIDPlay;
import app.constants.AppConstants;
import app.core.settings.SettingsManager;
import app.utils.I18N;
import app.utils.TextUtils;

public class ApiEndpoints {

  public static String checkForUpdate() {
    try {
      String baseUrl = AppConstants.SERVICE_URL + "/update?";
      if (baseUrl != null) {
        String version = MIDPlay.getAppVersion();
        if (version != null && version.length() > 0) {
          return baseUrl + "version=" + version;
        } else {
          return baseUrl + "version=unknown";
        }
      }
    } catch (Exception e) {
    }

    return null;
  }

  public static String getBillboard(int pageIndex, int pageSize) {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Page index cannot be negative: " + pageIndex);
    }

    if (pageSize <= 0 || pageSize > 500) {
      throw new IllegalArgumentException("Page size must be between 1 and 500: " + pageSize);
    }

    try {
      String baseUrl = AppConstants.SERVICE_URL + "/charts?";
      if (baseUrl != null) {
        String url = baseUrl + "page=" + pageIndex;
        url = url + "&pageSize=" + pageSize;

        String language = I18N.getLanguage();
        if (language != null && language.length() > 0) {
          url = url + "&lang=" + language;
        }

        return url;
      }
    } catch (Exception e) {
    }

    return null;
  }

  public static String getPlaylist(int pageIndex, int pageSize, String type, String genreKey) {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Page index cannot be negative: " + pageIndex);
    }

    if (pageSize <= 0 || pageSize > 500) {
      throw new IllegalArgumentException("Page size must be between 1 and 500: " + pageSize);
    }

    try {
      String baseUrl = AppConstants.SERVICE_URL + "/playlist?";
      if (baseUrl != null) {
        String url = baseUrl + "page=" + pageIndex;
        url = url + "&pageSize=" + pageSize;

        if (type != null && type.length() > 0) {
          url = url + "&type=" + type;
        }

        if (genreKey != null && genreKey.length() > 0) {
          url = url + "&key=" + genreKey;
        }

        String language = I18N.getLanguage();
        if (language != null && language.length() > 0) {
          url = url + "&lang=" + language;
        }

        String service = SettingsManager.getInstance().getCurrentService();
        if (service != null && service.length() > 0) {
          url = url + "&service=" + service;
        }

        return url;
      }
    } catch (Exception e) {
    }

    return null;
  }

  public static String getChatEndpoint(String message, String sessionId) {
    String client = AppConstants.SERVICE_URL + "/chat?";
    if (client != null) {
      try {
        client = client + "lang=" + I18N.getLanguage();
        client = client + "&m=" + TextUtils.urlEncodeUTF8(message);
        client = client + "&sessionId=" + sessionId;
        return client;
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getSongByPlaylist(String listKey, String userName, String type) {
    String client = AppConstants.SERVICE_URL + "/tracks?";
    if (client != null) {
      try {
        client = client + "listkey=" + listKey;
        client = client + "&quality=" + SettingsManager.getInstance().getCurrentAudioQuality();
        client = client + "&type=" + type;
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingsManager.getInstance().getCurrentService();
        return client;
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getCategory(int type) {
    String client = AppConstants.SERVICE_URL + "/genre?";
    if (client != null) {
      try {
        client = client + "lang=" + I18N.getLanguage();
        return client;
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getSearchData(
      String type, String keyword, String genreKey, int pageIndex, int pageSize) {
    String client = AppConstants.SERVICE_URL + "/search?";
    if (client != null) {
      try {
        client = client + "q=" + TextUtils.urlEncodeUTF8(keyword);
        client = client + "&page=" + pageIndex;
        client = client + "&type=" + type;
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingsManager.getInstance().getCurrentService();
        return client;
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getSearchTracks(String keyword) {
    String client = AppConstants.SERVICE_URL + "/tracks/search?";
    if (client != null) {
      try {
        client = client + "q=" + TextUtils.urlEncodeUTF8(keyword);
        client = client + "&lang=" + I18N.getLanguage();
        client = client + "&service=" + SettingsManager.getInstance().getCurrentService();
        client = client + "&quality=" + SettingsManager.getInstance().getCurrentAudioQuality();
        return client;
      } catch (Exception e) {
      }
    }

    return null;
  }

  private ApiEndpoints() {}
}
