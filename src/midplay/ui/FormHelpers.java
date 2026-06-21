package midplay.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import midplay.util.Lang;

// Builds the small "type a name" forms used for creating/renaming custom
// playlists and for saving the queue as one. Centralises the Form + TextField +
// ok/cancel scaffolding and the shared empty-name validation that
// FavoritesScreen and QueueTrackListScreen used to copy verbatim.
public final class FormHelpers {
  public interface NameSubmitHandler {
    void onSubmit(String name);
  }

  private FormHelpers() {}

  public static void promptName(
      final Navigator navigator,
      String formTitle,
      String initialName,
      final NameSubmitHandler handler) {
    final Form form = new Form(formTitle);
    final TextField nameField =
        new TextField(Lang.tr("playlist.name"), initialName, 50, TextField.ANY);
    form.append(nameField);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              String name = nameField.getString().trim();
              if (name.length() == 0) {
                navigator.showAlert(Lang.tr("playlist.error.empty_name"), AlertType.ERROR);
                return;
              }
              handler.onSubmit(name);
            } else if (c == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(form);
  }
}
