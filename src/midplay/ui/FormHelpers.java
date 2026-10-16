package midplay.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import midplay.util.Lang;

public final class FormHelpers {
  public interface NameSubmitHandler {
    void onSubmit(String name);
  }

  public interface LinkSubmitHandler {
    void onSubmit(String url, String thumb, String name, String artist);
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

  public static void promptLink(
      final Navigator navigator,
      String fileName,
      String initialUrl,
      String initialThumb,
      String initialName,
      String initialArtist,
      final LinkSubmitHandler handler) {
    final Form form = new Form(Lang.tr("download.link"));
    form.append(new StringItem(null, fileName));
    final TextField urlField =
        new TextField(Lang.tr("download.link_url"), initialUrl, 300, TextField.URL);
    final TextField thumbField =
        new TextField(Lang.tr("download.link_thumb"), initialThumb, 300, TextField.URL);
    final TextField nameField =
        new TextField(Lang.tr("download.link_name"), initialName, 60, TextField.ANY);
    final TextField artistField =
        new TextField(Lang.tr("download.link_artist"), initialArtist, 60, TextField.ANY);
    form.append(urlField);
    form.append(thumbField);
    form.append(nameField);
    form.append(artistField);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              String url = urlField.getString().trim();
              if (url.length() == 0) {
                navigator.showAlert(Lang.tr("download.link_error_url"), AlertType.ERROR);
                return;
              }
              handler.onSubmit(
                  url,
                  thumbField.getString().trim(),
                  nameField.getString().trim(),
                  artistField.getString().trim());
            } else if (c == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(form);
  }
}
