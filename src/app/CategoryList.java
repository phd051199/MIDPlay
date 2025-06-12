package app;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import app.common.ParseData;
import app.model.Category;
import app.utils.Utils;

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

    public CategoryList(String title, Vector items) {
        super(title, List.IMPLICIT);

        this.initCommands();
        this.cateItems = new Vector();
        this.images = new Vector();
        this.cateItems = items;

        this.initComponents();
        this.setCommandListener(this);
    }

    private void initCommands() {
        this.selectCommand = new Command("Chọn", Command.OK, 1);
        this.nowPlayingCommand = new Command("Đang chơi", Command.SCREEN, 2);
        this.exitCommand = new Command("Trở lại", Command.BACK, 0);
        this.searchCommand = new Command("Tìm kiếm", Command.SCREEN, 3);
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
                this.gotoPlaylistByCate(cate.getId(), 1, 10);
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
        this.setSelectedIndex(0, true);
    }

    private void createImages() {
        try {
            this.images.removeAllElements();
            for (int i = 0; i < this.cateItems.size(); ++i) {
                Image image = Image.createImage("/images/Album.png");
                this.images.addElement(image);
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    public Image getImage(int index) {
        return (Image) (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
    }

    public void setObserver(Utils.BreadCrumbTrail _observer) {
        this.observer = _observer;
    }

    private void gotoPlaylistByCate(final String genKey, final int curPage, final int perPage) {
        this.displayMessage("Đang tải dữ liệu...", "loading");
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector listItems = ParseData.parseSearch(genKey, "", curPage, perPage);
                if (listItems == null) {
                    CategoryList.this
                            .displayMessage("Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error");
                } else if (listItems.size() == 0) {
                    CategoryList.this.displayMessage("Chưa có dữ liệu!", "error");
                } else {
                    PlaylistList playlistList = new PlaylistList("Thể loại", listItems, "genre", "");
                   
                    playlistList.setObserver(CategoryList.this.observer);
                    CategoryList.this.observer.replaceCurrent(playlistList);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void displayMessage(String message, String messageType) {
        MainList.displayMessage("Thể loại", message, messageType, this.observer, this);
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
