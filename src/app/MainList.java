package app;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import app.common.ParseData;
import app.utils.Utils;

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
                    text += "\nTìm kiếm playlist";
                    break;
                case 1:
                    text += "\nCác thể loại nhạc";
                    break;
                case 2:
                    text += "\nCập nhật mới nhất";
                    break;
                case 3:
                    text += "\nNgôn ngữ, chất lượng âm thanh";
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
        this.selectCommand = new Command("Chọn", Command.OK, 0);
        this.nowPlayingCommand = new Command("Đang chơi", Command.SCREEN, 1);
        this.exitCommand = new Command("Thoát", Command.EXIT, 2);

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
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector cateItems = ParseData.parseCate(2);
                if (cateItems == null) {
                    MainList.displayMessage("MIDPlay",
                            "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error",
                            MainList.this.observer, MainList.this);
                } else if (cateItems.size() == 0) {
                    MainList.displayMessage("MIDPlay", "Chưa có dữ liệu!", "error", MainList.this.observer,
                            MainList.this);
                } else {
                    CategoryList cateCanvas = new CategoryList("Thể loại", cateItems);
                    cateCanvas.setObserver(MainList.this.observer);
                    MainList.this.observer.replaceCurrent(cateCanvas);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void gotoHotPlaylist() {
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector playlistItems = ParseData.parseHotPlaylist(1, 10);
                if (playlistItems == null) {
                    MainList.displayMessage("MIDPlay",
                            "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error",
                            MainList.this.observer, MainList.this);
                } else if (playlistItems.size() == 0) {
                    MainList.displayMessage("MIDPlay", "Chưa có dữ liệu!", "error", MainList.this.observer,
                            MainList.this);
                } else {
                    PlaylistList playlistList = new PlaylistList("Playlist Hot", playlistItems, "hot", "");
                    playlistList.setObserver(MainList.this.observer);
                    MainList.this.observer.replaceCurrent(playlistList);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void gotoSetting(Utils.BreadCrumbTrail observer) {
        SettingForm settingForm = new SettingForm("Cài đặt", observer);
        observer.go(settingForm);
    }

    public static void gotoSearch(Utils.BreadCrumbTrail observer) {
        SearchForm searchForm = new SearchForm("Tìm kiếm");
        searchForm.setObserver(observer);
        observer.go(searchForm);
    }

    public static void gotoAbout(Utils.BreadCrumbTrail observer) {
        AboutForm aboutForm = new AboutForm("Thông tin", observer);
        observer.go(aboutForm);
    }

    public static void gotoHelp(Utils.BreadCrumbTrail observer) {
        HelpForm helpForm = new HelpForm("Privacy Policy", observer);
        observer.go(helpForm);
    }

    private void itemAction() {
        int selectedItem = this.getSelectedIndex();
        switch (selectedItem) {
            case 0:
                gotoSearch(this.observer);
                break;
            case 1:
                displayMessage("MIDPlay", "Đang tải dữ liệu...", "loading", this.observer, this);
                this.gotoCate();
                break;
            case 2:
                displayMessage("MIDPlay", "Đang tải dữ liệu...", "loading", this.observer, this);
                this.gotoHotPlaylist();
                break;
            case 3:
                gotoSetting(this.observer);
                break;
            case 4:
                gotoAbout(this.observer);
                break;
            case 5:
                gotoHelp(this.observer);
                break;
            default:
                break;
        }
    }

    public static void displayMessage(String title, String message, String messageType, Utils.BreadCrumbTrail observer,
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
