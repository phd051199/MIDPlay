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
import musicapp.model.Category;
import musicapp.utils.Utils;

public class CategoryCanvasList extends Canvas implements CommandListener, LoadDataObserver {

    private Command exitCommand;
    private Command selectCommand;
    private Command nowPlayingCommand;
    private Command searchCommand;
    int linePadding = 0;
    int margin = 2;
    int padding = 0;
    Font font = Font.getDefaultFont();
    int bgColor = 15658734;
    int foreColor = 4297147;
    int foreSelectedColor = 15658734;
    int backColor = 15658734;
    int backSelectedColor = 5263440;
    int borderWidth = 0;
    int borderColor = 0;
    int borderSelectedColor = 16711680;
    String[][] itemLines = (String[][]) null;
    Vector images;
    public int selectedItem = 0;
    int[] itemsTop = null;
    int[] itemsHeight = null;
    int scrollTop = 0;
    final int SCROLL_STEP = 60;
    Vector cateItems;
    private Utils.BreadCrumbTrail observer;
    Thread mLoaDataThread;

    public CategoryCanvasList(String title, Vector items) {
        this.setTitle(title);
        this.initMenu();
        this.cateItems = new Vector();
        this.images = new Vector();
        this.cateItems = items;
        this.initComponents();
    }

    protected void paint(Graphics g) {
        g.setColor(this.bgColor);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.translate(0, -this.scrollTop);
        int top = 0;
        this.font = Font.getFont(0, 1, 16);
        g.setFont(this.font);
        if (this.itemLines != null) {
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
                g.fillRoundRect(this.margin + this.borderWidth, this.margin + this.borderWidth, this.getWidth() - 2 * this.margin - 2 * this.borderWidth, itemHeight - 2 * this.borderWidth, 5, 10);
                if (imagePart != null) {
                    g.drawImage(imagePart, this.margin + this.borderWidth + this.padding, this.margin + this.borderWidth + this.padding, 20);
                }

                g.setColor(i == this.selectedItem ? this.foreSelectedColor : this.foreColor);
                int textLeft = this.margin + this.borderWidth + this.padding + (imagePart != null ? imagePart.getWidth() + this.padding : 0);

                for (int j = 0; j < itemRows; ++j) {
                    g.drawString(this.itemLines[i][j], textLeft + 5, this.margin + this.borderWidth + this.padding + j * (this.linePadding + this.font.getHeight()), 20);
                }

                g.translate(0, -top);
                top += itemHeight + 2 * this.margin;
            }

            g.translate(0, this.scrollTop);
        }
    }

    protected void keyPressed(int key) {
        int keyCode = this.getGameAction(key);
        if (this.itemLines.length > 0) {
            if (keyCode == 1) {
                if (this.itemsTop[this.selectedItem] < this.scrollTop) {
                    this.scrollTop -= 60;
                    this.repaint();
                } else if (this.selectedItem > 0) {
                    --this.selectedItem;
                    this.repaint();
                }
            } else if (keyCode == 6) {
                if (this.itemsTop[this.selectedItem] + this.itemsHeight[this.selectedItem] >= this.scrollTop + this.getHeight()) {
                    this.scrollTop += 60;
                    this.repaint();
                } else if (this.selectedItem < this.itemLines.length - 1) {
                    ++this.selectedItem;
                    this.repaint();
                }
            } else if (keyCode == 8) {
                Category cate = (Category) this.cateItems.elementAt(this.selectedItem);
                this.gotoPlaylistByCate(cate.getId(), 1, 10);
            }
        }

        super.keyPressed(key);
    }

    public int getItemWidth() {
        return this.getWidth() - 2 * this.borderWidth - 2 * this.padding - 2 * this.margin;
    }

    Image getImage(int index) {
        return (Image) ((Image) (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null));
    }

    static String[] getTextRows(String text, Font font, int width) {
        char SPACE_CHAR = ' ';
        String VOID_STRING = "";
        int prevIndex = 0;
        int currIndex = text.indexOf(SPACE_CHAR);
        Vector rowsVector = new Vector();
        StringBuffer stringBuffer = new StringBuffer();
        String currentRowText = VOID_STRING;

        while (prevIndex != -1) {
            int startCharIndex = prevIndex == 0 ? prevIndex : prevIndex + 1;
            String currentToken;
            if (currIndex != -1) {
                currentToken = text.substring(startCharIndex, currIndex);
            } else {
                currentToken = text.substring(startCharIndex);
            }

            prevIndex = currIndex;
            currIndex = text.indexOf(SPACE_CHAR, currIndex + 1);
            if (currentToken.length() != 0) {
                if (stringBuffer.length() > 0) {
                    stringBuffer.append(SPACE_CHAR);
                }

                stringBuffer.append(currentToken);
                if (font.stringWidth(stringBuffer.toString()) > width) {
                    if (currentRowText.length() > 0) {
                        rowsVector.addElement(currentRowText);
                    }

                    stringBuffer.setLength(0);
                    stringBuffer.append(currentToken);
                    currentRowText = stringBuffer.toString();
                } else {
                    currentRowText = stringBuffer.toString();
                }
            }
        }

        if (currentRowText.length() > 0) {
            rowsVector.addElement(currentRowText);
        }

        String[] rowsArray = new String[rowsVector.size()];
        rowsVector.copyInto(rowsArray);
        return rowsArray;
    }

    protected void paintTitleBarBackground(Graphics g, int x, int y) {
        try {
            Image image = Image.createImage("/images/title.png");
            g.drawImage(image, x, y, 20);
            int ofset = (y + image.getHeight()) / 4;
            this.font = Font.getFont(0, 1, 16);
            g.setFont(this.font);
            g.drawString("Thể loại", (x + image.getWidth()) / 4, ofset, 20);
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.exitCommand) {
            this.observer.goBack();
        } else if (c == this.selectCommand) {
            Category cate = (Category) this.cateItems.elementAt(this.selectedItem);
            this.gotoPlaylistByCate(cate.getId(), 1, 10);
        } else if (c == this.nowPlayingCommand) {
            MainList.gotoNowPlaying(this.observer);
        } else if (c == this.searchCommand) {
            MainList.gotoSearch(this.observer);
        }

    }

    private void initComponents() {
        this.createImages();
        this.itemLines = new String[this.cateItems.size()][];
        this.itemsTop = new int[this.itemLines.length];
        this.itemsHeight = new int[this.itemLines.length];

        for (int i = 0; i < this.itemLines.length; ++i) {
            Image imagePart = this.getImage(i);
            int w = this.getItemWidth() - (imagePart != null ? imagePart.getWidth() + this.padding : 0);
            Category cate = (Category) this.cateItems.elementAt(i);
            this.itemLines[i] = getTextRows(cate.getName(), this.font, w);
        }

    }

    private void createImages() {
        try {
            for (int i = 0; i < this.cateItems.size(); ++i) {
                Image image = Image.createImage("/images/icon_theloai.png");
                this.images.addElement(image);
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }

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
                    CategoryCanvasList.this.displayMessage("Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.", "error");
                } else if (listItems.size() == 0) {
                    CategoryCanvasList.this.displayMessage("Chưa có dữ liệu!", "error");
                } else {
                    PlaylistList cateCanvas = new PlaylistList("Thể loại", listItems, "genre", "");
                    cateCanvas.setObserver(CategoryCanvasList.this.observer);
                    CategoryCanvasList.this.observer.replaceCurrent(cateCanvas);
                }
            }
        });
        this.mLoaDataThread.start();
    }

    private void displayMessage(String message, String messageType) {
        MainList.displayMessage("Thể loại", message, messageType, this.observer, this);
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
