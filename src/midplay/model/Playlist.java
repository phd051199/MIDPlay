package midplay.model;

import cc.nnproject.json.JSONObject;

public class Playlist extends Base {
  private String imageUrl;
  private String artist;

  public Playlist() {
    super();
  }

  public Playlist(String key, String name, String imageUrl) {
    this(key, name, imageUrl, "");
  }

  public Playlist(String key, String name, String imageUrl, String artist) {
    super(key, name);
    this.imageUrl = imageUrl;
    this.artist = artist;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getArtist() {
    return artist;
  }

  public String getDisplayTitle() {
    return buildDisplayTitle(getArtist());
  }

  public boolean isCustom() {
    return this.getKey() != null && this.getKey().startsWith("custom_");
  }

  public boolean isSame(Playlist playlist) {
    return playlist != null && equalsNullable(this.getKey(), playlist.getKey());
  }

  public static Playlist fromJSON(String jsonString) {
    return fromJSON(parseObject(jsonString));
  }

  public static Playlist fromJSON(JSONObject json) {
    if (json == null) {
      return null;
    }
    Playlist playlist =
        new Playlist(
            json.getString("ListKey", ""),
            json.getString("Name", ""),
            json.getString("Image", ""),
            json.getString("Singer", ""));
    if (json.has("Id")) {
      playlist.setId(json.getLong("Id", 0));
    }
    return playlist;
  }

  public JSONObject toJSON() {
    JSONObject item = new JSONObject();
    item.put("ListKey", this.getKey());
    item.put("Name", this.getName());
    item.put("Image", this.getImageUrl());
    item.put("Singer", this.getArtist());
    item.put("Id", this.getId());
    return item;
  }
}
