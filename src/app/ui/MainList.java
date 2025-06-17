package app.ui;

import app.common.ParseData;
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

  private Command nowPlayingCommand;
  private Command selectCommand;
  private Command exitCommand;
  private Utils.BreadCrumbTrail observer;
  Thread mLoaDataThread;

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

  private Image getImage(int index, Image[] imageElements) {
    return imageElements != null && imageElements.length > index ? imageElements[index] : null;
  }

  private void initCommands() {
    this.selectCommand = new Command(I18N.tr("select"), Command.OK, 0);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 1);
    this.exitCommand = new Command(I18N.tr("exit"), Command.EXIT, 2);

    this.addCommand(this.selectCommand);
    this.addCommand(this.nowPlayingCommand);
    this.addCommand(this.exitCommand);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.selectCommand || c == List.SELECT_COMMAND) {
      this.itemAction();
    } else if (c == this.exitCommand) {
      if (this.observer != null) {
        this.observer.goBack();
      }
    } else if (c == this.nowPlayingCommand) {
      gotoNowPlaying(this.observer);
    }
  }

  public static void gotoNowPlaying(Utils.BreadCrumbTrail observer) {
    if (SongList.playerCanvas != null) {
      SongList.playerCanvas.setObserver(observer);
      observer.go(SongList.playerCanvas);
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

  private void showCategoryList(String title, Vector items, String from, String itemType) {
    CategoryList cateCanvas = new CategoryList(title, items);
    cateCanvas.setObserver(this.observer);
    this.observer.replaceCurrent(cateCanvas);
  }

  private void gotoSetting() {
    SettingForm settingForm = new SettingForm(I18N.tr("settings"), observer);
    observer.go(settingForm);
  }

  private void gotoPlaylist(final String type) {
    loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            return ParseData.parsePlaylist(1, 30, type, "0");
          }
        },
        I18N.tr("hot_playlists"),
        "hot",
        "playlist");
  }

  public static void gotoSearch(Utils.BreadCrumbTrail observer) {
    SearchForm searchForm = new SearchForm(I18N.tr("search_title"));
    searchForm.setObserver(observer);
    observer.go(searchForm);
  }

  public static void gotoAbout(Utils.BreadCrumbTrail observer) {
    AboutForm aboutForm = new AboutForm(I18N.tr("about"), observer);
    observer.go(aboutForm);
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

  private interface DataLoader {
    Vector load() throws Exception;
  }

  private void loadDataAsync(
      final DataLoader loader, final String title, final String from, final String itemType) {
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  Vector items = loader.load();
                  if (items == null) {
                    showErrorMessage(I18N.tr("connection_error"));
                  } else if (items.isEmpty()) {
                    showErrorMessage(I18N.tr("no_data"));
                  } else {
                    if ("category".equals(from)) {
                      showCategoryList(title, items, from, itemType);
                    } else {
                      showPlaylistList(title, items, from, itemType);
                    }
                  }
                } catch (Exception e) {
                  showErrorMessage(I18N.tr("connection_error"));
                }
              }
            });
    this.mLoaDataThread.start();
  }

  private void showErrorMessage(String message) {
    displayMessage(I18N.tr("app_name"), message, "error", this.observer, this);
  }

  private void showPlaylistList(String title, Vector items, String from, String itemType) {
    PlaylistList playlistList = new PlaylistList(title, items, from, "", itemType);
    playlistList.setObserver(this.observer);
    this.observer.replaceCurrent(playlistList);
  }

  private void itemAction() {
    int selectedIndex = this.getSelectedIndex();
    switch (selectedIndex) {
      case 0:
        this.gotoSearch(this.observer);
        break;
      case 1:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoBillboard();
        break;
      case 2:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoPlaylist("new");
        break;
      case 3:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoPlaylist("hot");
        break;
      case 4:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoCate();
        break;
      case 5:
        this.gotoSetting();
        break;
      case 6:
        this.gotoAbout(this.observer);
        break;
      default:
        break;
    }
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

  public void cancel() {
    this.quit();
  }

  public synchronized void quit() {
    try {
      if (this.mLoaDataThread != null) {
        this.mLoaDataThread.interrupt();
      }
    } catch (Exception var2) {
      var2.printStackTrace();
    }
  }
}
