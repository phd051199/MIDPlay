package model;

import cc.nnproject.json.JSON;
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
    return track != null
        && equalsNullable(this.getName(), track.getName())
        && equalsNullable(this.getArtist(), track.getArtist())
        && this.getDuration() == track.getDuration();
  }

  private boolean equalsNullable(String left, String right) {
    if (left == null) {
      return right == null;
    }
    return left.equals(right);
  }

  public Track fromJSON(String jsonString) {
    if (jsonString == null || jsonString.trim().length() == 0) {
      return null;
    }
    JSONObject json = JSON.getObject(jsonString);
    Track track =
        new Track(
            json.getString("Key", ""),
            json.getString("Name", ""),
            json.getString("Url", ""),
            json.getInt("Duration", 0),
            json.getString("Singer", ""),
            json.getString("Image", ""));
    return track;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    this.artist = artist;
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

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
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
