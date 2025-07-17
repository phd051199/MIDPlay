package app.ui.category;

import app.core.data.DataLoader;
import app.core.data.DataParser;
import app.core.data.LoadDataListener;
import app.core.data.LoadDataObserver;
import app.core.threading.ThreadManagerIntegration;
import app.models.Category;
import app.ui.MainList;
import app.ui.MainObserver;
import app.ui.PlaylistList;
import app.utils.I18N;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class CategoryList extends List implements CommandListener, LoadDataObserver {

  private Command exitCommand;
  private Command nowPlayingCommand;
  private Command searchCommand;

  Image[] images;
  public int selectedItem = 0;
  Vector cateItems;
  private MainObserver observer;
  private Image defaultImage;

  public CategoryList(String title, Vector items) {
    super(title, List.IMPLICIT);

    this.loadDefaultImage();
    this.initCommands();
    this.cateItems = new Vector();
    this.cateItems = items;
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

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == List.SELECT_COMMAND) {
      this.selectedItem = this.getSelectedIndex();
      if (this.selectedItem >= 0 && this.selectedItem < this.cateItems.size()) {
        Category category = (Category) this.cateItems.elementAt(this.selectedItem);
        if (category.getSubItems() != null && category.getSubItems().size() > 0) {
          CategorySubList subList = new CategorySubList(category.getName(), category.getSubItems());
          subList.setObserver(this.observer);
          this.observer.go(subList);
        } else {
          this.gotoPlaylistByCategory(category.getId(), 1, 10);
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
      Category category = (Category) this.cateItems.elementAt(i);
      Image imagePart = this.getImage(i);
      this.append(category.getName(), imagePart);
    }
    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/Album.png");
    } catch (Exception e) {
    }
  }

  private void createImages() {
    try {
      int size = this.cateItems.size();
      this.images = new Image[size];
      for (int i = 0; i < size; ++i) {
        this.images[i] = this.defaultImage;
      }
    } catch (Exception e) {
    }
  }

  public Image getImage(int index) {
    return (this.images != null && this.images.length > index && index >= 0)
        ? this.images[index]
        : null;
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }

  private void gotoPlaylistByCategory(final String genKey, final int curPage, final int perPage) {
    this.displayMessage(I18N.tr("loading"), "loading");
    ThreadManagerIntegration.loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            return DataParser.parsePlaylist(curPage, perPage, "hot,new", genKey);
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(Vector listItems) {
            PlaylistList playlistList =
                new PlaylistList(I18N.tr("genres"), listItems, "genre", "", "playlist");
            playlistList.setObserver(CategoryList.this.observer);
            CategoryList.this.observer.replaceCurrent(playlistList);
          }

          public void loadError() {
            CategoryList.this.displayMessage(I18N.tr("connection_error"), "error");
          }

          public void noData() {
            CategoryList.this.displayMessage(I18N.tr("no_data"), "error");
          }
        });
  }

  private void displayMessage(String message, String messageType) {
    MainList.displayMessage(I18N.tr("genres"), message, messageType, this.observer, this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {}
}
