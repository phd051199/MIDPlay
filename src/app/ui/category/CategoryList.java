package app.ui.category;

import app.common.ParseData;
import app.interfaces.LoadDataObserver;
import app.model.Category;
import app.ui.MainList;
import app.ui.PlaylistList;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class CategoryList extends List implements CommandListener, LoadDataObserver {

  private Command exitCommand;
  private Command selectCommand;
  private Command nowPlayingCommand;
  private Command searchCommand;

  Vector images;
  public int selectedItem = 0;
  Vector cateItems;
  private Utils.BreadCrumbTrail observer;
  Thread mLoaDataThread;
  private Image defaultImage;

  public CategoryList(String title, Vector items) {
    super(title, List.IMPLICIT);

    this.loadDefaultImage();
    this.initCommands();
    this.cateItems = new Vector();
    this.images = new Vector();
    this.cateItems = items;
    this.initComponents();
    this.setCommandListener(this);
  }

  private void initCommands() {
    this.selectCommand = new Command(I18N.tr("select"), Command.OK, 1);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(I18N.tr("search"), Command.SCREEN, 3);
    this.addCommand(this.selectCommand);
    this.addCommand(this.exitCommand);
    this.addCommand(this.searchCommand);
    this.addCommand(this.nowPlayingCommand);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == this.selectCommand || c == List.SELECT_COMMAND) {
      this.selectedItem = this.getSelectedIndex();
      if (this.selectedItem >= 0 && this.selectedItem < this.cateItems.size()) {
        Category cate = (Category) this.cateItems.elementAt(this.selectedItem);
        if (cate.getSubItems() != null && cate.getSubItems().size() > 0) {
          CategorySubList subList = new CategorySubList(cate.getName(), cate.getSubItems());
          subList.setObserver(this.observer);
          this.observer.go(subList);
        } else {
          this.gotoPlaylistByCate(cate.getId(), 1, 10);
        }
      }
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      MainList.gotoSearch(this.observer);
    }
  }

  private void initComponents() {
    this.createImages();
    this.deleteAll();
    for (int i = 0; i < this.cateItems.size(); ++i) {
      Category cate = (Category) this.cateItems.elementAt(i);
      Image imagePart = this.getImage(i);
      this.append(cate.getName(), imagePart);
    }
    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/Album.png");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void createImages() {
    try {
      this.images.removeAllElements();
      for (int i = 0; i < this.cateItems.size(); ++i) {
        this.images.addElement(this.defaultImage);
      }
    } catch (Exception var3) {
      var3.printStackTrace();
    }
  }

  public Image getImage(int index) {
    return (Image)
        (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
  }

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  private void gotoPlaylistByCate(final String genKey, final int curPage, final int perPage) {
    this.displayMessage(I18N.tr("loading"), "loading");
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector listItems = ParseData.parsePlaylist(curPage, perPage, "hot,new", genKey);
                if (listItems == null) {
                  CategoryList.this.displayMessage(I18N.tr("connection_error"), "error");
                } else if (listItems.size() == 0) {
                  CategoryList.this.displayMessage(I18N.tr("no_data"), "error");
                } else {
                  PlaylistList playlistList =
                      new PlaylistList(I18N.tr("genres"), listItems, "genre", "", "playlist");

                  playlistList.setObserver(CategoryList.this.observer);
                  CategoryList.this.observer.replaceCurrent(playlistList);
                }
              }
            });
    this.mLoaDataThread.start();
  }

  private void displayMessage(String message, String messageType) {
    MainList.displayMessage(I18N.tr("genres"), message, messageType, this.observer, this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoaDataThread != null && this.mLoaDataThread.isAlive()) {
        this.mLoaDataThread.join();
      }
    } catch (InterruptedException var2) {
      var2.printStackTrace();
    }
  }
}
