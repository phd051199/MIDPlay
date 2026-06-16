package midplay.ui;

import midplay.MIDPlay;
import midplay.store.Configuration;
import midplay.util.Lang;
import midplay.util.Utils;

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

  public void forward(final Displayable newView) {
    if (newView == null) {
      return;
    }
    Display.getDisplay(this.midlet)
        .callSerially(
            new Runnable() {
              public void run() {
                Display display = Display.getDisplay(midlet);
                Displayable current = display.getCurrent();
                if (newView instanceof Alert) {
                  if (current != null && !(current instanceof Alert)) {
                    beforeAlert = current;
                  }
                  Alert alert = (Alert) newView;
                  if (beforeAlert != null) {
                    display.setCurrent(alert, beforeAlert);
                  } else {
                    display.setCurrent(alert);
                  }
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
            });
  }

  public void back() {
    Display.getDisplay(this.midlet)
        .callSerially(
            new Runnable() {
              public void run() {
                if (navStack.isEmpty()) {
                  midlet.notifyDestroyed();
                  return;
                }
                Displayable previous = (Displayable) navStack.pop();
                Display.getDisplay(midlet).setCurrent(previous);
                beforeAlert = null;
              }
            });
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

  // Run a runnable on the LCDUI event thread. Used to marshal off-thread
  // image-load callbacks before they touch Canvas fields or repaint.
  public void callSerially(Runnable r) {
    if (r == null) {
      return;
    }
    Display.getDisplay(this.midlet).callSerially(r);
  }

  public void showAlert(String message, AlertType type) {
    if (message == null) {
      return;
    }
    Alert a = new Alert(null, message, null, type);
    a.setTimeout(Configuration.ALERT_TIMEOUT);
    forward(a);
  }

  public void showAlert(String message, AlertType type, final Displayable d) {
    if (message == null) {
      return;
    }
    final Alert a = new Alert(null, message, null, type);
    a.setTimeout(Configuration.ALERT_TIMEOUT);
    Display.getDisplay(this.midlet)
        .callSerially(
            new Runnable() {
              public void run() {
                Display.getDisplay(midlet).setCurrent(a, d);
              }
            });
  }

  // Convenience for the common ok/cancel confirm: the cancel branch always
  // dismisses the alert, only the ok action differs. Absorbs the repeated
  // CommandListener boilerplate (ok -> onOk.run(), cancel -> dismissAlert).
  public void showConfirmationAlert(String message, Runnable onOk) {
    showConfirmationAlert(message, onOk, AlertType.INFO);
  }

  public void showConfirmationAlert(String message, final Runnable onOk, AlertType type) {
    if (message == null || onOk == null) {
      return;
    }
    Alert a = new Alert(null, message, null, type);
    a.addCommand(Commands.ok());
    a.addCommand(Commands.cancel());
    a.setTimeout(Alert.FOREVER);
    a.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              onOk.run();
            } else if (c == Commands.cancel()) {
              dismissAlert();
            }
          }
        });
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
    Display.getDisplay(this.midlet)
        .callSerially(
            new Runnable() {
              public void run() {
                Display display = Display.getDisplay(midlet);
                Displayable current = display.getCurrent();
                if (!(current instanceof Alert)) {
                  return;
                }
                if (beforeAlert != null) {
                  display.setCurrent(beforeAlert);
                  beforeAlert = null;
                } else if (!navStack.isEmpty()) {
                  Displayable previous = (Displayable) navStack.pop();
                  display.setCurrent(previous);
                } else {
                  midlet.notifyDestroyed();
                }
              }
            });
  }
}
