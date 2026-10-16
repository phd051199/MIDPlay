package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;

public final class UriHistoryManager {
  private static final int MAX_ITEMS = 10;
  private static final String STORAGE_NAME = "open_uri_history";
  private static UriHistoryManager instance;

  public static UriHistoryManager getInstance() {
    if (instance == null) {
      instance = new UriHistoryManager();
    }
    return instance;
  }

  private final JsonRecordStore storage = new JsonRecordStore(STORAGE_NAME, 1, "[]");
  private JSONArray entries;

  private UriHistoryManager() {}

  public String[] getItems() {
    JSONArray items = items();
    String[] result = new String[items.size()];
    int count = 0;
    for (int i = 0; i < items.size(); i++) {
      try {
        String uri = items.getObject(i).getString("uri", "");
        if (uri.length() > 0) {
          result[count++] = uri;
        }
      } catch (Exception e) {
      }
    }
    if (count == result.length) {
      return result;
    }
    String[] trimmed = new String[count];
    System.arraycopy(result, 0, trimmed, 0, count);
    return trimmed;
  }

  public void record(String uri) {
    if (uri == null || uri.length() == 0) {
      return;
    }
    JSONArray next = new JSONArray();
    add(next, uri);
    JSONArray current = items();
    for (int i = 0; i < current.size() && next.size() < MAX_ITEMS; i++) {
      try {
        String existing = current.getObject(i).getString("uri", "");
        if (!uri.equals(existing) && existing.length() > 0) {
          add(next, existing);
        }
      } catch (Exception e) {
      }
    }
    save(next);
  }

  public void clear() {
    save(new JSONArray());
  }

  private JSONArray items() {
    if (entries == null) {
      try {
        entries = JSON.getArray(storage.load());
      } catch (Exception e) {
        entries = new JSONArray();
      }
    }
    return entries;
  }

  private static void add(JSONArray entries, String uri) {
    JSONObject entry = new JSONObject();
    entry.put("uri", uri);
    entries.add(entry);
  }

  private void save(JSONArray next) {
    entries = next;
    try {
      storage.save(next.toString());
    } catch (RecordStoreException e) {
    }
  }
}
