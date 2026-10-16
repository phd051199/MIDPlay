package midplay.ui.screen;

import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public final class TextFileEditorScreen extends TextBox implements CommandListener {
  private final Navigator navigator;
  private final String path;

  public TextFileEditorScreen(String title, String path, String text, Navigator navigator) {
    super(title, text, 32768, TextField.ANY);
    this.path = path;
    this.navigator = navigator;
    addCommand(Commands.formSave());
    addCommand(Commands.back());
    setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    try {
      if (command == Commands.back()) {
        navigator.back();
      } else if (command == Commands.formSave()) {
        saveFile();
        navigator.showAlert(Lang.tr("note.saved"), AlertType.CONFIRMATION);
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void saveFile() throws Exception {
    FileConnection file = null;
    OutputStream output = null;
    try {
      file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
      file.truncate(0);
      output = file.openOutputStream();
      byte[] bytes = Utils.utf8ToBytes(getString());
      output.write(bytes, 0, bytes.length);
    } finally {
      Utils.closeQuietly(output);
      if (file != null) {
        try {
          file.close();
        } catch (Exception e) {
        }
      }
    }
  }
}
