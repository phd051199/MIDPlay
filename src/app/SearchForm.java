package app;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import app.common.ParseData;
import app.utils.Utils;

public class SearchForm extends Form implements CommandListener, LoadDataObserver {

    private TextField symbolField = new TextField("Nhập từ khoá cần tìm", "", 300, 0);
    private Command exitCommand;
    private Command searchCommand;
    private Command nowPlayingCommand;
    private String keyWord = "";
    private Utils.BreadCrumbTrail observer;
    Thread mLoaDataThread;

    public SearchForm(String title) {
        super(title);
        this.append(this.symbolField);
        this.initMenu();
    }

    public TextField getSymbolField() {
        return this.symbolField;
    }

    public Command getExitCommand() {
        return this.exitCommand;
    }

    public Command getGetCommand() {
        return this.searchCommand;
    }

    public void setObserver(Utils.BreadCrumbTrail _observer) {
        this.observer = _observer;
    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.exitCommand) {
            this.observer.goBack();
        } else if (c == this.searchCommand) {
            this.keyWord = this.symbolField.getString();
            this.gotoSearchPlaylist(this.keyWord, 1, 10);
        } else if (c == this.nowPlayingCommand) {
            MainList.gotoNowPlaying(this.observer);
        }

    }

    private void gotoSearchPlaylist(String keyword, final int curPage, final int perPage) {
        this.displayMessage("Từ khoá: " + keyword, "Đang tải dữ liệu...", "loading");
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector listItems = ParseData.parseSearch("", SearchForm.this.keyWord,
                        curPage, perPage);
                if (listItems == null) {
                    SearchForm.this.displayMessage("", "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error");
                } else if (listItems.isEmpty()) {
                    SearchForm.this.displayMessage("", "Không tìm thấy kết quả nào với từ khoá tìm kiếm trên!", "error");
                } else {
                    PlaylistList cateCanvas = new PlaylistList("Kết quả tìm kiếm: "
                            + SearchForm.this.keyWord, listItems, "search", SearchForm.this.keyWord);
                    cateCanvas.setObserver(SearchForm.this.observer);
                    SearchForm.this.observer.replaceCurrent(cateCanvas);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void displayMessage(String title, String message, String messageType) {
        MainList.displayMessage(title, message, messageType, this.observer, this);
    }

    private void initMenu() {
        this.searchCommand = new Command("Tìm", 8, 0);
        this.nowPlayingCommand = new Command("Đang chơi...", 8, 1);
        this.exitCommand = new Command("Trở lại", 7, 0);
        this.addCommand(this.searchCommand);
        this.addCommand(this.exitCommand);
        this.addCommand(this.nowPlayingCommand);
        this.setCommandListener(this);
    }

    public void cancel() {
        this.quit();
    }

    public void quit() {
        try {
            if (this.mLoaDataThread.isAlive()) {
                this.mLoaDataThread.join();
            }
        } catch (InterruptedException var2) {
            var2.printStackTrace();
        }

    }
}
