package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;
import midplay.model.Track;
import midplay.model.Tracks;

public class LastSessionManager {
  private static final int MAX_SESSION_TRACKS = 100;
  private static LastSessionManager instance;

  public static LastSessionManager getInstance() {
    if (instance == null) {
      instance = new LastSessionManager();
    }
    return instance;
  }

  private final JsonRecordStore storage;
  private JSONObject cached;

  private LastSessionManager() {
    storage = new JsonRecordStore(Configuration.STORAGE_LAST_SESSION, 1, "{}");
  }

  private JSONObject session() {
    if (cached == null) {
      try {
        cached = JSON.getObject(storage.load());
      } catch (Exception e) {
        cached = new JSONObject();
      }
    }
    return cached;
  }

  public void save(String title, Tracks tracks, int index, long positionMicros) {
    if (tracks == null || tracks.getTracks() == null || tracks.getTracks().length == 0) {
      return;
    }
    try {
      Track[] arr = tracks.getTracks();
      int len = arr.length;
      int cap = len < MAX_SESSION_TRACKS ? len : MAX_SESSION_TRACKS;
      int start = 0;
      if (len > cap) {
        int half = cap / 2;
        start = index - half;
        if (start < 0) {
          start = 0;
        }
        if (start > len - cap) {
          start = len - cap;
        }
      }
      JSONArray items = new JSONArray();
      for (int i = 0; i < cap; i++) {
        Track t = arr[start + i];
        if (t != null) {
          items.add(t.toJSON());
        }
      }
      int clampedIndex = index - start;
      if (clampedIndex < 0) {
        clampedIndex = 0;
      }
      if (clampedIndex >= cap) {
        clampedIndex = cap - 1;
      }
      JSONObject tracksObj = new JSONObject();
      tracksObj.put("Items", items);
      JSONObject root = new JSONObject();
      root.put("title", title == null ? "" : title);
      root.put("index", clampedIndex);
      root.put("position", positionMicros < 0 ? 0L : positionMicros);
      root.put("tracks", tracksObj);
      cached = root;
      storage.save(root.toString());
    } catch (Exception e) {
    }
  }

  public boolean hasSession() {
    try {
      JSONObject root = session();
      if (!root.has("tracks")) {
        return false;
      }
      return root.getObject("tracks").getArray("Items").size() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public String getTitle() {
    return session().getString("title", "");
  }

  public int getIndex() {
    return session().getInt("index", 0);
  }

  public long getPosition() {
    return session().getLong("position", 0L);
  }

  public Tracks getTracks() {
    try {
      Tracks result = new Tracks();
      result.parse(session().getObject("tracks").toString());
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  public void clear() {
    cached = new JSONObject();
    try {
      storage.save("{}");
    } catch (RecordStoreException e) {
    }
  }

  public void close() {
    storage.close();
  }
}
