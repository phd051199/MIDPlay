package app.ui;

import app.common.ParseData;
import app.constants.Constants;
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
      switch (i) {
        case 0:
          text += "\n" + I18N.tr("search_playlist");
          break;
        case 1:
          text += "\n" + I18N.tr("latest_updates");
          break;
        case 2:
          text += "\n" + I18N.tr("music_genres");
          break;
        case 3:
          text += "\n" + I18N.tr("language") + ", " + I18N.tr("audio_quality");
          break;
        case 4:
          text += "\n" + I18N.tr("version") + ": " + Constants.APP_VERSION;
          break;

        default:
          break;
      }
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
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector cateItems = ParseData.parseCate(2);
                if (cateItems == null) {
                  MainList.displayMessage(
                      I18N.tr("app_name"),
                      I18N.tr("connection_error"),
                      "error",
                      MainList.this.observer,
                      MainList.this);
                } else if (cateItems.size() == 0) {
                  MainList.displayMessage(
                      I18N.tr("app_name"),
                      I18N.tr("no_data"),
                      "error",
                      MainList.this.observer,
                      MainList.this);
                } else {
                  CategoryList cateCanvas = new CategoryList(I18N.tr("genres"), cateItems);
                  cateCanvas.setObserver(MainList.this.observer);
                  MainList.this.observer.replaceCurrent(cateCanvas);
                }
              }
            });
    this.mLoaDataThread.start();
  }

  private void gotoSetting() {
    SettingForm settingForm = new SettingForm(I18N.tr("settings"), observer);
    observer.go(settingForm);
  }

  private void gotoHotPlaylist() {
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector playlistItems = ParseData.parseHotPlaylist(1, 10);
                if (playlistItems == null) {
                  MainList.displayMessage(
                      I18N.tr("app_name"),
                      I18N.tr("connection_error"),
                      "error",
                      MainList.this.observer,
                      MainList.this);
                } else if (playlistItems.size() == 0) {
                  MainList.displayMessage(
                      I18N.tr("app_name"),
                      I18N.tr("no_data"),
                      "error",
                      MainList.this.observer,
                      MainList.this);
                } else {
                  PlaylistList playlistList =
                      new PlaylistList(I18N.tr("hot_playlists"), playlistItems, "hot", "");
                  playlistList.setObserver(MainList.this.observer);
                  MainList.this.observer.replaceCurrent(playlistList);
                }
              }
            });
    this.mLoaDataThread.start();
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

  private void itemAction() {
    int selectedIndex = this.getSelectedIndex();
    switch (selectedIndex) {
      case 0:
        MainList.gotoSearch(this.observer);
        break;
      case 1:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoHotPlaylist();
        break;
      case 2:
        displayMessage(I18N.tr("app_name"), I18N.tr("loading"), "loading", this.observer, this);
        this.gotoCate();
        break;
      case 3:
        this.gotoSetting();
        break;
      case 4:
        MainList.gotoAbout(this.observer);
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
