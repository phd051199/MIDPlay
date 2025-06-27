package app.ui;

import app.common.ParseData;
import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.model.Song;
import app.ui.player.PlayerCanvas;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class SongList extends List implements CommandListener, LoadDataObserver {
  public static PlayerCanvas playerCanvas = null;

  private Command nowPlayingCommand;
  private Command exitCommand;
  private Command selectCommand;
  private Command searchCommand;

  private final Vector images;
  private final Vector songItems;
  private Utils.BreadCrumbTrail observer;
  int curPage = 1;
  int perPage = 10;
  private final Playlist playlist;
  Thread mLoaDataThread;
  private Image defaultImage;

  public SongList(String title, Vector items, Playlist _playlist) {
    super(title, List.IMPLICIT);
    this.playlist = _playlist;
    this.curPage = 1;
    this.perPage = 10;
    this.loadDefaultImage();
    this.initCommands();
    this.images = new Vector();
    this.songItems = items;
    this.initComponents();
    this.setCommandListener(this);
  }

  private void initCommands() {
    this.selectCommand = new Command(I18N.tr("select"), Command.OK, 1);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 2);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.searchCommand = new Command(I18N.tr("search"), Command.SCREEN, 3);
    this.addCommand(this.selectCommand);
    this.addCommand(this.exitCommand);
    this.addCommand(this.searchCommand);
    this.addCommand(this.nowPlayingCommand);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == this.selectCommand || c == List.SELECT_COMMAND) {
      int selectedItemIndex = getSelectedIndex();
      if (selectedItemIndex >= 0 && selectedItemIndex < this.songItems.size()) {
        Song song = (Song) this.songItems.elementAt(selectedItemIndex);
        if (song.getSongName().equals(I18N.tr("load_more"))) {
          this.doLoadMoreAction(song);
        } else {
          this.gotoPlaySong();
        }
      }
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    } else if (c == this.searchCommand) {
      MainList.gotoSearch(this.observer);
    }
  }

  private void loadDefaultImage() {
    try {
      this.defaultImage = Image.createImage("/images/MusicDoubleNote.png");
    } catch (Exception e) {
    }
  }

  private void loadMoreSongs(final String genKey, final int curPage, final int perPage) {
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector listItems = ParseData.parseSongsInPlaylist(genKey, "", curPage, perPage, "");
                if (listItems != null) {
                  SongList.this.addMorePlaylists(listItems);
                  SongList.this.repaintList();
                }
              }
            });
    this.mLoaDataThread.start();
  }

  private void initComponents() {
    this.createImages();
    this.deleteAll();
    for (int i = 0; i < this.songItems.size(); ++i) {
      Song song = (Song) this.songItems.elementAt(i);
      if (song != null) {
        Image imagePart = this.getImage(i);
        this.append(song.getSongName() + " \n" + song.getArtistName(), imagePart);
      }
    }

    if (this.size() > 0) {
      this.setSelectedIndex(0, true);
    }
  }

  private void repaintList() {
    int currentIndex = this.getSelectedIndex();

    try {
      this.deleteAll();

      if (this.songItems != null && this.songItems.size() > 0) {
        for (int i = 0; i < this.songItems.size(); ++i) {
          Song song = (Song) this.songItems.elementAt(i);
          this.append(song.getSongName() + " \n" + song.getArtistName(), this.defaultImage);
        }

        if (currentIndex >= 0 && currentIndex < this.songItems.size()) {
          this.setSelectedIndex(currentIndex, true);
        } else {
          this.setSelectedIndex(0, true);
        }
      }
    } catch (Exception var3) {
    }
  }

  private void createImages() {
    try {
      this.images.removeAllElements();
      for (int i = 0; i < this.songItems.size(); ++i) {
        this.images.addElement(this.defaultImage);
      }
    } catch (Exception var3) {
    }
  }

  private void addMorePlaylists(Vector playlists) {
    for (int i = 0; i < playlists.size(); ++i) {
      this.songItems.addElement(playlists.elementAt(i));
    }
  }

  private void doLoadMoreAction(Song song) {
    this.curPage++;
    if (this.songItems.size() > 0) {
      Song lastSong = (Song) this.songItems.elementAt(this.songItems.size() - 1);
      if (lastSong.getSongName().equals(I18N.tr("load_more"))) {
        this.songItems.removeElementAt(this.songItems.size() - 1);
      }
    }
    this.loadMoreSongs(this.playlist.getId(), this.curPage, this.perPage);
  }

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  private void gotoPlaySong() {
    if (playerCanvas == null) {
      playerCanvas =
          new PlayerCanvas(
              this.playlist.getName(), this.songItems, this.getSelectedIndex(), this.playlist);
    } else {
      playerCanvas.change(
          this.playlist.getName(), this.songItems, this.getSelectedIndex(), this.playlist);
    }
    this.play();
  }

  void play() {
    playerCanvas.setObserver(this.observer);
    this.observer.go(playerCanvas);
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoaDataThread != null && this.mLoaDataThread.isAlive()) {
        this.mLoaDataThread.join();
      }
    } catch (InterruptedException var2) {
    }
  }

  public Image getImage(int index) {
    return (Image)
        (this.images != null && this.images.size() > index ? this.images.elementAt(index) : null);
  }
}
