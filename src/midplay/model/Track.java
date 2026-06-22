package midplay.model;

import cc.nnproject.json.JSONObject;

public class Track extends Base {
  private String url;
  private int duration;
  private String artist;
  private String imageUrl;

  public Track() {
    super();
  }

  public Track(String key, String name, String url, int duration, String artist, String imageUrl) {
    super(key, name);
    this.url = url;
    this.duration = duration;
    this.artist = artist;
    this.imageUrl = imageUrl;
  }

  public boolean isSame(Track track) {
    if (track == null) {
      return false;
    }
    String thisKey = this.getKey();
    if (thisKey != null && thisKey.length() > 0 && thisKey.equals(track.getKey())) {
      return true;
    }
    return equalsNullable(this.getName(), track.getName())
        && equalsNullable(this.getArtist(), track.getArtist())
        && this.getDuration() == track.getDuration();
  }

  public static Track fromJSON(String jsonString) {
    return fromJSON(parseObject(jsonString));
  }

  public static Track fromJSON(JSONObject json) {
    if (json == null) {
      return null;
    }
    return new Track(
        json.getString("Key", ""),
        json.getString("Name", ""),
        json.getString("Url", ""),
        json.getInt("Duration", 0),
        json.getString("Singer", ""),
        json.getString("Image", ""));
  }

  public String getUrl() {
    return url;
  }

  public int getDuration() {
    return duration;
  }

  public String getArtist() {
    return artist;
  }

  public String getDisplayTitle(String unknownArtistLabel) {
    String normalizedArtist = normalizeDisplayText(getArtist());
    if (normalizedArtist.length() == 0) {
      normalizedArtist = normalizeDisplayText(unknownArtistLabel);
    }
    return buildDisplayTitle(normalizedArtist);
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public JSONObject toJSON() {
    JSONObject item = new JSONObject();
    item.put("Key", this.getKey());
    item.put("Name", this.getName());
    item.put("Url", this.getUrl());
    item.put("Duration", this.getDuration());
    item.put("Singer", this.getArtist());
    item.put("Image", this.getImageUrl());
    return item;
  }
}
