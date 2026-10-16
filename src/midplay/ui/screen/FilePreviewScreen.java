package midplay.ui.screen;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import midplay.store.NotesManager;
import midplay.ui.BaseForm;
import midplay.ui.Navigator;
import midplay.util.Lang;
import midplay.util.Utils;

public final class FilePreviewScreen extends BaseForm {
  private static final int MAX_TEXT_BYTES = 32768;

  private final String path;
  private final StringItem status;
  private final Command editCommand;
  private final Command importNotesCommand;

  public FilePreviewScreen(String title, String path, Navigator navigator) {
    super(title, navigator);
    this.path = path;
    status = new StringItem(null, Lang.tr("status.loading"));
    editCommand = new Command(Lang.tr("note.edit"), Command.SCREEN, 1);
    importNotesCommand = new Command(Lang.tr("note.import"), Command.SCREEN, 2);
    append(status);
    addCommand(editCommand);
    if (path.toLowerCase().endsWith(".vnt")) {
      addCommand(importNotesCommand);
    }
    load();
  }

  protected void handleCommand(Command command, Displayable displayable) {
    if (command == editCommand) {
      navigator.forward(new TextFileEditorScreen(getTitle(), path, status.getText(), navigator));
    } else if (command == importNotesCommand) {
      try {
        NotesManager.getInstance().importFromDevice(path);
        navigator.showAlert(Lang.tr("note.imported"), javax.microedition.lcdui.AlertType.CONFIRMATION);
      } catch (Exception e) {
        navigator.showAlert(e.toString(), javax.microedition.lcdui.AlertType.ERROR);
      }
    }
  }

  private void load() {
    new Thread(
            new Runnable() {
              public void run() {
                try {
                  showText(loadText());
                } catch (final Exception e) {
                  navigator.callSerially(
                      new Runnable() {
                        public void run() {
                          status.setText(e.toString());
                        }
                      });
                }
              }
            })
        .start();
  }

  private String loadText() throws Exception {
    FileConnection file = null;
    InputStream input = null;
    try {
      file = (FileConnection) Connector.open(path, Connector.READ);
      input = file.openInputStream();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int remaining = MAX_TEXT_BYTES;
      while (remaining > 0) {
        int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
        if (count < 0) {
          break;
        }
        output.write(buffer, 0, count);
        remaining -= count;
      }
      return Utils.bytesToUtf8(output.toByteArray());
    } finally {
      Utils.closeQuietly(input);
      close(file);
    }
  }

  private void showText(final String text) {
    navigator.callSerially(
        new Runnable() {
          public void run() {
            status.setText(text);
          }
        });
  }

  private static void close(FileConnection file) {
    if (file != null) {
      try {
        file.close();
      } catch (Exception e) {
      }
    }
  }
}
