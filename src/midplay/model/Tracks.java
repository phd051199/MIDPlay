package midplay.model;

import cc.nnproject.json.JSONObject;
import java.util.Vector;

public class Tracks extends JsonListResult {
  private Track[] tracks = new Track[0];

  private static final JsonItemFactory TRACK_FACTORY =
      new JsonItemFactory() {
        public Base create(JSONObject item) {
          return Track.fromJSON(item);
        }
      };

  public Track[] getTracks() {
    return tracks;
  }

  public void setTracks(Track[] tracks) {
    this.tracks = tracks != null ? tracks : new Track[0];
  }

  public int size() {
    return tracks.length;
  }

  protected JsonItemFactory factory() {
    return TRACK_FACTORY;
  }

  protected void acceptItems(Vector items) {
    Track[] arr = new Track[items.size()];
    items.copyInto(arr);
    setTracks(arr);
  }

  public Tracks fromJSON(String jsonString) {
    parse(jsonString);
    return this;
  }
}
