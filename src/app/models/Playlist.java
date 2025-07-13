package app.models;

import org.json.me.JSONObject;

public class Playlist implements JSONAble {

  private String id = "";
  private String name = "";
  private String imageUrl = "";

  public void setId(String _id) {
    this.id = _id;
  }

  public String getId() {
    return this.id;
  }

  public void setName(String _name) {
    this.name = _name;
  }

  public String getName() {
    return this.name;
  }

  public void setImageUrl(String _imageUrl) {
    this.imageUrl = _imageUrl;
  }

  public String getImageUrl() {
    return this.imageUrl;
  }

  public void fromJSON(String jsonString) {
    if (jsonString == null || jsonString.trim().length() == 0) {
      return;
    }

    try {
      JSONObject json = new JSONObject(jsonString);

      if (json.has("ListKey")) {
        String id = json.optString("ListKey", "");
        this.setId(id.length() > 0 ? id : "unknown");
      } else {
        this.setId("unknown");
      }

      if (json.has("Name")) {
        String name = json.optString("Name", "");
        this.setName(name.length() > 0 ? name : "MIDPlay");
      } else {
        this.setName("MIDPlay");
      }

      if (json.has("Image")) {
        String imageUrl = json.optString("Image", "");
        this.setImageUrl(imageUrl);
      }
    } catch (Exception e) {
    }
  }
}
