package model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class Playlists {
  private Playlist[] playlists;
  private boolean hasMore;

  public Playlist[] getPlaylists() {
    return playlists;
  }

  public void setPlaylists(Playlist[] playlists) {
    this.playlists = playlists;
  }

  public boolean hasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public void add(Playlists newPlaylists) {
    if (newPlaylists == null) {
      return;
    }
    Playlist[] newItems = newPlaylists.getPlaylists();
    if (newItems == null || newItems.length == 0) {
      this.hasMore = newPlaylists.hasMore();
      return;
    }
    if (playlists == null || playlists.length == 0) {
      this.playlists = new Playlist[newItems.length];
      System.arraycopy(newItems, 0, this.playlists, 0, newItems.length);
      this.hasMore = newPlaylists.hasMore();
      return;
    }
    int currentLength = this.playlists.length;
    int newLength = newItems.length;
    Playlist[] combinedPlaylists = new Playlist[currentLength + newLength];
    System.arraycopy(this.playlists, 0, combinedPlaylists, 0, currentLength);
    System.arraycopy(newItems, 0, combinedPlaylists, currentLength, newLength);
    this.playlists = combinedPlaylists;
    this.hasMore = newPlaylists.hasMore();
  }

  public Playlists fromJSON(String jsonString) {
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
        playlists = new Playlist[jsonArray.size()];
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
          playlists[i] =
              new Playlist(
                  item.getString("ListKey", ""),
                  item.getString("Name", ""),
                  item.getString("Image", ""));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this;
  }
}
