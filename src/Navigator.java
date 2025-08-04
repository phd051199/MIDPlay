import java.util.Stack;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Gauge;
import javax.microedition.midlet.MIDlet;

public class Navigator {
  private final Stack navStack;
  private final MIDlet midlet;
  private Displayable beforeAlert;

  public Navigator(MIDlet midlet) {
    this.midlet = midlet;
    this.navStack = new Stack();
    this.beforeAlert = null;
  }

  public void forward(Displayable newView) {
    if (newView == null) {
      return;
    }
    Display display = Display.getDisplay(this.midlet);
    Displayable current = display.getCurrent();
    if (newView instanceof Alert) {
      if (current != null && !(current instanceof Alert)) {
        beforeAlert = current;
      }
      display.setCurrent((Alert) newView, beforeAlert);
    } else {
      if (current instanceof Alert) {
        if (beforeAlert != null) {
          navStack.push(beforeAlert);
        }
        beforeAlert = null;
      } else if (current != null) {
        navStack.push(current);
      }
      display.setCurrent(newView);
    }
  }

  public void back() {
    if (navStack.isEmpty()) {
      midlet.notifyDestroyed();
      return;
    }
    Displayable previous = (Displayable) navStack.pop();
    Display.getDisplay(this.midlet).setCurrent(previous);
    beforeAlert = null;
  }

  public Displayable getPrevious() {
    if (navStack.isEmpty()) {
      return null;
    }
    return (Displayable) navStack.peek();
  }

  public void clear() {
    navStack.removeAllElements();
    beforeAlert = null;
  }

  public Displayable getCurrent() {
    Displayable current = Display.getDisplay(midlet).getCurrent();
    return (current instanceof Alert && beforeAlert != null) ? beforeAlert : current;
  }

  public void showAlert(String message, AlertType type) {
    if (message == null) {
      return;
    }
    Alert a = new Alert(null, message, null, type);
    a.setTimeout(Configuration.ALERT_TIMEOUT);
    forward(a);
  }

  public void showAlert(String message, AlertType type, Displayable d) {
    if (message == null) {
      return;
    }
    Alert a = new Alert(null, message, null, type);
    a.setTimeout(Configuration.ALERT_TIMEOUT);
    Display.getDisplay(this.midlet).setCurrent(a, d);
  }

  public void showConfirmationAlert(String message, CommandListener listener) {
    showConfirmationAlert(message, listener, AlertType.INFO);
  }

  public void showConfirmationAlert(String message, CommandListener listener, AlertType type) {
    if (message == null || listener == null) {
      return;
    }
    Alert a = new Alert(null, message, null, type);
    a.addCommand(Commands.ok());
    a.addCommand(Commands.cancel());
    a.setTimeout(Alert.FOREVER);
    a.setCommandListener(listener);
    forward(a);
  }

  public void showLoadingAlert(String s) {
    if (Utils.isJ2MELoader()) {
      return;
    }
    Alert a = new Alert(null, s == null ? Lang.tr("status.loading") : s, null, null);
    a.addCommand(Commands.cancel());
    a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
    a.setTimeout(Alert.FOREVER);
    a.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.cancel()) {
              MIDPlay.cancelOperation();
              dismissAlert();
            }
          }
        });
    forward(a);
  }

  public void dismissAlert() {
    Display display = Display.getDisplay(this.midlet);
    if (beforeAlert != null) {
      display.setCurrent(beforeAlert);
      beforeAlert = null;
    } else if (!navStack.isEmpty()) {
      Displayable previous = (Displayable) navStack.pop();
      display.setCurrent(previous);
    } else {
      this.midlet.notifyDestroyed();
    }
  }
}
