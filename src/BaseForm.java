import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

public abstract class BaseForm extends Form implements CommandListener {
  protected final Navigator navigator;

  public BaseForm(String title, Navigator navigator) {
    super(title);
    this.navigator = navigator;
    setupCommands();
    this.setCommandListener(this);
  }

  private void setupCommands() {
    addCommand(Commands.back());
  }

  public void commandAction(Command c, Displayable d) {
    try {
      if (c == Commands.back()) {
        navigator.back();
      } else {
        handleCommand(c, d);
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  protected abstract void handleCommand(Command c, Displayable d);
}
