package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;
import midplay.model.Track;
import midplay.model.Tracks;

// Persists the last playback session (playlist title, track list, current index,
// and in-track position) so the app can offer to resume it on next launch. One
// JSON document in RMS, mirroring SettingsManager. Tracks are stored
// Items-wrapped so they round-trip through the existing Tracks.fromJSON parse
// path. Position is in microseconds and is applied as a seek after the resumed
// track starts (seek handles both the pass_url native setMediaTime path and the
// InputStream byte-offset reload).
public class LastSessionManager {
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
      JSONArray items = new JSONArray();
      for (int i = 0; i < arr.length; i++) {
        if (arr[i] != null) {
          items.add(arr[i].toJSON());
        }
      }
      JSONObject tracksObj = new JSONObject();
      tracksObj.put("Items", items);
      JSONObject root = new JSONObject();
      root.put("title", title == null ? "" : title);
      root.put("index", index);
      // ponytail: store position as a JSON number (micros). Long fits JS numbers
      // fine for media durations (<2^53); no string parse round-trip needed.
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

  // Microseconds into the resumed track to seek to. 0 (or absent on older
  // saves) means start from the beginning — the pre-position-save behavior.
  public long getPosition() {
    return session().getLong("position", 0L);
  }

  public Tracks getTracks() {
    try {
      return new Tracks().fromJSON(session().getObject("tracks").toString());
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
