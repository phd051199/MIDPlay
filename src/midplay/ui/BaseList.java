package midplay.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import midplay.player.PlayerNavHelper;
import midplay.util.Utils;

public abstract class BaseList extends List implements CommandListener {
  protected final Navigator navigator;

  public BaseList(String title, Navigator navigator) {
    super(title, List.IMPLICIT);
    this.navigator = navigator;
    setupCommands();
    setCommandListener(this);
  }

  private void setupCommands() {
    addCommand(Commands.back());
    addCommand(Commands.playerNowPlaying());
  }

  public void commandAction(Command c, Displayable d) {
    try {
      if (c == List.SELECT_COMMAND) {
        handleSelection();
      } else if (c == Commands.back()) {
        navigator.back();
      } else if (c == Commands.playerNowPlaying()) {
        PlayerNavHelper.showNowPlaying(navigator);
      } else {
        handleCommand(c, d);
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  protected abstract void populateItems();

  protected void refresh() {
    int index = getSelectedIndex();
    this.deleteAll();
    populateItems();
    Utils.clampAndSelect(this, index);
  }

  protected abstract void handleSelection();

  protected abstract void handleCommand(Command c, Displayable d);
}
