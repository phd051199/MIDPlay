package app.constants;

public class MenuConstants {

  public static final String[] MAIN_MENU_ICONS_NCT = {
    "/images/Magnifier.png",
    "/images/Heart.png",
    "/images/Album.png",
    "/images/MusicPlaylist.png",
    "/images/MusicNoteBlue.png",
    "/images/MusicNote.png",
    "/images/MessagingChat.png",
    "/images/Setting.png",
    "/images/Information.png",
  };

  public static final String[] MAIN_MENU_ITEMS_NCT = {
    "search_title",
    "favorites",
    "genres",
    "billboard",
    "new_playlists",
    "hot_playlists",
    "chat",
    "settings",
    "app_info",
  };

  public static final String[] MAIN_MENU_ICONS_SOUNDCLOUD = {
    "/images/Magnifier.png",
    "/images/Heart.png",
    "/images/Album.png",
    "/images/MessagingChat.png",
    "/images/Setting.png",
    "/images/Information.png",
  };

  public static final String[] MAIN_MENU_ITEMS_SOUNDCLOUD = {
    "search_title", "favorites", "discover_playlists", "chat", "settings", "app_info",
  };

  public static String[] getMenuIcons(String service) {
    return service.equals(Services.NCT) ? MAIN_MENU_ICONS_NCT : MAIN_MENU_ICONS_SOUNDCLOUD;
  }

  public static String[] getMenuItems(String service) {
    return service.equals(Services.NCT) ? MAIN_MENU_ITEMS_NCT : MAIN_MENU_ITEMS_SOUNDCLOUD;
  }

  private MenuConstants() {}
}
