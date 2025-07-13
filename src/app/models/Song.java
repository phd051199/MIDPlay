package app.models;

import app.utils.TextUtils;
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
    this.streamUrl = TextUtils.replace(streamUrl, " ", "%20");
    this.streamUrl = TextUtils.replace(this.streamUrl, "[", "%5B");
    this.streamUrl = TextUtils.replace(this.streamUrl, "]", "%5D");
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
    if (jsonString == null || jsonString.trim().length() == 0) {
      return;
    }

    try {
      JSONObject json = new JSONObject(jsonString);

      if (json.has("Name")) {
        String name = json.optString("Name", "");
        this.setSongName(name.length() > 0 ? name : "Unknown Song");
      } else {
        this.setSongName("Unknown Song");
      }

      if (json.has("Url")) {
        String url = json.optString("Url", "");
        if (url.length() > 0) {
          this.setStreamUrl(url);
        }
      }

      if (json.has("Singer")) {
        String artist = json.optString("Singer", "");
        this.setArtistName(artist.length() > 0 ? artist : "Unknown Artist");
      } else {
        this.setArtistName("Unknown Artist");
      }

      if (json.has("Duration")) {
        try {
          this.setDuration(json.getInt("Duration"));
        } catch (Exception e) {
          this.setDuration(0);
        }
      }

      if (json.has("Image")) {
        String image = json.optString("Image", "");
        this.setImage(image);
      }
    } catch (Exception e) {
    }
  }
}
