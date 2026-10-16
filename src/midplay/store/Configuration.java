package midplay.store;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
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
  public static final String STORAGE_DOWNLOADS = "storage.downloads";
  public static final String STORAGE_DOWNLOADS_BACKUP = "storage.downloads.bak";
  public static final String STORAGE_CACHE = "storage.cache";
  public static final String STORAGE_CACHE_AUDIO = "storage.cache_audio";
  public static final String STORAGE_NOTES = "storage.notes";

  public static final String DEFAULT_DOWNLOAD_PATH = "file:///SDCard/MIDPlay/Downloads/";
  public static final String DEFAULT_CACHE_PATH = "file:///SDCard/MIDPlay/Cache/";

  public static final int CACHE_OFF = 0;
  public static final int CACHE_ON = 1;
  public static final String CACHE_STORAGE_RMS = "rms";
  public static final String CACHE_STORAGE_FILE = "file";
  public static final int MAX_CACHE_ENTRY_BYTES = 8 * 1024 * 1024;

  public static final String MENU_SEARCH = "menu.search";
  public static final String MENU_FAVORITES = "menu.favorites";
  public static final String MENU_DISCOVER_PLAYLISTS = "menu.discover_playlists";
  public static final String MENU_SETTINGS = "menu.settings";
  public static final String MENU_ABOUT = "menu.about";
  public static final String MENU_RECENT = "menu.recent";
  public static final String MENU_EQUALIZER = "menu.equalizer";
  public static final String MENU_DOWNLOADS = "menu.downloads";
  public static final String MENU_FILES = "menu.files";
  public static final String MENU_MPGRAM = "menu.mpgram";
  public static final String MENU_GITHUB = "menu.github";
  public static final String MENU_OPEN_URI = "menu.open_uri";
  public static final String MENU_NOTES = "menu.notes";
  public static final String MENU_NOW_PLAYING = "menu.now_playing";

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
  public static final int MPGRAM_AUTO_LOGIN_OFF = 0;
  public static final int MPGRAM_AUTO_LOGIN_ON = 1;

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
  public static Image mpgramIcon;
  public static Image githubIcon;
  public static Image notesIcon;
  public static Image fileExplorerIcon;
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

  private static final int ICON_CELL = 42;
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
    mpgramIcon = Utils.resizeImageToFit(Image.createImage("/m.png"), 36, 36);
    githubIcon = Utils.resizeImageToFit(Image.createImage("/g.png"), 36, 36);
    notesIcon = createNotesIcon();
    fileExplorerIcon = createFileExplorerIcon();

    folderBadgeIcon = Utils.resizeImageToFit(folderIcon, BADGE_SIZE, BADGE_SIZE);
    musicBadgeIcon = Utils.resizeImageToFit(musicIcon, BADGE_SIZE, BADGE_SIZE);

    loadPlayerIcons();
  }

  private static Image createNotesIcon() {
    Image image = Image.createImage(36, 36);
    Graphics graphics = image.getGraphics();
    // Paper shadow and page silhouette.
    graphics.setColor(0x243447);
    graphics.fillRoundRect(7, 5, 23, 29, 3, 3);
    graphics.setColor(0xF4F7FB);
    graphics.fillRoundRect(5, 3, 23, 29, 3, 3);
    graphics.setColor(0x52708F);
    graphics.drawRoundRect(5, 3, 23, 29, 3, 3);
    // Folded corner.
    graphics.setColor(0xDCE6F0);
    graphics.fillTriangle(20, 4, 27, 11, 20, 11);
    graphics.setColor(0x52708F);
    graphics.drawLine(20, 4, 20, 11);
    graphics.drawLine(20, 11, 27, 11);
    // Text lines and writing pencil.
    graphics.setColor(0x4B83B5);
    graphics.fillRect(9, 14, 14, 2);
    graphics.fillRect(9, 19, 12, 2);
    graphics.fillRect(9, 24, 9, 2);
    graphics.setColor(0xF0A83A);
    graphics.drawLine(19, 29, 29, 19);
    graphics.drawLine(20, 30, 30, 20);
    graphics.setColor(0xD37C35);
    graphics.drawLine(29, 19, 30, 20);
    return image;
  }

  private static Image createFileExplorerIcon() {
    Image image = Image.createImage(36, 36);
    Graphics graphics = image.getGraphics();
    // Distinct folder silhouette with a visible tab and blue front panel.
    graphics.setColor(0xB97920);
    graphics.fillRoundRect(3, 11, 30, 21, 4, 4);
    graphics.setColor(0xE7A832);
    graphics.fillRoundRect(4, 8, 15, 8, 3, 3);
    graphics.setColor(0xF4C65E);
    graphics.fillRoundRect(3, 12, 30, 18, 4, 4);
    graphics.setColor(0x397DB5);
    graphics.fillRoundRect(7, 16, 22, 12, 2, 2);
    graphics.setColor(0xDCEEFF);
    graphics.fillRect(10, 19, 10, 2);
    graphics.fillRect(10, 23, 7, 2);
    // Small magnifier indicates browsing/searching files.
    graphics.setColor(0xFFFFFF);
    graphics.drawArc(20, 3, 10, 10, 0, 360);
    graphics.drawLine(28, 12, 33, 17);
    graphics.setColor(0x376B93);
    graphics.drawArc(21, 4, 8, 8, 0, 360);
    graphics.drawLine(28, 11, 32, 15);
    return image;
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
