package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;
import midplay.model.Playlist;
import midplay.model.RecentItem;
import midplay.model.Track;

public class RecentManager {
  private static final int MAX_ITEMS = 30;
  private static RecentManager instance;

  public static RecentManager getInstance() {
    if (instance == null) {
      instance = new RecentManager();
    }
    return instance;
  }

  private final JsonRecordStore storage;
  private JSONArray cached;

  private RecentManager() {
    storage = new JsonRecordStore(Configuration.STORAGE_RECENT, 1, "[]");
  }

  private JSONArray items() {
    if (cached == null) {
      try {
        cached = JSON.getArray(storage.load());
      } catch (Exception e) {
        cached = new JSONArray();
      }
    }
    return cached;
  }

  public void recordTrack(Track track) {
    if (track == null) {
      return;
    }
    JSONArray arr = items();
    JSONArray next = new JSONArray();
    JSONObject entry = new JSONObject();
    entry.put("type", RecentItem.TRACK);
    entry.put("data", track.toJSON());
    next.add(entry);
    int carried = 0;
    for (int i = 0; i < arr.size() && carried < MAX_ITEMS - 1; i++) {
      JSONObject existing = arr.getObject(i);
      boolean dup = false;
      try {
        if (existing.getInt("type", -1) == RecentItem.TRACK) {
          Track existingTrack = Track.fromJSON(existing.getObject("data"));
          dup = existingTrack != null && track.isSame(existingTrack);
        }
      } catch (Exception e) {
      }
      if (dup) {
        continue;
      }
      next.add(existing);
      carried++;
    }
    save(next);
  }

  public void recordFolder(Playlist folder) {
    if (folder == null) {
      return;
    }
    record(RecentItem.FOLDER, folder.getKey(), folder.toJSON());
  }

  private void record(int type, String key, JSONObject data) {
    String dedup = type + ":" + (key == null ? "" : key);
    JSONArray arr = items();
    JSONArray next = new JSONArray();
    JSONObject entry = new JSONObject();
    entry.put("type", type);
    entry.put("dedup", dedup);
    entry.put("data", data);
    next.add(entry);
    int carried = 0;
    for (int i = 0; i < arr.size() && carried < MAX_ITEMS - 1; i++) {
      JSONObject existing = arr.getObject(i);
      if (!dedup.equals(existing.getString("dedup", ""))) {
        next.add(existing);
        carried++;
      }
    }
    save(next);
  }

  public RecentItem[] getItems() {
    JSONArray arr = items();
    RecentItem[] result = new RecentItem[arr.size()];
    int n = 0;
    for (int i = 0; i < arr.size(); i++) {
      try {
        JSONObject entry = arr.getObject(i);
        int type = entry.getInt("type", -1);
        JSONObject data = entry.getObject("data");
        if (type == RecentItem.TRACK) {
          Track t = Track.fromJSON(data);
          if (t != null) {
            result[n++] = RecentItem.forTrack(t);
          }
        } else if (type == RecentItem.FOLDER) {
          Playlist p = Playlist.fromJSON(data);
          if (p != null) {
            result[n++] = RecentItem.forFolder(p);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (n != result.length) {
      RecentItem[] trimmed = new RecentItem[n];
      System.arraycopy(result, 0, trimmed, 0, n);
      return trimmed;
    }
    return result;
  }

  public void clear() {
    cached = new JSONArray();
    try {
      storage.save("[]");
    } catch (RecordStoreException e) {
    }
  }

  private void save(JSONArray arr) {
    cached = arr;
    try {
      storage.save(arr.toString());
    } catch (RecordStoreException e) {
    }
  }

  public void close() {
    storage.close();
  }
}
