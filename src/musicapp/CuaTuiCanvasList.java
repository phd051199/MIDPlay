package musicapp;

import java.io.IOException;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import musicapp.common.ParseData;
import musicapp.model.Playlist;
import musicapp.utils.Utils;

public final class CuaTuiCanvasList extends Canvas implements CommandListener, LoadDataObserver {

    private Command nowPlayingCommand;
    private Command selectCommand;
    private Command exitCommand;
    private Command searchCommand;
    private Utils.BreadCrumbTrail observer;
    int linePadding = 0;
    int margin = 2;
    int padding = 0;
    Font font = Font.getDefaultFont();
    int bgColor = 15658734;
    int foreColor = 0;
    int foreSelectedColor = 15658734;
    int backColor = 15658734;
    int backSelectedColor = 5263440;
    int borderWidth = 0;
    int borderColor = 0;
    int borderSelectedColor = 16711680;
    String[][] itemLines = (String[][]) null;
    Image[] images = null;
    public int selectedItem = 0;
    int[] itemsTop = null;
    int[] itemsHeight = null;
    int scrollTop = 0;
    final int SCROLL_STEP = 20;
    private String userName;
    private String defaultlistkey;
    private Playlist defaultPlaylist;
    Thread mLoaDataThread;

    public CuaTuiCanvasList(String title, String[] items, Image[] imageElements, String _defaultlistkey) {
        this.setTitle(title);
        this.userName = title;
        this.defaultlistkey = _defaultlistkey;
        this.initMenu();
        this.images = imageElements;
        this.itemLines = new String[items.length][];
        this.itemsTop = new int[this.itemLines.length];
        this.itemsHeight = new int[this.itemLines.length];

      // for(int i = 0; i < this.itemLines.length; ++i) {
        // Image imagePart = this.getImage(i);
        // int w = this.getItemWidth() - (imagePart != null ? imagePart.getWidth() +
        // this.padding : 0);
        // this.itemLines[i] = MainCanvasList.getTextRows(items[i], this.font, w);
        // }
    }

    protected void paint(Graphics g) {
        g.setColor(this.bgColor);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.translate(0, -this.scrollTop);
        int top = 0;
        this.font = Font.getFont(0, 1, 16);
        g.setFont(this.font);

        for (int i = 0; i < this.itemLines.length; ++i) {
            int itemRows = this.itemLines[i].length;
            Image imagePart = this.getImage(i);
            int itemHeight = itemRows * this.font.getHeight() + this.linePadding * (itemRows - 1);
            this.itemsTop[i] = top;
            this.itemsHeight[i] = itemHeight;
            if (imagePart != null && imagePart.getHeight() > itemHeight) {
                itemHeight = imagePart.getHeight();
            }

            itemHeight += 2 * this.padding + 2 * this.borderWidth;
            g.translate(0, top);
            if (this.borderWidth > 0) {
                g.setColor(i == this.selectedItem ? this.borderSelectedColor : this.borderColor);
                g.fillRect(this.margin, this.margin, this.getWidth() - 2 * this.margin, itemHeight);
            }

            g.setColor(i == this.selectedItem ? this.backSelectedColor : this.backColor);
            g.fillRoundRect(this.margin + this.borderWidth, this.margin + this.borderWidth,
                    this.getWidth() - 2 * this.margin - 2 * this.borderWidth, itemHeight - 2 * this.borderWidth, 5, 10);
            if (imagePart != null) {
                g.drawImage(imagePart, this.margin + this.borderWidth + this.padding,
                        this.margin + this.borderWidth + this.padding, 20);
            }

            g.setColor(i == this.selectedItem ? this.foreSelectedColor : this.foreColor);
            int textLeft = this.margin + this.borderWidth + this.padding
                    + (imagePart != null ? imagePart.getWidth() + this.padding : 0);

            for (int j = 0; j < itemRows; ++j) {
                g.drawString(this.itemLines[i][j], textLeft + 5,
                        this.margin + this.borderWidth + this.padding + j * (this.linePadding + this.font.getHeight()), 20);
            }

            g.translate(0, -top);
            top += itemHeight + 2 * this.margin;
        }

        g.translate(0, this.scrollTop);
    }

    protected void keyPressed(int key) {
        int keyCode = this.getGameAction(key);
        if (this.itemLines.length > 0) {
            if (keyCode == 1) {
                if (this.itemsTop[this.selectedItem] < this.scrollTop) {
                    this.scrollTop -= 20;
                    this.repaint();
                } else if (this.selectedItem > 0) {
                    --this.selectedItem;
                    this.repaint();
                }
            } else if (keyCode == 6) {
                if (this.itemsTop[this.selectedItem] + this.itemsHeight[this.selectedItem] >= this.scrollTop
                        + this.getHeight()) {
                    this.scrollTop += 20;
                    this.repaint();
                } else if (this.selectedItem < this.itemLines.length - 1) {
                    ++this.selectedItem;
                    this.repaint();
                }
            } else if (keyCode == 8 || keyCode == -5) {
                this.itemAction();
            }
        }

        super.keyPressed(key);
    }

    public int getItemWidth() {
        return this.getWidth() - 2 * this.borderWidth - 2 * this.padding - 2 * this.margin;
    }

    Image getImage(int index) {
        return this.images != null && this.images.length > index ? this.images[index] : null;
    }

    protected void paintTitleBarBackground(Graphics g, int x, int y) {
        try {
            Image image = Image.createImage("/images/title.png");
            g.drawImage(image, x, y, 20);
            int ofset = (y + image.getHeight()) / 4;
            this.font = Font.getFont(0, 1, 16);
            g.setFont(this.font);
            g.drawString("NhacCuaTui", (x + image.getWidth()) / 4, ofset, 20);
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.selectCommand) {
            this.itemAction();
        } else if (c == this.exitCommand) {
            this.observer.goBack();
        } else if (c == this.nowPlayingCommand) {
            MainList.gotoNowPlaying(this.observer);
        } else if (c == this.searchCommand) {
            MainList.gotoSearch(this.observer);
        }

    }

    public void setObserver(Utils.BreadCrumbTrail mObserver) {
        this.observer = mObserver;
    }

    private void gotoPlaylistByAction(final String action, final String userName) {
        this.displayMessage("NhacCuaTui", "Đang tải dữ liệu...", "loading");
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector listItems = ParseData.parseUserPlaylistsByAction(action, userName, 1, 10);
                if (listItems == null) {
                    CuaTuiCanvasList.this.displayMessage("NhacCuaTui",
                            "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error");
                } else if (listItems.isEmpty()) {
                    CuaTuiCanvasList.this.displayMessage("NhacCuaTui", "Chưa có dữ liệu!", "error");
                } else {
                    PlaylistList cateCanvas = new PlaylistList("Playlist", listItems, "genre", "");
                    cateCanvas.setObserver(CuaTuiCanvasList.this.observer);
                    CuaTuiCanvasList.this.observer.replaceCurrent(cateCanvas);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void gotoPlayDefaultPlaylist() {
        this.displayMessage("NhacCuaTui", "Đang tải dữ liệu...", "loading");
        this.mLoaDataThread = new Thread(new Runnable() {
            public void run() {
                Vector songItems = ParseData.parseSongsInPlaylist(CuaTuiCanvasList.this.defaultlistkey,
                        CuaTuiCanvasList.this.userName, 1, 10);
                if (songItems == null) {
                    CuaTuiCanvasList.this.displayMessage("NhacCuaTui",
                            "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error");
                } else if (songItems.isEmpty()) {
                    CuaTuiCanvasList.this.displayMessage("NhacCuaTui", "Chưa có dữ liệu!", "error");
                } else {
                    CuaTuiCanvasList.this.gotoPlaySong(songItems);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void itemAction() {
        if (this.selectedItem == 0) {

        } else if (this.selectedItem == 1) {
            this.gotoPlaylistByAction("myPlaylist", this.userName);
        } else if (this.selectedItem == 2) {
            this.gotoPlaylistByAction("playlistLiked", this.userName);
        }

    }

    private void gotoPlaySong(Vector songItems) {
        if (SongList.playerCanvas == null) {
            SongList.playerCanvas = new PlayerCanvas("NhacCuaTui", songItems, this.selectedItem,
                    this.defaultPlaylist);
        } else {
            SongList.playerCanvas.change("NhacCuaTui", songItems, this.selectedItem, this.defaultPlaylist);
        }

        SongList.playerCanvas.setObserver(this.observer);
        this.observer.replaceCurrent(SongList.playerCanvas);
    }

    private void initMenu() {
        this.selectCommand = new Command("Chọn", 8, 1);
        this.nowPlayingCommand = new Command("Đang chơi...", 8, 1);
        this.exitCommand = new Command("Trở lại", 7, 0);
        this.searchCommand = new Command("Tìm kiếm", 8, 1);
        this.addCommand(this.selectCommand);
        this.addCommand(this.exitCommand);
        this.addCommand(this.searchCommand);
        this.addCommand(this.nowPlayingCommand);
        this.setCommandListener(this);
    }

    private void displayMessage(String title, String message, String messageType) {
        MainList.displayMessage(title, message, messageType, this.observer, this);
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
