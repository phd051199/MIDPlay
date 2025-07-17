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
        StringBuffer urlBuffer = new StringBuffer(baseUrl);
        urlBuffer.append("page=").append(pageIndex);
        urlBuffer.append("&pageSize=").append(pageSize);

        if (type != null && type.length() > 0) {
          urlBuffer.append("&type=").append(type);
        }

        if (genreKey != null && genreKey.length() > 0) {
          urlBuffer.append("&key=").append(genreKey);
        }

        String language = I18N.getLanguage();
        if (language != null && language.length() > 0) {
          urlBuffer.append("&lang=").append(language);
        }

        String service = SettingsManager.getInstance().getCurrentService();
        if (service != null && service.length() > 0) {
          urlBuffer.append("&service=").append(service);
        }

        return urlBuffer.toString();
      }
    } catch (Exception e) {
    }

    return null;
  }

  public static String getChatEndpoint(String message, String sessionId) {
    String baseUrl = AppConstants.SERVICE_URL + "/chat?";
    if (baseUrl != null) {
      try {
        StringBuffer urlBuffer = new StringBuffer(baseUrl);
        urlBuffer.append("lang=").append(I18N.getLanguage());
        urlBuffer.append("&m=").append(TextUtils.urlEncodeUTF8(message));
        urlBuffer.append("&sessionId=").append(sessionId);
        return urlBuffer.toString();
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getSongByPlaylist(String listKey, String userName, String type) {
    String baseUrl = AppConstants.SERVICE_URL + "/tracks?";
    if (baseUrl != null) {
      try {
        StringBuffer urlBuffer = new StringBuffer(baseUrl);
        urlBuffer.append("listkey=").append(listKey);
        urlBuffer
            .append("&quality=")
            .append(SettingsManager.getInstance().getCurrentAudioQuality());
        urlBuffer.append("&type=").append(type);
        urlBuffer.append("&lang=").append(I18N.getLanguage());
        urlBuffer.append("&service=").append(SettingsManager.getInstance().getCurrentService());
        return urlBuffer.toString();
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getCategory(int type) {
    String baseUrl = AppConstants.SERVICE_URL + "/genre?";
    if (baseUrl != null) {
      try {
        StringBuffer urlBuffer = new StringBuffer(baseUrl);
        urlBuffer.append("lang=").append(I18N.getLanguage());
        return urlBuffer.toString();
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getSearchData(
      String type, String keyword, String genreKey, int pageIndex, int pageSize) {
    String baseUrl = AppConstants.SERVICE_URL + "/search?";
    if (baseUrl != null) {
      try {
        StringBuffer urlBuffer = new StringBuffer(baseUrl);
        urlBuffer.append("q=").append(TextUtils.urlEncodeUTF8(keyword));
        urlBuffer.append("&page=").append(pageIndex);
        urlBuffer.append("&type=").append(type);
        urlBuffer.append("&lang=").append(I18N.getLanguage());
        urlBuffer.append("&service=").append(SettingsManager.getInstance().getCurrentService());
        return urlBuffer.toString();
      } catch (Exception e) {
      }
    }

    return null;
  }

  public static String getSearchTracks(String keyword) {
    String baseUrl = AppConstants.SERVICE_URL + "/tracks/search?";
    if (baseUrl != null) {
      try {
        StringBuffer urlBuffer = new StringBuffer(baseUrl);
        urlBuffer.append("q=").append(TextUtils.urlEncodeUTF8(keyword));
        urlBuffer.append("&lang=").append(I18N.getLanguage());
        urlBuffer.append("&service=").append(SettingsManager.getInstance().getCurrentService());
        urlBuffer
            .append("&quality=")
            .append(SettingsManager.getInstance().getCurrentAudioQuality());
        return urlBuffer.toString();
      } catch (Exception e) {
      }
    }

    return null;
  }

  private ApiEndpoints() {}
}
