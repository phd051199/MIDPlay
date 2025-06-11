package app;

import java.util.Stack;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import app.common.Observer;
import app.model.Song;
import app.utils.Utils;

public class MIDPlay extends MIDlet implements CommandListener, Utils.BreadCrumbTrail {

    private final Stack history = new Stack();
    private Displayable currDisplayable;
    private Form mainForm;
    Command okCommand = new Command("Có", 4, 0);
    Command cancelCommand = new Command("Không", 3, 0);
    String messUpdate = "";
    String linkUpdate = "";
    CommandListener updateListener = new CommandListener() {
        public void commandAction(Command c, Displayable d) {
            if (c == MIDPlay.this.okCommand) {
                try {
                    if (!MIDPlay.this.platformRequest(MIDPlay.this.linkUpdate)) {
                        MIDPlay.this.exit();
                    }
                } catch (ConnectionNotFoundException var4) {
                }
            } else if (c == MIDPlay.this.cancelCommand) {
                MIDPlay.this.goBack();
            }

        }
    };

    public MIDPlay() {
        this.history.setSize(0);
    }

    public void startApp() {
        this.setMainScreen();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void commandAction(Command c, Displayable s) {
    }

    private void setMainScreen() {
        Image[] images = null;
        try {
            images = new Image[]{
                Image.createImage("/images/Magnifier.png"),
                Image.createImage("/images/Album.png"),
                Image.createImage("/images/MusicNotes.png"),
                Image.createImage("/images/Setting.png"),
                Image.createImage("/images/Mail.png"),
                Image.createImage("/images/Help.png")
            };
        } catch (Exception var4) {
            System.out.println(var4.getMessage());
        }

        String[] mainItems = new String[]{
            "Tìm kiếm",
            "Thể loại",
            "Playlist Hot",
            "Cài đặt",
            "Thông tin ứng dụng",
            "Chính sách bảo mật"
        };

        MainList mainMenu = new MainList("MIDPlay", mainItems, images);
        mainMenu.setObserver(this);
        this.go(mainMenu);
    }

    public Displayable go(Displayable d) {
        Displayable curr = this.getCurrentDisplayable();
        if (curr != null && !(curr instanceof Observer) && !this.history.contains(curr) && curr.getClass().toString().compareTo(curr.getClass().toString()) == 0) {
            System.out.println(curr.getClass());
            this.history.push(curr);
        }
        return this.replaceCurrent(d);
    }

    public Displayable goBack() {
        if (this.history.empty()) {
            this.exit();
            return null;
        } else {
            Displayable d;
            do {
                d = (Displayable) this.history.pop();
            } while (d.getClass().toString().compareTo(this.getCurrentDisplayable().getClass().toString()) == 0);

            return this.replaceCurrent(d);
        }
    }

    public void handle(Song song) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Displayable replaceCurrent(Displayable d) {
        this.getDisplay().setCurrent(d);
        if (!(d instanceof Alert)) {
            this.currDisplayable = d;
        }
        return d;
    }

    public Displayable getCurrentDisplayable() {
        return this.currDisplayable;
    }

    public final void exit() {
        this.destroyApp(false);
        this.notifyDestroyed();
    }

    private Display getDisplay() {
        return Display.getDisplay(this);
    }

}
