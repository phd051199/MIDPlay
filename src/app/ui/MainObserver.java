package app.ui;

import app.models.Song;
import javax.microedition.lcdui.Displayable;

public interface MainObserver {

  Displayable go(Displayable displayable);

  Displayable goBack();

  void handle(Song song);

  Displayable replaceCurrent(Displayable displayable);

  Displayable getCurrentDisplayable();
}
