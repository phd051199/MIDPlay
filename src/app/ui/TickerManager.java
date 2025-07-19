package app.ui;

import app.MIDPlay;
import app.ui.player.PlayerCanvas;
import java.util.Hashtable;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Ticker;

public class TickerManager {

  private static TickerManager instance;
  private static final Object instanceLock = new Object();
  private String currentSongInfo = null;
  private final Hashtable displayableTickerTexts = new Hashtable();

  private TickerManager() {}

  public static TickerManager getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new TickerManager();
        }
      }
    }
    return instance;
  }

  public void updateTicker(Displayable displayable) {
    if (displayable == null) {
      return;
    }

    if (displayable instanceof PlayerCanvas) {
      displayable.setTicker(null);
      return;
    }

    try {
      String songInfo = getCurrentSongInfo();
      String displayableKey = displayable.getClass().getName() + "@" + displayable.hashCode();
      String lastText = (String) displayableTickerTexts.get(displayableKey);

      if (songInfo != null && songInfo.length() > 0) {
        if (lastText == null || !songInfo.equals(lastText)) {
          displayableTickerTexts.put(displayableKey, songInfo);
          Ticker ticker = new Ticker(songInfo);
          displayable.setTicker(ticker);
        }
      } else {
        if (lastText != null) {
          displayableTickerTexts.remove(displayableKey);
          displayable.setTicker(null);
        }
      }
    } catch (Exception e) {
      displayable.setTicker(null);
    }
  }

  public void notifySongChanged() {
    try {
      String newSongInfo = getCurrentSongInfo();
      if (currentSongInfo == null || !newSongInfo.equals(currentSongInfo)) {
        currentSongInfo = newSongInfo;
        displayableTickerTexts.clear();
        updateCurrentDisplayable();
      }
    } catch (Exception e) {
    }
  }

  private String getCurrentSongInfo() {
    try {
      if (SongList.playerCanvas != null && SongList.playerCanvas.getPlayerGUI() != null) {
        return SongList.playerCanvas.getPlayerGUI().getSongName()
            + " - "
            + SongList.playerCanvas.getPlayerGUI().getSinger();
      }
    } catch (Exception e) {
    }
    return null;
  }

  private void updateCurrentDisplayable() {
    try {
      Displayable current = MIDPlay.getInstance().getCurrentDisplayable();
      if (current != null && !(current instanceof PlayerCanvas)) {
        updateTicker(current);
      }
    } catch (Exception e) {
    }
  }
}
