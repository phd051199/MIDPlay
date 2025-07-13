package app.ui;

import app.MIDPlay;
import app.constants.AppConstants;
import app.models.Song;
import app.utils.I18N;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public class AboutForm extends Form implements MainObserver, CommandListener {

  public static final String APPLICATION = I18N.tr("about");
  protected Command backCommand = new Command(I18N.tr("back"), Command.BACK, 1);
  private final MainObserver parent;

  public AboutForm(String title, MainObserver parent) {
    super(title);
    this.parent = parent;

    append(new StringItem(null, "Version: " + MIDPlay.getAppVersion() + "\n"));
    append(new StringItem(null, "Developer: " + AppConstants.APP_AUTHOR + "\n"));

    this.addCommand(this.backCommand);
    this.setCommandListener(this);
  }

  public Displayable go(Displayable d) {
    return this.parent.go(d);
  }

  public Displayable goBack() {
    return this.parent.goBack();
  }

  public void handle(Song song) {}

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
