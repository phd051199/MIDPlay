package midplay.net;

import midplay.MIDPlay;
import midplay.store.SettingsManager;
import midplay.util.Lang;
import midplay.util.Utils;

public class URLProvider {
  public static final String SERVICE_URL = "http://music.s60tube.io.vn";
  private static final SettingsManager settingsManager = SettingsManager.getInstance();

  public static String checkForUpdate() {
    return SERVICE_URL + "/update?version=" + MIDPlay.APP_VERSION;
  }

  private static void appendCommonParams(StringBuffer urlBuffer) {
    urlBuffer
        .append("&lang=")
        .append(Lang.getCurrentLang())
        .append("&service=")
        .append(settingsManager.getCurrentService());
  }

  public static String getHotPlaylists(int pageIndex) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/playlist")
            .append("?page=")
            .append(pageIndex)
            .append("&type=new");
    appendCommonParams(urlBuffer);
    return urlBuffer.toString();
  }

  public static String searchPlaylists(String keyword, String type, int pageIndex) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/search")
            .append("?q=")
            .append(Utils.urlEncode(keyword))
            .append("&page=")
            .append(pageIndex)
            .append("&type=")
            .append(type);
    appendCommonParams(urlBuffer);
    return urlBuffer.toString();
  }

  public static String searchTracks(String keyword, int pageIndex) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/tracks/search")
            .append("?q=")
            .append(Utils.urlEncode(keyword))
            .append("&page=")
            .append(pageIndex);
    appendCommonParams(urlBuffer);
    return urlBuffer.toString();
  }

  public static String getTracks(String listKey) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/tracks")
            .append("?listkey=")
            .append(listKey)
            .append("&quality=")
            .append(settingsManager.getCurrentQuality());
    appendCommonParams(urlBuffer);
    return urlBuffer.toString();
  }

  public static String getSizedImage(String imageUrl, int size) {
    StringBuffer urlBuffer =
        new StringBuffer("http://wsrv.nl/?url=")
            .append(Utils.urlEncode(imageUrl))
            .append("&output=jpg")
            .append("&w=")
            .append(size)
            .append("&h=")
            .append(size)
            .append("&fit=cover");
    return urlBuffer.toString();
  }

  public static String getThemeColor(String hex) {
    return SERVICE_URL + "/theme?color=" + Utils.urlEncode(hex);
  }

  public static String getTakumiTextImage(
      String text, int width, int fontSize, int color, String align) {
    int safeWidth = width > 0 ? width : 1;
    int safeFontSize = fontSize > 0 ? fontSize : 1;
    String hexColor = "#" + Utils.toHexRgb(color);

    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/takumi?text=")
            .append(Utils.urlEncode(text == null ? "" : text))
            .append("&width=")
            .append(safeWidth)
            .append("&fontSize=")
            .append(safeFontSize)
            .append("&color=")
            .append(Utils.urlEncode(hexColor));
    if (align != null && align.length() > 0) {
      urlBuffer.append("&align=").append(Utils.urlEncode(align));
    }
    return urlBuffer.toString();
  }

  private URLProvider() {}
}
