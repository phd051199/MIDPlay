package midplay.ui.screen;

import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Utils;

public final class ImagePreviewCanvas extends Canvas implements CommandListener {
  private final Navigator navigator;
  private final String path;
  private Image original;
  private Image scaled;
  private String error;

  public ImagePreviewCanvas(String title, String path, Navigator navigator) {
    setTitle(title);
    this.path = path;
    this.navigator = navigator;
    addCommand(Commands.back());
    setCommandListener(this);
    load();
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == Commands.back()) {
      navigator.back();
    }
  }

  protected void paint(Graphics graphics) {
    graphics.setColor(0x000000);
    graphics.fillRect(0, 0, getWidth(), getHeight());
    if (scaled != null) {
      graphics.drawImage(scaled, getWidth() / 2, getHeight() / 2, Graphics.HCENTER | Graphics.VCENTER);
    } else {
      graphics.setColor(0xFFFFFF);
      String message = error == null ? "Loading..." : error;
      graphics.drawString(message, getWidth() / 2, getHeight() / 2, Graphics.HCENTER | Graphics.BASELINE);
    }
  }

  protected void sizeChanged(int width, int height) {
    scaleToCanvas();
  }

  private void load() {
    new Thread(
            new Runnable() {
              public void run() {
                FileConnection file = null;
                InputStream input = null;
                try {
                  file = (FileConnection) Connector.open(path, Connector.READ);
                  input = file.openInputStream();
                  original = Image.createImage(input);
                  scaleToCanvas();
                } catch (Exception e) {
                  error = e.toString();
                } finally {
                  Utils.closeQuietly(input);
                  if (file != null) {
                    try {
                      file.close();
                    } catch (Exception e) {
                    }
                  }
                  repaint();
                }
              }
            })
        .start();
  }

  private void scaleToCanvas() {
    if (original == null || getWidth() <= 0 || getHeight() <= 0) {
      return;
    }
    scaled = Utils.resizeImageToFit(original, getWidth(), getHeight());
    repaint();
  }
}
