package midplay.model;

import cc.nnproject.json.JSONObject;
import java.util.Vector;

public class Playlists extends JsonListResult {
  private Playlist[] playlists = new Playlist[0];

  private static final JsonItemFactory PLAYLIST_FACTORY =
      new JsonItemFactory() {
        public Base create(JSONObject item) {
          return Playlist.fromJSON(item);
        }
      };

  public Playlist[] getPlaylists() {
    return playlists;
  }

  public void setPlaylists(Playlist[] playlists) {
    this.playlists = playlists != null ? playlists : new Playlist[0];
  }

  public int size() {
    return playlists.length;
  }

  protected JsonItemFactory factory() {
    return PLAYLIST_FACTORY;
  }

  protected void acceptItems(Vector items) {
    Playlist[] arr = new Playlist[items.size()];
    items.copyInto(arr);
    setPlaylists(arr);
  }

  public Playlists fromJSON(String jsonString) {
    parse(jsonString);
    return this;
  }

  public void add(Playlists newPlaylists) {
    if (newPlaylists == null) {
      return;
    }
    Playlist[] newItems = newPlaylists.getPlaylists();
    if (newItems == null || newItems.length == 0) {
      setHasMore(newPlaylists.hasMore());
      return;
    }
    if (playlists == null || playlists.length == 0) {
      this.playlists = new Playlist[newItems.length];
      System.arraycopy(newItems, 0, this.playlists, 0, newItems.length);
      setHasMore(newPlaylists.hasMore());
      return;
    }
    int currentLength = this.playlists.length;
    int newLength = newItems.length;
    Playlist[] combinedPlaylists = new Playlist[currentLength + newLength];
    System.arraycopy(this.playlists, 0, combinedPlaylists, 0, currentLength);
    System.arraycopy(newItems, 0, combinedPlaylists, currentLength, newLength);
    this.playlists = combinedPlaylists;
    setHasMore(newPlaylists.hasMore());
  }
}
