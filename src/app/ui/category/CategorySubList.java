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

public class CategorySubList extends List implements CommandListener, LoadDataObserver {

  private Command exitCommand;
  private Command selectCommand;
  private Command nowPlayingCommand;
  private Command searchCommand;

  private Vector subItems;
  private Vector images;
  private Utils.BreadCrumbTrail observer;

  public CategorySubList(String title, Vector subs) {
    super(title, List.IMPLICIT);
    this.subItems = subs;
    this.images = new Vector();
    this.initCommands();
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

  private void initComponents() {
    try {
      this.images.removeAllElements();
      this.deleteAll();
      for (int i = 0; i < this.subItems.size(); ++i) {
        Category cate = (Category) this.subItems.elementAt(i);
        Image folderIcon = Image.createImage("/images/FolderSound.png");
        this.images.addElement(folderIcon);
        this.append(cate.getName(), folderIcon);
      }
      if (this.subItems.size() > 0) this.setSelectedIndex(0, true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == this.selectCommand || c == List.SELECT_COMMAND) {
      int selected = this.getSelectedIndex();
      if (selected >= 0 && selected < this.subItems.size()) {
        Category cate = (Category) this.subItems.elementAt(selected);
        this.gotoPlaylistByCate(cate.getId(), 1, 10);
      }
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      MainList.gotoSearch(this.observer);
    }
  }

  public void cancel() {}

  public void quit() {}

  private void gotoPlaylistByCate(final String genKey, final int curPage, final int perPage) {
    MainList.displayMessage(I18N.tr("genres"), I18N.tr("loading"), "loading", this.observer, this);

    Thread loader =
        new Thread(
            new Runnable() {
              public void run() {
                Vector listItems = ParseData.parseResolvePlaylist(genKey, curPage, perPage);
                if (listItems == null) {
                  MainList.displayMessage(
                      I18N.tr("genres"),
                      I18N.tr("connection_error"),
                      "error",
                      observer,
                      CategorySubList.this);
                } else if (listItems.size() == 0) {
                  MainList.displayMessage(
                      I18N.tr("genres"),
                      I18N.tr("no_data"),
                      "error",
                      observer,
                      CategorySubList.this);
                } else {
                  PlaylistList playlistList =
                      new PlaylistList(I18N.tr("genres"), listItems, "genre", "");
                  playlistList.setObserver(observer);
                  observer.replaceCurrent(playlistList);
                }
              }
            });
    loader.start();
  }
}
