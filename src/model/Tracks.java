package model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class Tracks {
  private Track[] tracks;
  private boolean hasMore;

  public Track[] getTracks() {
    return tracks;
  }

  public void setTracks(Track[] tracks) {
    this.tracks = tracks;
  }

  public boolean hasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public Tracks fromJSON(String jsonString) {
    if (jsonString == null || jsonString.trim().length() == 0) {
      return null;
    }
    try {
      JSONObject json = JSON.getObject(jsonString);
      if (json.has("GetMore")) {
        hasMore = json.getString("GetMore", "no").equals("yes");
      } else {
        hasMore = false;
      }
      if (json.has("Items")) {
        JSONArray jsonArray = json.getArray("Items");
        tracks = new Track[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
          JSONObject item = null;
          try {
            item = jsonArray.getObject(i);
          } catch (Exception e) {
            continue;
          }
          if (item == null) {
            continue;
          }
          tracks[i] =
              new Track(
                  item.getString("Key", ""),
                  item.getString("Name", ""),
                  item.getString("Url", ""),
                  item.getInt("Duration", 0),
                  item.getString("Singer", ""),
                  item.getString("Image", ""));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this;
  }
}
