package app;

import app.common.AudioFileConnector;
import app.interfaces.Observer;
import app.model.Song;
import app.ui.MainList;
import app.ui.SettingForm;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Stack;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

public class MIDPlay extends MIDlet implements CommandListener, Utils.BreadCrumbTrail {

  private final Stack history = new Stack();
  private Displayable currDisplayable;

  public MIDPlay() {
    this.history.setSize(0);
    try {
      I18N.initialize(this);
      SettingForm.loadSettings();
      AudioFileConnector.getInstance().initialize();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void startApp() {
    this.setMainScreen();
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {}

  public void commandAction(Command c, Displayable s) {}

  private void setMainScreen() {
    MainList mainMenu = Utils.createMainMenu(this);
    this.go(mainMenu);
  }

  public Displayable go(Displayable d) {
    Displayable curr = this.getCurrentDisplayable();
    if (curr != null
        && !(curr instanceof Observer)
        && !this.history.contains(curr)
        && curr.getClass().toString().compareTo(curr.getClass().toString()) == 0) {
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
      } while (d.getClass().toString().compareTo(this.getCurrentDisplayable().getClass().toString())
          == 0);

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

  public void clearHistory() {
    this.history.removeAllElements();
  }

  private Display getDisplay() {
    return Display.getDisplay(this);
  }
}
