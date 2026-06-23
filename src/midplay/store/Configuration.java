package midplay.store;

import java.io.IOException;
import javax.microedition.lcdui.Image;
import midplay.ui.Theme;
import midplay.util.Utils;

public class Configuration {
  public static final int ALERT_TIMEOUT = 2000;
  public static final String SORT_ICON = "→ ";
  public static final String VISIBILITY_ICON = "[x] ";
  public static final String HIDDEN_ICON = "[  ] ";

  public static final String STORAGE_MENU = "storage.menu";
  public static final String STORAGE_SETTINGS = "storage.settings";
  public static final String STORAGE_FAVORITES = "storage.favorites";
  public static final String STORAGE_TRACKS = "storage.tracks";
  public static final String STORAGE_LAST_SESSION = "storage.last_session";
  public static final String STORAGE_RECENT = "storage.recent";

  public static final String MENU_SEARCH = "menu.search";
  public static final String MENU_FAVORITES = "menu.favorites";
  public static final String MENU_DISCOVER_PLAYLISTS = "menu.discover_playlists";
  public static final String MENU_SETTINGS = "menu.settings";
  public static final String MENU_ABOUT = "menu.about";
  public static final String MENU_RECENT = "menu.recent";
  public static final String MENU_EQUALIZER = "menu.equalizer";

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

  public static final int SAVE_LAST_SESSION_OFF = 0;
  public static final int SAVE_LAST_SESSION_ON = 1;

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
  public static final int QUEUE_MAX_SIZE = 120;

  public static final String PLAYER_STATUS_STOPPED = "player.status.stopped";
  public static final String PLAYER_STATUS_PLAYING = "player.status.playing";
  public static final String PLAYER_STATUS_PAUSED = "player.status.paused";
  public static final String PLAYER_STATUS_LOADING = "player.status.loading";
  public static final String PLAYER_STATUS_FINISHED = "player.status.finished";
  public static final String PLAYER_STATUS_READY = "player.status.ready";

  public static final int BLACKBERRY_WIFI_OFF = 0;
  public static final int BLACKBERRY_WIFI_ON = 1;

  public static final int THUMBNAILS_OFF = 0;
  public static final int THUMBNAILS_ON = 1;

  public static Image folderIcon;
  public static Image musicIcon;
  public static Image searchIcon;
  public static Image favoriteIcon;
  public static Image playlistIcon;
  public static Image settingsIcon;
  public static Image infoIcon;
  public static Image recentIcon;
  public static Image equalizerIcon;
  public static Image folderBadgeIcon;
  public static Image musicBadgeIcon;

  public static Image playIcon;
  public static Image pauseIcon;
  public static Image playDimIcon;
  public static Image nextIcon;
  public static Image prevIcon;
  public static Image repeatIcon;
  public static Image repeatOffIcon;
  public static Image repeatOneIcon;
  public static Image shuffleIcon;
  public static Image shuffleOffIcon;
  public static Image nextDimIcon;
  public static Image prevDimIcon;

  private static final int ICON_CELL = 42; // largest icon dim; every icon <= 42x42
  private static final int ICON_COLS = 4;
  private static final int BADGE_SIZE = 24;
  private static Image iconSheet;

  public static void loadIcons() throws IOException {
    iconSheet = Image.createImage("/icons.png");

    folderIcon = region(0, 42, 40);
    musicIcon = region(1, 36, 36);
    searchIcon = region(2, 36, 33);
    favoriteIcon = region(3, 36, 36);
    playlistIcon = region(4, 36, 36);
    settingsIcon = region(5, 36, 36);
    infoIcon = region(6, 36, 36);
    recentIcon = region(7, 36, 36);
    equalizerIcon = region(8, 36, 36);

    folderBadgeIcon = Utils.resizeImageToFit(folderIcon, BADGE_SIZE, BADGE_SIZE);
    musicBadgeIcon = Utils.resizeImageToFit(musicIcon, BADGE_SIZE, BADGE_SIZE);

    loadPlayerIcons();
  }

  public static void loadPlayerIcons() {
    int activeIconColor = Theme.getPrimaryColor();
    int inactiveIconColor = Theme.getOutlineColor();

    playIcon = tintedRegion(9, 32, 32, activeIconColor);
    pauseIcon = tintedRegion(10, 32, 32, activeIconColor);
    nextIcon = tintedRegion(11, 32, 32, activeIconColor);
    prevIcon = tintedRegion(12, 32, 32, activeIconColor);
    repeatIcon = tintedRegion(13, 20, 20, activeIconColor);
    repeatOneIcon = tintedRegion(14, 20, 20, activeIconColor);
    shuffleIcon = tintedRegion(15, 20, 20, activeIconColor);

    repeatOffIcon = Utils.applyColor(repeatIcon, inactiveIconColor);
    shuffleOffIcon = Utils.applyColor(shuffleIcon, inactiveIconColor);
    playDimIcon = Utils.applyColor(playIcon, inactiveIconColor);
    nextDimIcon = Utils.applyColor(nextIcon, inactiveIconColor);
    prevDimIcon = Utils.applyColor(prevIcon, inactiveIconColor);
  }

  private static Image region(int index, int w, int h) {
    int x = (index % ICON_COLS) * ICON_CELL;
    int y = (index / ICON_COLS) * ICON_CELL;
    return Image.createImage(iconSheet, x, y, w, h, 0);
  }

  private static Image tintedRegion(int index, int w, int h, int color) {
    return Utils.applyColor(region(index, w, h), color);
  }

  private Configuration() {}
}
