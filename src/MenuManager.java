import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.rms.RecordStoreException;
import model.MenuItem;

public class MenuManager {
  private static final int MENU_ID = 1;
  private static MenuManager instance;

  public static MenuManager getInstance() {
    if (instance == null) {
      instance = new MenuManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;
  private final Hashtable actions;
  private final Vector menuItems;

  private MenuManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_MENU);
    actions = new Hashtable();
    menuItems = new Vector();
    loadMenuConfig();
  }

  private JSONArray getMenuJSON() {
    try {
      if (storage.recordExists(MENU_ID)) {
        String jsonString = storage.getRecordAsString(MENU_ID);
        return JSON.getArray(jsonString);
      }
    } catch (RecordStoreException e) {
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
  }

  private JSONArray createDefaultMenuJSON() {
    JSONArray defaultConfig = new JSONArray();
    JSONObject searchItem = new JSONObject();
    searchItem.put("key", Configuration.MENU_SEARCH);
    searchItem.put("order", 1);
    searchItem.put("enabled", true);
    defaultConfig.add(searchItem);

    JSONObject favoritesItem = new JSONObject();
    favoritesItem.put("key", Configuration.MENU_FAVORITES);
    favoritesItem.put("order", 2);
    favoritesItem.put("enabled", true);
    defaultConfig.add(favoritesItem);

    JSONObject playlistsItem = new JSONObject();
    playlistsItem.put("key", Configuration.MENU_DISCOVER_PLAYLISTS);
    playlistsItem.put("order", 3);
    playlistsItem.put("enabled", true);
    defaultConfig.add(playlistsItem);

    JSONObject chatItem = new JSONObject();
    chatItem.put("key", Configuration.MENU_CHAT);
    chatItem.put("order", 4);
    chatItem.put("enabled", true);
    defaultConfig.add(chatItem);

    JSONObject settingsItem = new JSONObject();
    settingsItem.put("key", Configuration.MENU_SETTINGS);
    settingsItem.put("order", 5);
    settingsItem.put("enabled", true);
    defaultConfig.add(settingsItem);

    JSONObject aboutItem = new JSONObject();
    aboutItem.put("key", Configuration.MENU_ABOUT);
    aboutItem.put("order", 6);
    aboutItem.put("enabled", true);
    defaultConfig.add(aboutItem);

    return defaultConfig;
  }

  private void ensureMenuRecordExists() throws RecordStoreException {
    if (!storage.recordExists(MENU_ID)) {
      JSONArray defaultConfig = createDefaultMenuJSON();
      while (storage.getNumRecords() < MENU_ID) {
        if (storage.getNumRecords() == MENU_ID - 1) {
          storage.addRecord(defaultConfig.toString());
        } else {
          storage.addRecord("");
        }
      }
    }
  }

  private void saveSetting(String value) throws RecordStoreException {
    ensureMenuRecordExists();
    storage.setRecord(MENU_ID, value);
  }

  public void saveMenuConfig() {
    try {
      JSONArray config = new JSONArray();
      for (int i = 0; i < menuItems.size(); i++) {
        MenuItem item = (MenuItem) menuItems.elementAt(i);
        JSONObject obj = new JSONObject();
        obj.put("key", item.key);
        obj.put("order", item.order);
        obj.put("enabled", item.enabled);
        config.add(obj);
      }
      saveSetting(config.toString());
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

  public MenuItem[] getSortedMenuItems() {
    Vector enabledItems = new Vector();
    for (int i = 0; i < menuItems.size(); i++) {
      MenuItem item = (MenuItem) menuItems.elementAt(i);
      if (item.enabled) {
        enabledItems.addElement(item);
      }
    }
    MIDPlay.bubbleSort(enabledItems, 2);
    MenuItem[] result = new MenuItem[enabledItems.size()];
    for (int i = 0; i < enabledItems.size(); i++) {
      result[i] = (MenuItem) enabledItems.elementAt(i);
    }
    return result;
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
        return;
      }
    }
  }

  public MenuItem[] getAllMenuItems() {
    Vector allItems = new Vector();
    for (int i = 0; i < menuItems.size(); i++) {
      MenuItem item = (MenuItem) menuItems.elementAt(i);
      allItems.addElement(item);
    }
    MIDPlay.bubbleSort(allItems, 2);
    MenuItem[] result = new MenuItem[allItems.size()];
    for (int i = 0; i < allItems.size(); i++) {
      result[i] = (MenuItem) allItems.elementAt(i);
    }
    return result;
  }

  public void close() {
    if (storage != null) {
      storage.closeRecordStore();
    }
  }
}
