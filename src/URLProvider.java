public class URLProvider {
  public static final String SERVICE_URL = "http://music.s60tube.io.vn";
  private static final SettingsManager settingsManager = SettingsManager.getInstance();

  public static String checkForUpdate() {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/update")
            .append("?version=")
            .append(MIDPlay.APP_VERSION);
    return urlBuffer.toString();
  }

  public static String getHotPlaylists(int pageIndex) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/playlist")
            .append("?page=")
            .append(pageIndex)
            .append("&type=new")
            .append("&lang=")
            .append(Lang.getCurrentLang())
            .append("&service=")
            .append(settingsManager.getCurrentService());
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
            .append(type)
            .append("&lang=")
            .append(Lang.getCurrentLang())
            .append("&service=")
            .append(settingsManager.getCurrentService());
    return urlBuffer.toString();
  }

  public static String searchTracks(String keyword, int pageIndex) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/tracks/search")
            .append("?q=")
            .append(Utils.urlEncode(keyword))
            .append("&page=")
            .append(pageIndex)
            .append("&lang=")
            .append(Lang.getCurrentLang())
            .append("&service=")
            .append(settingsManager.getCurrentService());
    return urlBuffer.toString();
  }

  public static String getTracks(String listKey) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/tracks")
            .append("?listkey=")
            .append(listKey)
            .append("&quality=")
            .append(settingsManager.getCurrentQuality())
            .append("&lang=")
            .append(Lang.getCurrentLang())
            .append("&service=")
            .append(settingsManager.getCurrentService());
    return urlBuffer.toString();
  }

  public static String getSizedImage(String imageUrl, int size) {
    StringBuffer wsrvBuffer =
        new StringBuffer("https://wsrv.nl/?url=")
            .append(Utils.urlEncode(imageUrl))
            .append("&output=jpg")
            .append("&w=")
            .append(size)
            .append("&h=")
            .append(size)
            .append("&fit=cover");
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/proxy?url=")
            .append(Utils.urlEncode(wsrvBuffer.toString()));
    return urlBuffer.toString();
  }

  public static String getChatEndpoint(String message, String sessionId) {
    StringBuffer urlBuffer =
        new StringBuffer(SERVICE_URL)
            .append("/chat?")
            .append("lang=")
            .append(Lang.getCurrentLang())
            .append("&m=")
            .append(Utils.urlEncode(message))
            .append("&sessionId=")
            .append(sessionId);
    return urlBuffer.toString();
  }

  private URLProvider() {}
}
