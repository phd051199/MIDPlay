package app.ui;

import app.MIDPlay;
import app.common.Common;
import app.common.MenuSettingsManager;
import app.common.ParseData;
import app.constants.Services;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.interfaces.MainObserver;
import app.ui.category.CategoryList;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class MainList extends List implements CommandListener, LoadDataObserver {

  public static void gotoNowPlaying(MainObserver observer) {
    if (SongList.playerCanvas != null) {
      SongList.playerCanvas.setObserver(observer);
      observer.go(SongList.playerCanvas);
    }
  }

  public static void gotoSearch(MainObserver observer) {
    SearchForm searchForm = new SearchForm(I18N.tr("search_title"));
    searchForm.setObserver(observer);
    observer.go(searchForm);
  }

  public static void displayMessage(
      String title,
      String message,
      String messageType,
      MainObserver observer,
      LoadDataObserver loadDataOberserver) {
    MessageForm messageForm = new MessageForm(title, message, messageType);
    messageForm.setObserver(observer);
    if (loadDataOberserver != null) {
      messageForm.setLoadDataOberserver(loadDataOberserver);
    }

    if (messageType.equals("error")) {
      observer.replaceCurrent(messageForm);
    } else {
      observer.go(messageForm);
    }
  }

  private Command nowPlayingCommand;
  private Command reorderCommand;
  private Command saveOrderCommand;
  private Command exitCommand;
  private MainObserver observer;
  private final String service;
  private boolean isReorderMode = false;
  private int selectedItemIndex = -1;
  Thread mLoadDataThread;

  public MainList(String title, String subtitle, String service) {
    super(title, List.IMPLICIT);
    this.setTitle(title);
    this.service = service;
    this.initCommands();
    this.setCommandListener(this);
  }

  public void gotoAbout() {
    AboutForm aboutForm = new AboutForm(I18N.tr("about"), this.observer);
    this.observer.go(aboutForm);
  }

  private void initCommands() {
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 1);
    this.reorderCommand = new Command(I18N.tr("reorder"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("exit"), Command.EXIT, 3);

    this.addCommand(this.nowPlayingCommand);
    this.addCommand(this.reorderCommand);
    this.addCommand(this.exitCommand);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == List.SELECT_COMMAND) {
      if (isReorderMode) {
        handleReorderSelection();
      } else if (Services.NCT.equals(service)) {
        this.itemActionNCT();
      } else {
        this.itemActionSoundCloud();
      }
    } else if (c == this.exitCommand) {
      if (isReorderMode) {
        cancelReorderMode();
      } else if (this.observer != null) {
        this.observer.goBack();
      }
    } else if (c == this.nowPlayingCommand) {
      gotoNowPlaying(this.observer);
    } else if (c == this.reorderCommand) {
      startReorderMode();
    } else if (c == this.saveOrderCommand) {
      saveCurrentOrder();
      exitReorderMode();
    }
  }

  private void startReorderMode() {
    isReorderMode = true;
    selectedItemIndex = -1;

    this.removeCommand(this.nowPlayingCommand);
    this.removeCommand(this.reorderCommand);

    this.saveOrderCommand = new Command(I18N.tr("save"), Command.SCREEN, 1);
    this.addCommand(this.saveOrderCommand);

    this.removeCommand(this.exitCommand);
    this.exitCommand = new Command(I18N.tr("cancel"), Command.BACK, 2);
    this.addCommand(this.exitCommand);

    Alert alert = new Alert("", I18N.tr("reorder_instructions"), null, AlertType.INFO);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  private void exitReorderMode() {
    isReorderMode = false;
    selectedItemIndex = -1;

    this.removeCommand(this.saveOrderCommand);
    this.saveOrderCommand = null;

    this.removeCommand(this.exitCommand);
    this.exitCommand = new Command(I18N.tr("exit"), Command.EXIT, 3);
    this.addCommand(this.exitCommand);

    this.addCommand(this.nowPlayingCommand);
    this.addCommand(this.reorderCommand);
  }

  private void cancelReorderMode() {
    exitReorderMode();

    final MainList mainList = Utils.createMainMenu(observer, service);
    observer.replaceCurrent(mainList);
  }

  private void handleReorderSelection() {
    int currentIndex = this.getSelectedIndex();

    if (selectedItemIndex == -1) {

      selectedItemIndex = currentIndex;
      this.set(currentIndex, "→ " + this.getString(currentIndex), this.getImage(currentIndex));
    } else {

      if (selectedItemIndex != currentIndex) {

        String item1 = this.getString(selectedItemIndex).substring(2);
        Image image1 = this.getImage(selectedItemIndex);
        String item2 = this.getString(currentIndex);
        Image image2 = this.getImage(currentIndex);

        this.set(selectedItemIndex, item2, image2);
        this.set(currentIndex, item1, image1);
      } else {

        this.set(
            currentIndex, this.getString(currentIndex).substring(2), this.getImage(currentIndex));
      }

      selectedItemIndex = -1;
    }
  }

  private void saveCurrentOrder() {
    MenuSettingsManager menuSettingsManager = MenuSettingsManager.getInstance();
    int count = this.size();
    int[] newOrder = new int[count];

    for (int i = 0; i < count; i++) {

      String itemText = this.getString(i);

      if (selectedItemIndex == i) {
        itemText = itemText.substring(2);
      }

      int originalIndex = findOriginalIndex(itemText);
      if (originalIndex != -1) {
        newOrder[i] = originalIndex;
      } else {
        newOrder[i] = i;
      }
    }

    if (Services.NCT.equals(service)) {
      menuSettingsManager.saveNctMenuOrder(newOrder);
    } else {
      menuSettingsManager.saveSoundcloudMenuOrder(newOrder);
    }

    Alert alert = new Alert("", I18N.tr("order_saved"), null, AlertType.CONFIRMATION);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  private int findOriginalIndex(String itemText) {
    if (Services.NCT.equals(service)) {
      for (int i = 0; i < Utils.MAIN_MENU_ITEMS_NCT.length; i++) {
        if (itemText.equals(I18N.tr(Utils.MAIN_MENU_ITEMS_NCT[i]))) {
          return i;
        }
      }
    } else {
      for (int i = 0; i < Utils.MAIN_MENU_ITEMS_SOUNDCLOUD.length; i++) {
        if (itemText.equals(I18N.tr(Utils.MAIN_MENU_ITEMS_SOUNDCLOUD[i]))) {
          return i;
        }
      }
    }
    return -1;
  }

  public void setObserver(MainObserver mObserver) {
    this.observer = mObserver;
  }

  private void gotoCate() {
    loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            return ParseData.parseCate(2);
          }
        },
        I18N.tr("genres"),
        "category",
        "category");
  }

  private void gotoFavorites() {
    FavoritesList favoritesList = new FavoritesList(this.observer);
    this.observer.go(favoritesList);
  }

  private void showCategoryList(String title, Vector items, String from, String itemType) {
    CategoryList cateCanvas = new CategoryList(title, items);
    cateCanvas.setObserver(this.observer);
    this.observer.replaceCurrent(cateCanvas);
  }

  private void gotoSetting() {
    SettingForm settingForm = new SettingForm(I18N.tr("settings"), this.observer);
    this.observer.go(settingForm);
  }

  private void gotoChat() {
    ChatCanvas chatCanvas = new ChatCanvas(I18N.tr("chat"), this.observer);
    this.observer.go(chatCanvas);
  }

  private void gotoPlaylist(final String type) {
    String title = I18N.tr("discover_playlists");

    if (type.equals("new")) {
      title = I18N.tr("new_playlists");
    } else if (type.equals("hot")) {
      title = I18N.tr("hot_playlists");
    }
    loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            return ParseData.parsePlaylist(1, 30, type, "0");
          }
        },
        title,
        "hot",
        "playlist");
  }

  private void gotoBillboard() {
    loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            return ParseData.parseBillboard(1, 10);
          }
        },
        I18N.tr("billboard"),
        "billboard",
        "chart");
  }

  private void loadDataAsync(
      final DataLoader loader, final String title, final String from, final String itemType) {
    Common.loadDataAsync(
        loader,
        new LoadDataListener() {
          public void loadDataCompleted(Vector items) {
            if ("category".equals(from)) {
              showCategoryList(title, items, from, itemType);
            } else {
              showPlaylistList(title, items, from, itemType);
            }
          }

          public void loadError() {
            showErrorMessage(I18N.tr("connection_error"));
          }

          public void noData() {
            showErrorMessage(I18N.tr("no_data"));
          }
        },
        this.mLoadDataThread);
  }

  private void showErrorMessage(String message) {
    displayMessage(I18N.tr("app_name"), message, "error", this.observer, this);
  }

  private void showPlaylistList(String title, Vector items, String from, String itemType) {
    boolean showAddToFavorites = !"billboard".equals(from);
    PlaylistList playlistList =
        new PlaylistList(title, items, from, "", itemType, showAddToFavorites);
    playlistList.setObserver(this.observer);
    this.observer.replaceCurrent(playlistList);
  }

  private void itemActionNCT() {
    int selectedIndex = this.getSelectedIndex();
    MenuSettingsManager menuSettingsManager = MenuSettingsManager.getInstance();
    int[] order = menuSettingsManager.getNctMenuOrder(9);
    int originalIndex = order[selectedIndex];

    if (originalIndex == 0) { // search
      MainList.gotoSearch(this.observer);
    } else if (originalIndex == 1) { // favorites
      this.gotoFavorites();
    } else if (originalIndex == 2) { // genres
      displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
      this.gotoCate();
    } else if (originalIndex == 3) { // billboard
      displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
      this.gotoBillboard();
    } else if (originalIndex == 4) { // new_playlists
      displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
      this.gotoPlaylist("new");
    } else if (originalIndex == 5) { // hot_playlists
      displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
      this.gotoPlaylist("hot");
    } else if (originalIndex == 6) { // chat
      this.gotoChat();
    } else if (originalIndex == 7) { // settings
      this.gotoSetting();
    } else if (originalIndex == 8) { // app_info
      this.gotoAbout();
    }
  }

  private void itemActionSoundCloud() {
    int selectedIndex = this.getSelectedIndex();
    MenuSettingsManager menuSettingsManager = MenuSettingsManager.getInstance();
    int[] order = menuSettingsManager.getSoundcloudMenuOrder(6);
    int originalIndex = order[selectedIndex];

    if (originalIndex == 0) { // search
      MainList.gotoSearch(this.observer);
    } else if (originalIndex == 1) { // favorites
      this.gotoFavorites();
    } else if (originalIndex == 2) { // discover_playlists
      displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
      this.gotoPlaylist("discover");
    } else if (originalIndex == 3) { // chat
      this.gotoChat();
    } else if (originalIndex == 4) { // settings
      this.gotoSetting();
    } else if (originalIndex == 5) { // app_info
      this.gotoAbout();
    }
  }

  public void cancel() {
    this.quit();
  }

  public synchronized void quit() {
    try {
      if (this.mLoadDataThread != null && this.mLoadDataThread.isAlive()) {
        this.mLoadDataThread.interrupt();
      }
    } catch (Exception var2) {
    }

    MenuSettingsManager menuSettingsManager = MenuSettingsManager.getInstance();
    if (menuSettingsManager != null) {
      menuSettingsManager.shutdown();
    }
  }
}
