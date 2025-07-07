package app.ui.category;

import app.common.Common;
import app.common.ParseData;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.interfaces.MainObserver;
import app.model.Category;
import app.ui.MainList;
import app.ui.PlaylistList;
import app.utils.I18N;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class CategorySubList extends List implements CommandListener, LoadDataObserver {

  private Command exitCommand;
  private Command nowPlayingCommand;
  private Command searchCommand;

  private final Vector subItems;
  private final Vector images;
  private MainObserver observer;
  private Thread mLoadDataThread;
  private Image defaultImage;

  public CategorySubList(String title, Vector subs) {
    super(title, List.IMPLICIT);
    this.subItems = subs;
    this.images = new Vector();
    this.loadDefaultImage();
    this.initCommands();
    this.initComponents();
    this.setCommandListener(this);
  }

  private void initCommands() {
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(I18N.tr("search"), Command.SCREEN, 3);
    this.addCommand(this.exitCommand);
    this.addCommand(this.searchCommand);
    this.addCommand(this.nowPlayingCommand);
  }

  private void initComponents() {
    try {
      this.images.removeAllElements();
      this.deleteAll();
      for (int i = 0; i < this.subItems.size(); ++i) {
        Category cate = (Category) this.subItems.elementAt(i);
        this.images.addElement(this.defaultImage);
        this.append(cate.getName(), this.defaultImage);
      }
      if (this.size() > 0) {
        this.setSelectedIndex(0, true);
      }
    } catch (Exception e) {
    }
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/FolderSound.png");
    } catch (Exception e) {
    }
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      int selected = this.getSelectedIndex();
      if (selected >= 0 && selected < this.subItems.size()) {
        Category cate = (Category) this.subItems.elementAt(selected);
        this.gotoPlaylistByCate(cate.getId(), 1, 10, cate.getName());
      }
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      MainList.gotoSearch(this.observer);
    }
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoadDataThread != null && this.mLoadDataThread.isAlive()) {
        this.mLoadDataThread.join();
      }
    } catch (InterruptedException var2) {
    }
  }

  private void gotoPlaylistByCate(
      final String genKey, final int curPage, final int perPage, final String title) {
    MainList.displayMessage(title, I18N.tr("loading"), "loading", this.observer, this);

    Common.loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            return ParseData.parsePlaylist(curPage, perPage, "hot,new", genKey);
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(Vector listItems) {
            PlaylistList playlistList = new PlaylistList(title, listItems, "genre", "", "playlist");
            playlistList.setObserver(observer);
            observer.replaceCurrent(playlistList);
          }

          public void loadError() {
            MainList.displayMessage(
                title, I18N.tr("connection_error"), "error", observer, CategorySubList.this);
          }

          public void noData() {
            MainList.displayMessage(
                title, I18N.tr("no_data"), "error", observer, CategorySubList.this);
          }
        },
        this.mLoadDataThread);
  }
}
