package app.ui;

import app.core.data.LoadDataObserver;
import app.core.threading.ThreadManagerIntegration;
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
  private volatile boolean isCancelled = false;
  private final Object cancellationLock = new Object();

  public MessageForm(String title, String dataMessage, String messageType) {
    super(title);
    Font font = Font.getFont(0, 1, 0);
    this.message.setText(dataMessage);
    this.message.setFont(font);
    this.append(this.message);
    if (messageType.equals("error")
        || messageType.equals("info")
        || messageType.equals("loading")) {
      this.addCommand(this.exitCommand);
      this.setCommandListener(this);
    }
  }

  public void setLoadDataOberserver(LoadDataObserver observer) {
    this.loadDataObserver = observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      performCancellationAndExit();
    }
  }

  private void performCancellationAndExit() {
    synchronized (cancellationLock) {
      if (isCancelled) {
        return;
      }
      isCancelled = true;
    }

    try {
      if (this.loadDataObserver != null) {
        this.loadDataObserver.cancel();
      }

      ThreadManagerIntegration.cancelPendingDataOperations();
    } catch (Exception e) {
    } finally {
      if (this.observer != null) {
        this.observer.goBack();
      }
    }
  }

  public boolean isCancelled() {
    synchronized (cancellationLock) {
      return isCancelled;
    }
  }

  public void setObserver(MainObserver _observer) {
    this.observer = _observer;
  }
}
