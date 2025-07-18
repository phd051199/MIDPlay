package app.utils;

import app.models.Playlist;
import java.util.Vector;

public class PlaylistPool {

  private static volatile PlaylistPool instance;
  private static final Object instanceLock = new Object();

  public static PlaylistPool getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new PlaylistPool();
        }
      }
    }
    return instance;
  }

  private final Vector availablePlaylists;
  private final int maxPoolSize;

  private PlaylistPool() {
    this.maxPoolSize = 100;
    this.availablePlaylists = new Vector();
  }

  public synchronized Playlist borrowPlaylist() {
    if (availablePlaylists.size() > 0) {
      Playlist playlist = (Playlist) availablePlaylists.elementAt(availablePlaylists.size() - 1);
      availablePlaylists.removeElementAt(availablePlaylists.size() - 1);
      resetPlaylist(playlist);
      return playlist;
    } else {
      return new Playlist();
    }
  }

  public synchronized void returnPlaylist(Playlist playlist) {
    if (playlist != null) {
      if (availablePlaylists.size() < maxPoolSize) {
        resetPlaylist(playlist);
        availablePlaylists.addElement(playlist);
      }
    }
  }

  private void resetPlaylist(Playlist playlist) {
    playlist.setId("");
    playlist.setName("");
    playlist.setImageUrl("");
  }

  public synchronized int getAvailableCount() {
    return availablePlaylists.size();
  }

  public synchronized int getMaxPoolSize() {
    return maxPoolSize;
  }

  public synchronized void returnPlaylists(Vector playlists) {
    if (playlists != null) {
      for (int i = 0; i < playlists.size(); i++) {
        Object obj = playlists.elementAt(i);
        if (obj instanceof Playlist) {
          returnPlaylist((Playlist) obj);
        }
      }
    }
  }

  public synchronized void clear() {
    availablePlaylists.removeAllElements();
  }

  public synchronized boolean isPoolFull() {
    return availablePlaylists.size() >= maxPoolSize;
  }

  public synchronized String getPoolStats() {
    return "PlaylistPool: " + availablePlaylists.size() + "/" + maxPoolSize + " available";
  }
}
