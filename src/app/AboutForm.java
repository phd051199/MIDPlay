package app;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import app.model.Song;
import app.utils.Utils;

public class AboutForm extends Form implements Utils.BreadCrumbTrail, CommandListener {

    public static final String APPLICATION = "Ứng dụng: Music\n";

    protected Command backCommand = new Command("Trở lại", Command.BACK, 1);
    private Utils.BreadCrumbTrail parent;

    public AboutForm(String title, Utils.BreadCrumbTrail parent) {
        super(title);
        this.parent = parent;

        append(new StringItem(null, APPLICATION));

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
