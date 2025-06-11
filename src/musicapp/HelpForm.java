package musicapp;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import musicapp.model.Song;
import musicapp.utils.Utils;

public class HelpForm extends Form implements Utils.BreadCrumbTrail, CommandListener {

    public static final String APPLICATION = "- The data is collected about a user is your device's IMEI. We don't share these information with 3rd parties.NhacCuaTui will get your device's IMEI to statistic the functionalities of application which you used. So, we can update the next version better. And Your account is secret and safe.";
    protected Command backCommand = new Command("Trở lại", Command.BACK, 1);
    private final Utils.BreadCrumbTrail parent;

    public HelpForm(String title, Utils.BreadCrumbTrail parent) {
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
