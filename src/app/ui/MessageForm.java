package app.ui;

import app.interfaces.LoadDataObserver;
import app.interfaces.MainObserver;
import app.utils.I18N;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public class MessageForm extends Form implements CommandListener {

  private LoadDataObserver loadDataObserver;
  Command exitCommand = new Command(I18N.tr("back"), 2, 1);
  StringItem message = new StringItem("", "");
  private MainObserver observer;

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

  public void setLoadDataOberserver(LoadDataObserver observer) {
    this.loadDataObserver = observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
      this.loadDataObserver.cancel();
    }
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }
}
