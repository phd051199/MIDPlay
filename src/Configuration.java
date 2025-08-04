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
  
  public static final String THEME_DARK = "dark";
  public static final String THEME_LIGHT = "light";
  
  public static final String IMAGES_BASE_PATH = "/imgs";

  public static Image folderIcon;
  public static Image musicIcon;
  public static Image searchIcon;
  public static Image favoriteIcon;
  public static Image playlistIcon;
  public static Image chatIcon;
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
  private static boolean isLightImgs = true;

  public static void loadImages() throws IOException {
    folderIcon = Image.createImage(IMAGES_BASE_PATH+"/FolderSound.png");
    musicIcon = Image.createImage(IMAGES_BASE_PATH+"/MusicDoubleNote.png");
    searchIcon = Image.createImage(IMAGES_BASE_PATH+"/Magnifier.png");
    favoriteIcon = Image.createImage(IMAGES_BASE_PATH+"/Heart.png");
    playlistIcon = Image.createImage(IMAGES_BASE_PATH+"/Album.png");
    chatIcon = Image.createImage(IMAGES_BASE_PATH+"/MessagingChat.png");
    settingsIcon = Image.createImage(IMAGES_BASE_PATH+"/Setting.png");
    infoIcon = Image.createImage(IMAGES_BASE_PATH+"/Information.png");
    
    boolean lightImgs = true;
    if(Theme.getCurrentTheme() != null){
        lightImgs = Theme.getCurrentTheme().getUseLightImages();
    }
    
    
    loadThemeImages(lightImgs);
  }
  
  public static void loadThemeImages(boolean light) throws IOException{
    isLightImgs = light;
    String themeImagesBasePath = IMAGES_BASE_PATH;  
    if(light){
        themeImagesBasePath += "/light";
    } else{
        themeImagesBasePath += "/dark";
    }
      
    nextIcon = Image.createImage(themeImagesBasePath+"/Next.png");
    pauseIcon = Image.createImage(themeImagesBasePath+"/Pause.png");
    playIcon = Image.createImage(themeImagesBasePath+"/Play.png");
    previousIcon = Image.createImage(themeImagesBasePath+"/Previous.png");
    repeatIcon = Image.createImage(themeImagesBasePath+"/Repeat.png");
    repeatOffIcon = Image.createImage(themeImagesBasePath+"/RepeatOff.png");
    repeatOneIcon = Image.createImage(themeImagesBasePath+"/RepeatOne.png");
    shuffleIcon = Image.createImage(themeImagesBasePath+"/Shuffle.png");
    shuffleOffIcon = Image.createImage(themeImagesBasePath+"/ShuffleOff.png");
  }

  private Configuration() {}
}
