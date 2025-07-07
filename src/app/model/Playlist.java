package app.model;

import app.interfaces.JSONAble;
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
    try {
      JSONObject json = new JSONObject(jsonString);
      this.setId(json.getString("ListKey"));
      if (json.getString("Name").length() != 0) {
        String listName = json.getString("Name");
        this.setName(listName);
      } else {
        this.setName("MIDPlay");
      }
      this.setImageUrl(json.getString("Image"));
    } catch (Exception var4) {
    }
  }
}
