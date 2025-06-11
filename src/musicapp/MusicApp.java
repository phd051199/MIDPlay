package musicapp;

import java.util.Stack;
import java.util.Vector;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import musicapp.common.DeviceInfo;
import musicapp.common.Observer;
import musicapp.constants.APIConstants;
import musicapp.model.Song;
import musicapp.utils.Utils;

public class MusicApp extends MIDlet implements CommandListener, Utils.BreadCrumbTrail {

    private final Stack history = new Stack();
    private Displayable currDisplayable;
    private Form mainForm;
    Command okCommand = new Command("Có", 4, 0);
    Command cancelCommand = new Command("Không", 3, 0);
    String messUpdate = "";
    String linkUpdate = "";
    CommandListener updateListener = new CommandListener() {
        public void commandAction(Command c, Displayable d) {
            if (c == MusicApp.this.okCommand) {
                try {
                    if (!MusicApp.this.platformRequest(MusicApp.this.linkUpdate)) {
                        MusicApp.this.exit();
                    }
                } catch (ConnectionNotFoundException var4) {
                }
            } else if (c == MusicApp.this.cancelCommand) {
                MusicApp.this.goBack();
            }

        }
    };

    public MusicApp() {
        this.history.setSize(0);
        String imei = DeviceInfo.getDeviceImei();
        APIConstants.DEVICE_INFOR = "{\"DeviceID\":\"" + imei + "\",\"OsName\":\"Nokia\",\"OsVersion\":\"J2ME\",\"AppName\":\"MusicApp\",\"AppVersion\":\"2.0\",\"UserInfo\":\"\",\"LocationInfo\":\"\"}";
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
                Image.createImage("/images/MusicDoubleNote.png"),
                Image.createImage("/images/MusicArtist.png"),
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
            "Playlist New",
            "MusicApp",
            "Thông tin",
            "Privacy Policy"
        };

        MainList mainMenu = new MainList("NhacCuaTui", mainItems, images);
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

    private Vector splitReportMessage(String message) {
        Vector nodes = new Vector();
        String updateMess = "";
        String linkMess = "";
        int index = message.indexOf("@");
        if (index <= 0) {
            return null;
        } else {
            int j;
            for (j = 0; j < index; ++j) {
                updateMess = updateMess + message.charAt(j);
            }

            for (j = index + 2; j < message.length(); ++j) {
                linkMess = linkMess + message.charAt(j);
            }

            nodes.addElement(updateMess);
            nodes.addElement(linkMess);
            return nodes;
        }
    }


}
