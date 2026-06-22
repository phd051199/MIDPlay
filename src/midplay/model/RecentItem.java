package midplay.model;

import midplay.util.Lang;

public class RecentItem {
  public static final int TRACK = 0;
  public static final int FOLDER = 1;

  private final int type;
  private final Track track;
  private final Playlist folder;

  private RecentItem(int type, Track track, Playlist folder) {
    this.type = type;
    this.track = track;
    this.folder = folder;
  }

  public static RecentItem forTrack(Track track) {
    return new RecentItem(TRACK, track, null);
  }

  public static RecentItem forFolder(Playlist folder) {
    return new RecentItem(FOLDER, null, folder);
  }

  public int getType() {
    return type;
  }

  public Track getTrack() {
    return track;
  }

  public Playlist getFolder() {
    return folder;
  }

  public String getName() {
    if (type == TRACK && track != null) {
      return track.getDisplayTitle(Lang.tr("details.unknown_artist"));
    }
    if (type == FOLDER && folder != null) {
      return folder.getDisplayTitle();
    }
    return "";
  }
}
