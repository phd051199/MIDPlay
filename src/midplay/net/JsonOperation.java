package midplay.net;

import midplay.model.JsonListResult;
import midplay.model.Playlists;
import midplay.model.Tracks;

public class JsonOperation extends NetworkOperation {

  private final String url;
  private final JsonListResult result;
  private final JsonListListener listener;

  private JsonOperation(String url, JsonListResult result, JsonListListener listener) {
    this.url = url;
    this.result = result;
    this.listener = listener;
  }

  public static JsonOperation searchTracks(String keyword, int page, JsonListListener listener) {
    return new JsonOperation(URLProvider.searchTracks(keyword, page), new Tracks(), listener);
  }

  public static JsonOperation searchTracks(String keyword, JsonListListener listener) {
    return searchTracks(keyword, 1, listener);
  }

  public static JsonOperation getTracks(String listKey, JsonListListener listener) {
    return new JsonOperation(URLProvider.getTracks(listKey), new Tracks(), listener);
  }

  public static JsonOperation searchPlaylists(
      String keyword, String type, int page, JsonListListener listener) {
    return new JsonOperation(
        URLProvider.searchPlaylists(keyword, type, page), new Playlists(), listener);
  }

  public static JsonOperation searchPlaylists(
      String keyword, String type, JsonListListener listener) {
    return searchPlaylists(keyword, type, 1, listener);
  }

  public static JsonOperation getHotPlaylists(int page, JsonListListener listener) {
    return new JsonOperation(URLProvider.getHotPlaylists(page), new Playlists(), listener);
  }

  public static JsonOperation getHotPlaylists(JsonListListener listener) {
    return getHotPlaylists(1, listener);
  }

  protected void execute() {
    fetchText(this.url);
  }

  protected void processResponse(String response) {
    result.parse(response);
    if (result.size() == 0) {
      listener.onNoData();
    } else {
      listener.onDataReceived(result);
    }
  }

  protected void handleNoData() {
    listener.onNoData();
  }

  protected void handleError(Exception e) {
    listener.onError(e);
  }

  public interface JsonListListener {
    void onDataReceived(JsonListResult result);

    void onNoData();

    void onError(Exception e);
  }
}
