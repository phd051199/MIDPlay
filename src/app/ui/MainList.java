package app.ui;

import app.MIDPlay;
import app.common.Common;
import app.common.ParseData;
import app.common.SettingManager;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.ui.category.CategoryList;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class MainList extends List implements CommandListener, LoadDataObserver {

  public static void gotoNowPlaying(Utils.BreadCrumbTrail observer) {
    if (SongList.playerCanvas != null) {
      SongList.playerCanvas.setObserver(observer);
      observer.go(SongList.playerCanvas);
    }
  }

  public static void gotoSearch(Utils.BreadCrumbTrail observer) {
    SearchForm searchForm = new SearchForm(I18N.tr("search_title"));
    searchForm.setObserver(observer);
    observer.go(searchForm);
  }

  public static void displayMessage(
      String title,
      String message,
      String messageType,
      Utils.BreadCrumbTrail observer,
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
  private Command switchServiceCommand;
  private Command exitCommand;
  private Utils.BreadCrumbTrail observer;
  Thread mLoadDataThread;

  public MainList(String title, String[] items, Image[] imageElements) {
    super(title, List.IMPLICIT);
    for (int i = 0; i < items.length; ++i) {
      Image imagePart = getImage(i, imageElements);
      String text = items[i];

      this.append(text, imagePart);
    }
    this.initCommands();
    this.setCommandListener(this);
  }

  public void gotoAbout() {
    AboutForm aboutForm = new AboutForm(I18N.tr("about"), this.observer);
    this.observer.go(aboutForm);
  }

  private Image getImage(int index, Image[] imageElements) {
    return imageElements != null && imageElements.length > index ? imageElements[index] : null;
  }

  private void initCommands() {
    this.switchServiceCommand = new Command(getNextServiceCommandLabel(), Command.SCREEN, 2);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 1);
    this.exitCommand = new Command(I18N.tr("exit"), Command.EXIT, 3);

    this.addCommand(this.switchServiceCommand);
    this.addCommand(this.nowPlayingCommand);
    this.addCommand(this.exitCommand);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == List.SELECT_COMMAND) {
      if (SettingManager.getInstance().getCurrentService().equals("nct")) {
        this.itemActionNCT();
      } else {
        this.itemActionSoundCloud();
      }
    } else if (c == this.exitCommand) {
      if (this.observer != null) {
        this.observer.goBack();
      }
    } else if (c == this.nowPlayingCommand) {
      gotoNowPlaying(this.observer);
    } else if (c == this.switchServiceCommand) {
      switchService();
    }
  }

  public void setObserver(Utils.BreadCrumbTrail mObserver) {
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
    switch (selectedIndex) {
      case 0:
        MainList.gotoSearch(this.observer);
        break;
      case 1:
        this.gotoFavorites();
        break;
      case 2:
        this.gotoChat();
        break;
      case 3:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoCate();
        break;
      case 4:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoPlaylist("new");
        break;
      case 5:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoPlaylist("hot");
        break;
      case 6:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoBillboard();
        break;
      case 7:
        this.gotoSetting();
        break;
      case 8:
        this.gotoAbout();
        break;
      default:
        break;
    }
  }

  private void itemActionSoundCloud() {
    int selectedIndex = this.getSelectedIndex();
    switch (selectedIndex) {
      case 0:
        MainList.gotoSearch(this.observer);
        break;
      case 1:
        this.gotoFavorites();
        break;
      case 2:
        this.gotoChat();
        break;
      case 3:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoPlaylist("discover");
        break;
      case 4:
        this.gotoSetting();
        break;
      case 5:
        this.gotoAbout();
        break;
      default:
        break;
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
  }

  private String getNextServiceCommandLabel() {
    SettingManager settingManager = SettingManager.getInstance();
    String currentService = settingManager.getCurrentService();
    String[] availableServices = settingManager.getAvailableServices();

    String nextService = availableServices[0];
    for (int i = 0; i < availableServices.length - 1; i++) {
      if (availableServices[i].equals(currentService)) {
        nextService = availableServices[i + 1];
        break;
      }
    }

    if (currentService.equals(availableServices[availableServices.length - 1])) {
      nextService = availableServices[0];
    }

    return Common.replace(I18N.tr("switch_service_to"), "{0}", nextService);
  }

  private void switchService() {
    SettingManager settingManager = SettingManager.getInstance();
    String currentService = settingManager.getCurrentService();
    String[] availableServices = settingManager.getAvailableServices();

    String nextService = availableServices[0];
    for (int i = 0; i < availableServices.length - 1; i++) {
      if (availableServices[i].equals(currentService)) {
        nextService = availableServices[i + 1];
        break;
      }
    }

    if (currentService.equals(availableServices[availableServices.length - 1])) {
      nextService = availableServices[0];
    }

    settingManager.saveSettings(
        settingManager.getCurrentLanguage(),
        settingManager.getCurrentAudioQuality(),
        nextService,
        settingManager.isAutoUpdateEnabled() ? "true" : "false",
        settingManager.isLoadPlaylistArtEnabled() ? "true" : "false");

    final MainList mainList = Utils.createMainMenu(observer, nextService);
    if (observer instanceof MIDPlay) {
      ((MIDPlay) observer).clearHistory();
    }
    observer.replaceCurrent(mainList);
  }
}
