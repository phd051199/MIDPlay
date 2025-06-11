package musicapp;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import musicapp.model.Song;
import musicapp.utils.Utils;

public class AboutForm extends Form implements Utils.BreadCrumbTrail, CommandListener {

    public static final String APPLICATION = "Ứng dụng: NhacCuaTui";
    public static final String VERSION = "Phiên bản: 2.0";
    public static final String PRODUCT = "Đơn vị chủ quản: Công ty cổ phần NCT  ";
    public static final String EMAIL = "Email hỗ trợ: msupport@nct.vn";
    public static final String COPY_RIGHT = "Copyright@2012 NCT Corp. All rights reserved";

    protected Command backCommand = new Command("Trở lại", Command.BACK, 1);
    private Utils.BreadCrumbTrail parent;

    public AboutForm(String title, Utils.BreadCrumbTrail parent) {
        super(title);
        this.parent = parent;

        append(new StringItem(null, APPLICATION));
        append(new StringItem(null, VERSION));
        append(new StringItem(null, PRODUCT));
        append(new StringItem(null, EMAIL));
        append(new StringItem(null, COPY_RIGHT));

        this.addCommand(this.backCommand);
        this.setCommandListener(this);
    }

    public Displayable go(Displayable d) {
        return this.parent.go(d);
    }

    public Displayable goBack() {
        return this.parent.goBack();
    }

    public void handle(Song song) {
    }

    public Displayable replaceCurrent(Displayable d) {
        return this.parent.replaceCurrent(d);
    }

    public Displayable getCurrentDisplayable() {
        return this.parent.getCurrentDisplayable();
    }

    public void commandAction(Command c, Displayable d) {
        if (d == this && c == this.backCommand) {
            this.goBack();
        }
    }
}
