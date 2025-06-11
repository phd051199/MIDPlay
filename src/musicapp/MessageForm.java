package musicapp;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import musicapp.utils.Utils;

public class MessageForm extends Form implements CommandListener {

    private LoadDataObserver loadDataObserver;
    Command exitCommand = new Command("Trở lại", 2, 1);
    StringItem message = new StringItem("", "");
    private Utils.BreadCrumbTrail observer;

    public void setLoadDataOberserver(LoadDataObserver observer) {
        this.loadDataObserver = observer;
    }

    public MessageForm(String title, String dataMessage, String messageType) {
        super(title);
        Font font = Font.getFont(0, 1, 0);
        this.message.setText(dataMessage);
        this.message.setFont(font);
        this.append(this.message);
        if (messageType.equals("error")) {
            this.addCommand(this.exitCommand);
            this.setCommandListener(this);
        }

    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.exitCommand) {
            this.observer.goBack();
            this.loadDataObserver.cancel();
        }

    }

    public void setObserver(Utils.BreadCrumbTrail _observer) {
        this.observer = _observer;
    }
}
