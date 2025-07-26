package model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;

public class Playlist extends Base {
  private String imageUrl;

  public Playlist() {
    super();
  }

  public Playlist(String key, String name, String imageUrl) {
    super(key, name);
    this.imageUrl = imageUrl;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public boolean isCustom() {
    return this.getKey().startsWith("custom_");
  }

  public Playlist fromJSON(String jsonString) {
    if (jsonString == null || jsonString.trim().length() == 0) {
      return null;
    }
    JSONObject json = JSON.getObject(jsonString);
    Playlist playlist =
        new Playlist(
            json.getString("ListKey", ""), json.getString("Name", ""), json.getString("Image", ""));
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
    item.put("Id", this.getId());
    return item;
  }
}
