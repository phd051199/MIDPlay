package app.ui.player;

import app.models.Playlist;
import app.models.Song;
import app.ui.SongList;
import app.utils.text.LocalizationManager;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.List;

public class CurrentPlaylistSongList extends SongList {

  private final int currentPlayingIndex;
  private final PlayerCanvas playerCanvas;

  public CurrentPlaylistSongList(
      String title, Vector items, Playlist playlist, int currentIndex, PlayerCanvas parent) {
    super(title, items, playlist);
    this.currentPlayingIndex = currentIndex;
    this.playerCanvas = parent;

    this.initializeCurrentPlaylistView();
  }

  private void initializeCurrentPlaylistView() {
    this.deleteAll();

    if (this.songItems != null && this.songItems.size() > 0) {
      for (int i = 0; i < this.songItems.size(); i++) {
        Song song = (Song) this.songItems.elementAt(i);
        if (song != null) {
          String songName = song.getSongName();
          String artistName = song.getArtistName();

          if (songName == null || songName.length() == 0) {
            songName = "Unknown Song";
          }
          if (artistName == null || artistName.length() == 0) {
            artistName = "Unknown Artist";
          }

          String displayText = songName + "\n" + artistName;

          if (i == this.currentPlayingIndex) {
            displayText = "(" + LocalizationManager.tr("currently_playing") + ") " + displayText;
          }

          this.append(displayText, this.defaultImage);
        } else {
          this.append("Unknown Song\nUnknown Artist", this.defaultImage);
        }
      }

      scrollToCurrentSong();
    } else {
      this.append("No songs in playlist", null);
    }
  }

  private void scrollToCurrentSong() {
    if (this.size() == 0) {
      return;
    }

    int targetIndex = this.currentPlayingIndex;

    if (targetIndex < 0) {
      targetIndex = 0;
    } else if (targetIndex >= this.size()) {
      targetIndex = this.size() - 1;
    }

    this.setSelectedIndex(targetIndex, true);
    ensureCurrentSongVisible();
  }

  private void ensureCurrentSongVisible() {
    if (this.currentPlayingIndex < 0 || this.currentPlayingIndex >= this.size()) {
      return;
    }

    try {
      int visibleItems = getVisibleItemCount();

      if (visibleItems > 0 && this.size() > visibleItems) {
        int optimalPosition = Math.max(0, this.currentPlayingIndex - (visibleItems / 3));

        if (optimalPosition < this.size()) {
          this.setSelectedIndex(optimalPosition, false);
          this.setSelectedIndex(this.currentPlayingIndex, true);
        }
      }
    } catch (Exception e) {
    }
  }

  private int getVisibleItemCount() {
    try {
      int listHeight = this.getHeight();
      Font font = this.size() > 0 ? this.getFont(0) : Font.getDefaultFont();
      int itemHeight = font.getHeight() * 2 + 4;
      return Math.max(1, listHeight / itemHeight);
    } catch (Exception e) {
      return 5;
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      if (this.observer != null) {
        this.observer.replaceCurrent(this.playerCanvas);
      }
    } else if (c == this.nowPlayingCommand) {
      if (this.observer != null) {
        this.observer.replaceCurrent(this.playerCanvas);
      }
    } else if (c == List.SELECT_COMMAND) {
      int selectedIndex = this.getSelectedIndex();
      if (selectedIndex >= 0
          && selectedIndex < this.songItems.size()
          && selectedIndex != this.currentPlayingIndex) {
        try {
          this.playerCanvas.getGUI().setCurrentSongIndex(selectedIndex);
          this.playerCanvas.getGUI().closePlayer();
          this.playerCanvas.getGUI().startPlayer();
          this.playerCanvas.setStatus(LocalizationManager.tr("playing"));

          if (this.observer != null) {
            this.observer.replaceCurrent(this.playerCanvas);
          }
        } catch (Throwable e) {
        }
      } else if (selectedIndex == this.currentPlayingIndex) {
        if (this.observer != null) {
          this.observer.replaceCurrent(this.playerCanvas);
        }
      }
    } else {
      super.commandAction(c, d);
    }
  }
}
