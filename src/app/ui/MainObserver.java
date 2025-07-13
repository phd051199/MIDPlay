package app.ui;

import app.models.Song;
import javax.microedition.lcdui.Displayable;

public interface MainObserver {

  Displayable go(Displayable e);

  Displayable goBack();

  void handle(Song e);

  Displayable replaceCurrent(Displayable e);

  Displayable getCurrentDisplayable();
}
