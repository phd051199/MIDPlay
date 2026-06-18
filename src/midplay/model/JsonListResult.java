package midplay.model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.util.Vector;

// Base for paged list results (Tracks/Playlists): owns the shared JSON-list
// parsing algorithm and the hasMore flag. Subclasses supply the item factory,
// how the typed items are stashed, and their element count. Distinct from the
// domain Base supertype that Track/Playlist use.
public abstract class JsonListResult {
  private boolean hasMore;

  public boolean hasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  // Number of parsed elements; lets callers test emptiness without knowing the
  // concrete subtype.
  public abstract int size();

  protected abstract JsonItemFactory factory();

  // Stash the parsed items (a Vector of the domain subtype) into the typed
  // array; an empty vector means "no items" (reset to an empty array).
  protected abstract void acceptItems(Vector items);

  // Parse a list response into this result. Null/blank or unparseable input
  // yields an empty item set and hasMore=false; parse failures are swallowed
  // with a stack trace (matches the former per-class behavior).
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

  // Parses the "Items" array into a Vector of non-null JSONObjects,
  // skipping entries that fail to parse. Returns null when there is no "Items" key.
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

  // Builds the typed item Vector from the "Items" array of a parsed list response,
  // skipping entries that fail to parse. Returns an empty Vector when there is no
  // "Items" key, so callers can unconditionally copyInto a freshly-sized array.
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

  // Factory that turns one "Items" entry into its domain object.
  public interface JsonItemFactory {
    Base create(JSONObject json);
  }
}
