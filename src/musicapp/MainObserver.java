package musicapp;

import javax.microedition.lcdui.Displayable;
import musicapp.model.Song;

public interface MainObserver {

    Displayable go(Displayable var1);

    Displayable goBack();

    void handle(Song var1);

    Displayable replaceCurrent(Displayable var1);

    Displayable getCurrentDisplayable();
}
