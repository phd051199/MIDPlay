package app.model;

import app.common.Common;
import app.interfaces.JSONAble;
import org.json.me.JSONObject;

public class Song implements JSONAble {

  private String songId = "";
  private String songName = "";
  private String artistName = "";
  private String streamUrl = "";
  private String image = "";
  private int duration = 0;

  public void setSongId(String songId) {
    this.songId = songId;
  }

  public String getSongId() {
    return this.songId;
  }

  public void setSongName(String songName) {
    this.songName = songName;
  }

  public String getSongName() {
    return this.songName;
  }

  public void setArtistName(String artistName) {
    this.artistName = artistName;
  }

  public String getArtistName() {
    return this.artistName;
  }

  public void setStreamUrl(String streamUrl) {
    this.streamUrl = Common.replace(streamUrl, " ", "%20");
    this.streamUrl = Common.replace(this.streamUrl, "[", "%5B");
    this.streamUrl = Common.replace(this.streamUrl, "]", "%5D");
  }

  public String getStreamUrl() {
    return this.streamUrl;
  }

  public int getDuration() {
    return this.duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getImage() {
    return this.image;
  }

  public void fromJSON(String jsonString) {
    try {
      JSONObject json = new JSONObject(jsonString);
      this.setSongName(json.getString("Name"));
      this.setStreamUrl(json.getString("Url"));
      this.setArtistName(json.getString("Singer"));
      this.setDuration(json.getInt("Duration"));
      this.setImage(json.getString("Image"));
    } catch (Exception var3) {
    }
  }
}
