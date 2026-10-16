package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import midplay.MIDPlay;
import midplay.store.UriHistoryManager;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class OpenUriScreen extends BaseForm {
  private final TextField uriField;
  private final ChoiceGroup history;
  private final Command useHistory;
  private final Command clearHistory;

  public OpenUriScreen(Navigator navigator) {
    super(Lang.tr("open_uri.title"), navigator);
    uriField = new TextField(null, "", 255, TextField.ANY);
    append(uriField);
    history = new ChoiceGroup(Lang.tr("open_uri.history"), Choice.POPUP, UriHistoryManager.getInstance().getItems(), null);
    append(history);
    useHistory = new Command(Lang.tr("open_uri.use"), Command.SCREEN, 1);
    clearHistory = new Command(Lang.tr("open_uri.clear"), Command.SCREEN, 2);
    addCommand(Commands.ok());
    addCommand(useHistory);
    addCommand(clearHistory);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == useHistory) {
      useSelectedHistory();
      return;
    }
    if (c == clearHistory) {
      UriHistoryManager.getInstance().clear();
      refreshHistory();
      return;
    }
    if (c != Commands.ok()) {
      return;
    }
    String uri = uriField.getString().trim();
    if (uri.length() == 0) {
      navigator.showAlert(Lang.tr("open_uri.empty"), AlertType.ERROR);
      return;
    }
    try {
      MIDPlay.getInstance().openExternalUri(uri);
      UriHistoryManager.getInstance().record(uri);
      refreshHistory();
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void useSelectedHistory() {
    int selected = history.getSelectedIndex();
    if (selected >= 0) {
      uriField.setString(history.getString(selected));
    }
  }

  private void refreshHistory() {
    history.deleteAll();
    String[] items = UriHistoryManager.getInstance().getItems();
    for (int i = 0; i < items.length; i++) {
      history.append(items[i], null);
    }
  }
}
