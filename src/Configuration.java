import java.io.IOException;
import javax.microedition.lcdui.Image;

public class Configuration {
  public static final int ALERT_TIMEOUT = 2000;
  public static final String SORT_ICON = "→ ";
  public static final String VISIBILITY_ICON = "[x] ";
  public static final String HIDDEN_ICON = "[  ] ";

  private Configuration() {}

  public static class Images {
    public static Image folderIcon;
    public static Image musicIcon;
    public static Image searchIcon;
    public static Image favoriteIcon;
    public static Image playlistIcon;
    public static Image settingsIcon;
    public static Image infoIcon;
    public static Image nextIcon;
    public static Image pauseIcon;
    public static Image playIcon;
    public static Image previousIcon;
    public static Image repeatIcon;
    public static Image repeatOffIcon;
    public static Image repeatOneIcon;
    public static Image shuffleIcon;
    public static Image shuffleOffIcon;

    public static void load() throws IOException {
      folderIcon = Image.createImage("FolderSound.png");
      musicIcon = Image.createImage("MusicDoubleNote.png");
      searchIcon = Image.createImage("Magnifier.png");
      favoriteIcon = Image.createImage("Heart.png");
      playlistIcon = Image.createImage("Album.png");
      settingsIcon = Image.createImage("Setting.png");
      infoIcon = Image.createImage("Information.png");
      nextIcon = Image.createImage("Next.png");
      pauseIcon = Image.createImage("Pause.png");
      playIcon = Image.createImage("Play.png");
      previousIcon = Image.createImage("Previous.png");
      repeatIcon = Image.createImage("Repeat.png");
      repeatOffIcon = Image.createImage("RepeatOff.png");
      repeatOneIcon = Image.createImage("RepeatOne.png");
      shuffleIcon = Image.createImage("Shuffle.png");
      shuffleOffIcon = Image.createImage("ShuffleOff.png");
    }

    private Images() {}
  }

  public static class HttpMethod {
    public static final int PASS_URL = 0;
    public static final int PASS_CONNECTION_STREAM = 1;

    private HttpMethod() {}
  }

  public static class StorageKeys {
    public static final String MENU = "mn";
    public static final String SETTINGS = "st";
    public static final String FAVORITES = "fv";
    public static final String TRACKS = "tr";

    private StorageKeys() {}
  }

  public static class Menu {
    public static final String SEARCH = "search";
    public static final String FAVORITES = "favorites";
    public static final String DISCOVER_PLAYLISTS = "discover_playlists";
    public static final String SETTINGS = "settings";
    public static final String ABOUT = "about";

    private Menu() {}
  }

  public static class Services {
    public static final String NCT = "NCT";
    public static final String SOUNDCLOUD = "SoundCloud";
    public static final String YTMUSIC = "YTMusic";
    public static final String SPOTIFY = "Spotify";
    public static final String[] ALL = {NCT, SOUNDCLOUD, YTMUSIC, SPOTIFY};

    private Services() {}
  }

  public static class Quality {
    public static final String QUALITY_128 = "128kbps";
    public static final String QUALITY_320 = "320kbps";
    public static final String[] ALL = {QUALITY_128, QUALITY_320};

    private Quality() {}
  }

  public static class SearchType {
    public static final String PLAYLIST = "playlist";
    public static final String ALBUM = "album";
    public static final String TRACK = "track";
    public static final String[] ALL = {PLAYLIST, ALBUM, TRACK};

    private SearchType() {}
  }

  public static class AutoUpdate {
    public static final int DISABLED = 0;
    public static final int ENABLED = 1;

    private AutoUpdate() {}
  }

  public static class PlayerMethodInputStream {
    public static final int DISABLED = 0;
    public static final int ENABLED = 1;

    private PlayerMethodInputStream() {}
  }

  public static class Player {
    public static final int MAX_VOLUME = 100;

    private Player() {}

    public static class RepeatMode {
      public static final int OFF = 0;
      public static final int ONE = 1;
      public static final int ALL = 2;

      private RepeatMode() {}
    }

    public static class ShuffleMode {
      public static final int OFF = 0;
      public static final int ON = 1;

      private ShuffleMode() {}
    }
  }
}
