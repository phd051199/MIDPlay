package midplay.model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.util.Vector;

public abstract class JsonListResult {
  private boolean hasMore;

  public boolean hasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public abstract int size();

  protected abstract JsonItemFactory factory();

  protected abstract void acceptItems(Vector items);

  public void parse(String jsonString) {
    Vector items;
    if (jsonString == null || jsonString.trim().length() == 0) {
      setHasMore(false);
      items = new Vector();
    } else {
      try {
        JSONObject json = JSON.getObject(jsonString);
        setHasMore(readHasMore(json));
        items = buildItems(json, factory());
      } catch (Exception e) {
        e.printStackTrace();
        setHasMore(false);
        items = new Vector();
      }
    }
    acceptItems(items);
  }

  protected static boolean readHasMore(JSONObject json) {
    return json.has("GetMore") && json.getString("GetMore", "no").equals("yes");
  }

  protected static Vector extractItems(JSONObject json) {
    if (!json.has("Items")) {
      return null;
    }
    JSONArray jsonArray = json.getArray("Items");
    Vector items = new Vector();
    for (int i = 0; i < jsonArray.size(); i++) {
      JSONObject item = null;
      try {
        item = jsonArray.getObject(i);
      } catch (Exception e) {
        continue;
      }
      if (item != null) {
        items.addElement(item);
      }
    }
    return items;
  }

  protected static Vector buildItems(JSONObject json, JsonItemFactory factory) {
    Vector jsonItems = extractItems(json);
    Vector result = new Vector();
    if (jsonItems == null) {
      return result;
    }
    for (int i = 0; i < jsonItems.size(); i++) {
      Base item = factory.create((JSONObject) jsonItems.elementAt(i));
      if (item != null) {
        result.addElement(item);
      }
    }
    return result;
  }

  public interface JsonItemFactory {
    Base create(JSONObject json);
  }
}
