package model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.util.Vector;

public class Tracks {
  private Track[] tracks = new Track[0];
  private boolean hasMore;

  public Track[] getTracks() {
    return tracks;
  }

  public void setTracks(Track[] tracks) {
    this.tracks = tracks != null ? tracks : new Track[0];
  }

  public boolean hasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public Tracks fromJSON(String jsonString) {
    if (jsonString == null || jsonString.trim().length() == 0) {
      tracks = new Track[0];
      hasMore = false;
      return this;
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
        Vector trackList = new Vector();
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
          Track track = new Track().fromJSON(item.toString());
          if (track != null) {
            trackList.addElement(track);
          }
        }
        tracks = new Track[trackList.size()];
        trackList.copyInto(tracks);
      } else {
        tracks = new Track[0];
      }
    } catch (Exception e) {
      e.printStackTrace();
      tracks = new Track[0];
      hasMore = false;
    }
    return this;
  }
}
