package app;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import app.common.ParseData;
import app.model.Playlist;
import app.utils.Utils;

public class PlaylistList extends List implements CommandListener, LoadDataObserver {

    private Command exitCommand;
    private Command selectCommand;
    private Command nowPlayingCommand;
    private Command searchCommand;

    private Vector images;
    private Vector playlistItems;
    private Utils.BreadCrumbTrail observer;
    int curPage = 1;
    int perPage = 10;
    String from = "";
    String keyWord = "";
    Thread mLoaDataThread;

    public PlaylistList(String title, Vector items, String _from, String keySearh) {
        super(title, List.IMPLICIT);

        this.from = _from;
        this.keyWord = keySearh;
        this.curPage = 1;
        this.perPage = 10;
        this.initCommands();
        this.playlistItems = new Vector();
        this.images = new Vector();
        this.playlistItems = items;
        this.initComponents();
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
        this.setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.exitCommand) {
            this.observer.goBack();
        } else if (c == this.selectCommand || c == List.SELECT_COMMAND) {
            int selectedItemIndex = getSelectedIndex();
            if (selectedItemIndex >= 0 && selectedItemIndex < this.playlistItems.size()) {
                Playlist playlist = (Playlist) this.playlistItems.elementAt(selectedItemIndex);
                if (playlist.getName().equals("Xem thêm ...")) {
                    this.doLoadMoreAction(playlist);
                } else {
                    this.gotoSongByPlaylist();
                }
            }
        } else if (c == this.nowPlayingCommand) {
            MainList.gotoNowPlaying(this.observer);
        } else if (c == this.searchCommand) {
            MainList.gotoSearch(this.observer);
        }
    }

    private void loadMorePlaylists(final String keyword, final String genKey, final int curPage, final int perPage) {
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector listItems = null;
                if (PlaylistList.this.from.equals("genre")) {
                    listItems = ParseData.parseSearch(genKey, "", curPage, perPage);
                } else if (PlaylistList.this.from.equals("search")) {
                    listItems = ParseData.parseSearch("", keyword, curPage, perPage);
                } else if (PlaylistList.this.from.equals("hot")) {
                    listItems = ParseData.parseHotPlaylist(curPage, perPage);
                } else if (PlaylistList.this.from.equals("new")) {
                    listItems = ParseData.parseNewPlaylist(curPage, perPage);
                }

                if (listItems != null) {
                    PlaylistList.this.addMorePlaylists(listItems);
                    PlaylistList.this.repaintList();
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void initComponents() {
        this.createImages();
        this.deleteAll();
        for (int i = 0; i < this.playlistItems.size(); ++i) {
            Playlist playlist = (Playlist) this.playlistItems.elementAt(i);
            Image imagePart = this.getImage(i);
            this.append(playlist.getName(), imagePart);
        }
        this.setSelectedIndex(0, true);
    }

    private void repaintList() {
        int currentIndex = this.getSelectedIndex();
        this.deleteAll();
        for (int i = 0; i < this.playlistItems.size(); ++i) {
            Playlist playlist = (Playlist) this.playlistItems.elementAt(i);
            Image imagePart = this.getImage(i);
            this.append(playlist.getName(), imagePart);
        }
        if (currentIndex >= 0 && currentIndex < this.playlistItems.size()) {
            this.setSelectedIndex(currentIndex, true);
        } else if (this.playlistItems.size() > 0) {
            this.setSelectedIndex(0, true);
        }

    }

    private void createImages() {
        try {
            this.images.removeAllElements();
            for (int i = 0; i < this.playlistItems.size(); ++i) {
                Image image = Image.createImage("/images/FolderSound.png");
                this.images.addElement(image);
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    private void addMorePlaylists(Vector playlists) {
        for (int i = 0; i < playlists.size(); ++i) {
            this.playlistItems.addElement(playlists.elementAt(i));
            try {
                Image image = Image.createImage("/images/FolderSound.png");
                this.images.addElement(image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void doLoadMoreAction(Playlist playlist) {
        this.curPage++;
        if (this.playlistItems.size() > 0) {
            Playlist lastPlaylist = (Playlist) this.playlistItems.elementAt(this.playlistItems.size() - 1);
            if (lastPlaylist.getName().equals("Xem thêm ...")) {
                this.playlistItems.removeElementAt(this.playlistItems.size() - 1);
            }
        }

        this.loadMorePlaylists(this.keyWord, playlist.getId(), this.curPage, this.perPage);
    }

    public void setObserver(Utils.BreadCrumbTrail _observer) {
        this.observer = _observer;
    }

    private void gotoSongByPlaylist() {
        final Playlist playlist = (Playlist) this.playlistItems.elementAt(this.getSelectedIndex());
        this.displayMessage(playlist.getName(), "Đang tải dữ liệu...", "loading");
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector songItems = ParseData.parseSongsInPlaylist(playlist.getId(), "", 1, 10);
                if (songItems == null) {
                    PlaylistList.this.displayMessage(playlist.getName(),
                            "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error");
                } else if (songItems.size() == 0) {
                    PlaylistList.this.displayMessage(playlist.getName(), "Chưa có dữ liệu!", "error");
                } else {
                    SongList songList = new SongList(playlist.getName(), songItems, playlist);
                    songList.setObserver(PlaylistList.this.observer);
                    PlaylistList.this.observer.replaceCurrent(songList);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void displayMessage(String title, String message, String messageType) {
        MainList.displayMessage(title, message, messageType, this.observer, this);
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




    public Image getImage(int index) {
        return (Image) (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
    }
}
