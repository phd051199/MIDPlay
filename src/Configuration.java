import java.io.IOException;
import javax.microedition.lcdui.Image;

public class Configuration {
  public static final int ALERT_TIMEOUT = 2000;
  public static final String SORT_ICON = "→ ";
  public static final String VISIBILITY_ICON = "[x] ";
  public static final String HIDDEN_ICON = "[  ] ";

  public static final String STORAGE_MENU = "storage.menu";
  public static final String STORAGE_SETTINGS = "storage.settings";
  public static final String STORAGE_FAVORITES = "storage.favorites";
  public static final String STORAGE_TRACKS = "storage.tracks";

  public static final String MENU_SEARCH = "menu.search";
  public static final String MENU_FAVORITES = "menu.favorites";
  public static final String MENU_DISCOVER_PLAYLISTS = "menu.discover_playlists";
  public static final String MENU_CHAT = "menu.chat";
  public static final String MENU_SETTINGS = "menu.settings";
  public static final String MENU_ABOUT = "menu.about";

  public static final String SERVICE_NCT = "NCT";
  public static final String SERVICE_SOUNDCLOUD = "SoundCloud";
  public static final String SERVICE_YTMUSIC = "YTMusic";
  public static final String SERVICE_SPOTIFY = "Spotify";
  public static final String[] ALL_SERVICES = {
    SERVICE_NCT, SERVICE_SOUNDCLOUD, SERVICE_YTMUSIC, SERVICE_SPOTIFY
  };

  public static final String THEME_LIGHT = "light";
  public static final String THEME_DARK = "dark";
  public static final String[] ALL_THEME_MODES = {THEME_LIGHT, THEME_DARK};

  public static final String[] THEME_COLOR_NAMES = {
    "Purple", "Blue", "Green", "Red", "Orange", "Pink", "Teal", "Indigo",
    "Cyan", "Amber", "Brown", "Grey", "Lime", "Deep Purple", "Light Blue", "Yellow"
  };
  public static final int[] THEME_COLORS = {
    0x65558F, 0x1976D2, 0x388E3C, 0xD32F2F,
    0xF57C00, 0xC2185B, 0x00796B, 0x303F9F,
    0x00BCD4, 0xFFC107, 0x795548, 0x607D8B,
    0x8BC34A, 0x512DA8, 0x03A9F4, 0xFFEB3B
  };

  public static final String QUALITY_128 = "128kbps";
  public static final String QUALITY_320 = "320kbps";
  public static final String[] ALL_QUALITIES = {QUALITY_128, QUALITY_320};

  public static final String SEARCH_PLAYLIST = "media.playlist";
  public static final String SEARCH_ALBUM = "media.album";
  public static final String SEARCH_TRACK = "media.track";
  public static final String[] ALL_SEARCH_TYPES = {SEARCH_PLAYLIST, SEARCH_ALBUM, SEARCH_TRACK};

  public static final int AUTO_UPDATE_DISABLED = 0;
  public static final int AUTO_UPDATE_ENABLED = 1;

  public static final String PLAYER_METHOD_PASS_INPUTSTREAM = "pass_inputstream";
  public static final String PLAYER_METHOD_PASS_URL = "pass_url";
  public static final String[] ALL_PLAYER_METHODS = {
    PLAYER_METHOD_PASS_INPUTSTREAM, PLAYER_METHOD_PASS_URL
  };

  public static final int PLAYER_MAX_VOLUME = 100;
  public static final int PLAYER_REPEAT_OFF = 0;
  public static final int PLAYER_REPEAT_ONE = 1;
  public static final int PLAYER_REPEAT_ALL = 2;
  public static final int PLAYER_SHUFFLE_OFF = 0;
  public static final int PLAYER_SHUFFLE_ON = 1;

  public static final String PLAYER_STATUS_STOPPED = "player.status.stopped";
  public static final String PLAYER_STATUS_PLAYING = "player.status.playing";
  public static final String PLAYER_STATUS_PAUSED = "player.status.paused";
  public static final String PLAYER_STATUS_LOADING = "player.status.loading";
  public static final String PLAYER_STATUS_STARTING = "player.status.starting";
  public static final String PLAYER_STATUS_STOPPING = "player.status.stopping";
  public static final String PLAYER_STATUS_FINISHED = "player.status.finished";
  public static final String PLAYER_STATUS_READY = "player.status.ready";

  public static final int BLACKBERRY_WIFI_OFF = 0;
  public static final int BLACKBERRY_WIFI_ON = 1;

  public static Image folderIcon;
  public static Image musicIcon;
  public static Image searchIcon;
  public static Image favoriteIcon;
  public static Image playlistIcon;
  public static Image chatIcon;
  public static Image settingsIcon;
  public static Image infoIcon;

  public static Image playIcon;
  public static Image pauseIcon;
  public static Image nextIcon;
  public static Image prevIcon;
  public static Image repeatIcon;
  public static Image repeatOffIcon;
  public static Image repeatOneIcon;
  public static Image shuffleIcon;
  public static Image shuffleOffIcon;
  public static Image nextDimIcon;
  public static Image prevDimIcon;

  public static void loadIcons() throws IOException {
    folderIcon = loadIcon("/FolderSound.png");
    musicIcon = loadIcon("/MusicDoubleNote.png");
    searchIcon = loadIcon("/Magnifier.png");
    favoriteIcon = loadIcon("/Heart.png");
    playlistIcon = loadIcon("/Album.png");
    chatIcon = loadIcon("/MessagingChat.png");
    settingsIcon = loadIcon("/Setting.png");
    infoIcon = loadIcon("/Information.png");

    loadPlayerIcons();
  }

  public static void loadPlayerIcons() throws IOException {
    int activeIconColor = Theme.getPrimaryColor();
    int inactiveIconColor = Theme.getOutlineColor();

    playIcon = loadIcon("/Play.png", activeIconColor);
    pauseIcon = loadIcon("/Pause.png", activeIconColor);
    nextIcon = loadIcon("/Next.png", activeIconColor);
    prevIcon = loadIcon("/Previous.png", activeIconColor);
    repeatIcon = loadIcon("/Repeat.png", activeIconColor);
    repeatOneIcon = loadIcon("/RepeatOne.png", activeIconColor);
    shuffleIcon = loadIcon("/Shuffle.png", activeIconColor);

    repeatOffIcon = Utils.applyColor(repeatIcon, inactiveIconColor);
    shuffleOffIcon = Utils.applyColor(shuffleIcon, inactiveIconColor);
    nextDimIcon = Utils.applyColor(nextIcon, inactiveIconColor);
    prevDimIcon = Utils.applyColor(prevIcon, inactiveIconColor);
  }

  private static Image loadIcon(String path) throws IOException {
    return Image.createImage(path);
  }

  private static Image loadIcon(String path, int color) throws IOException {
    return Utils.applyColor(loadIcon(path), color);
  }

  private Configuration() {}
}
