package midplay.ui;

import midplay.store.Configuration;
import midplay.store.JsonRecordStore;
import midplay.util.Utils;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.rms.RecordStoreException;
import midplay.model.MenuItem;

public class MenuManager {
  private static final int MENU_ID = 1;
  private static MenuManager instance;

  public static MenuManager getInstance() {
    if (instance == null) {
      instance = new MenuManager();
    }
    return instance;
  }

  private final JsonRecordStore storage;
  private final Hashtable actions;
  private final Vector menuItems;
  // Cached sorted views of menuItems. The menu config only changes through
  // updateItemOrder/setItemEnabled (and initial load), so recomputing a Vector +
  // sort + array copy on every menu render is wasted work.
  private MenuItem[] sortedEnabledCache;
  private MenuItem[] sortedAllCache;

  private MenuManager() {
    storage = new JsonRecordStore(Configuration.STORAGE_MENU, MENU_ID, createDefaultMenuJSON().toString());
    actions = new Hashtable();
    menuItems = new Vector();
    loadMenuConfig();
  }

  private JSONArray getMenuJSON() {
    try {
      return JSON.getArray(storage.load());
    } catch (JSONException e) {
    }
    return createDefaultMenuJSON();
  }

  private void loadMenuConfig() {
    JSONArray config = getMenuJSON();
    menuItems.removeAllElements();
    for (int i = 0; i < config.size(); i++) {
      JSONObject item = config.getObject(i);
      MenuItem menuItem =
          new MenuItem(
              item.getString("key"), item.getInt("order"), item.getBoolean("enabled", true));
      menuItems.addElement(menuItem);
    }
    invalidateMenuCache();
  }

  private void invalidateMenuCache() {
    sortedEnabledCache = null;
    sortedAllCache = null;
  }

  private JSONArray createDefaultMenuJSON() {
    JSONArray defaultConfig = new JSONArray();
    defaultConfig.add(createMenuItem(Configuration.MENU_SEARCH, 1, true));
    defaultConfig.add(createMenuItem(Configuration.MENU_FAVORITES, 2, true));
    defaultConfig.add(createMenuItem(Configuration.MENU_DISCOVER_PLAYLISTS, 3, true));
    defaultConfig.add(createMenuItem(Configuration.MENU_SETTINGS, 4, true));
    defaultConfig.add(createMenuItem(Configuration.MENU_ABOUT, 5, true));
    return defaultConfig;
  }

  private JSONObject createMenuItem(String key, int order, boolean enabled) {
    JSONObject item = new JSONObject();
    item.put("key", key);
    item.put("order", order);
    item.put("enabled", enabled);
    return item;
  }

  public void saveMenuConfig() {
    try {
      JSONArray config = new JSONArray();
      for (int i = 0; i < menuItems.size(); i++) {
        MenuItem item = (MenuItem) menuItems.elementAt(i);
        config.add(createMenuItem(item.key, item.order, item.enabled));
      }
      storage.save(config.toString());
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public void registerAction(String key, Runnable action) {
    if (key != null && action != null) {
      actions.put(key, action);
    }
  }

  public void executeAction(String key) {
    if (key == null) {
      return;
    }
    Runnable action = (Runnable) actions.get(key);
    if (action != null) {
      try {
        action.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private MenuItem[] collectMenuItems(boolean enabledOnly) {
    Vector items = new Vector();
    for (int i = 0; i < menuItems.size(); i++) {
      MenuItem item = (MenuItem) menuItems.elementAt(i);
      if (!enabledOnly || item.enabled) {
        items.addElement(item);
      }
    }
    Utils.sortMenuItemsByOrder(items);
    MenuItem[] result = new MenuItem[items.size()];
    for (int i = 0; i < items.size(); i++) {
      result[i] = (MenuItem) items.elementAt(i);
    }
    return result;
  }

  public MenuItem[] getSortedMenuItems() {
    if (sortedEnabledCache == null) {
      sortedEnabledCache = collectMenuItems(true);
    }
    return sortedEnabledCache;
  }

  public void updateItemOrder(String key, int newOrder) {
    if (key == null) {
      return;
    }
    for (int i = 0; i < menuItems.size(); i++) {
      MenuItem item = (MenuItem) menuItems.elementAt(i);
      if (key.equals(item.key)) {
        item.order = newOrder;
        saveMenuConfig();
        invalidateMenuCache();
        return;
      }
    }
  }

  public void setItemEnabled(String key, boolean enabled) {
    if (key == null) {
      return;
    }
    for (int i = 0; i < menuItems.size(); i++) {
      MenuItem item = (MenuItem) menuItems.elementAt(i);
      if (key.equals(item.key)) {
        item.enabled = enabled;
        saveMenuConfig();
        invalidateMenuCache();
        return;
      }
    }
  }

  public MenuItem[] getAllMenuItems() {
    if (sortedAllCache == null) {
      sortedAllCache = collectMenuItems(false);
    }
    return sortedAllCache;
  }

  public void close() {
    if (storage != null) {
      storage.close();
    }
  }
}
