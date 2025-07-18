package app.utils;

import app.models.Song;
import java.util.Vector;

public class SongPool {

  private static volatile SongPool instance;
  private static final Object instanceLock = new Object();

  public static SongPool getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new SongPool();
        }
      }
    }
    return instance;
  }

  private final Vector availableSongs;
  private final int maxPoolSize;

  private SongPool() {
    this.maxPoolSize = 100;
    this.availableSongs = new Vector();
  }

  public synchronized Song borrowSong() {
    if (availableSongs.size() > 0) {
      Song song = (Song) availableSongs.elementAt(availableSongs.size() - 1);
      availableSongs.removeElementAt(availableSongs.size() - 1);
      resetSong(song);
      return song;
    } else {
      return new Song();
    }
  }

  public synchronized void returnSong(Song song) {
    if (song != null) {
      if (availableSongs.size() < maxPoolSize) {
        resetSong(song);
        availableSongs.addElement(song);
      }
    }
  }

  private void resetSong(Song song) {
    song.setSongId("");
    song.setSongName("");
    song.setArtistName("");
    song.setStreamUrl("");
    song.setImage("");
    song.setDuration(0);
  }

  public synchronized int getAvailableCount() {
    return availableSongs.size();
  }

  public synchronized int getMaxPoolSize() {
    return maxPoolSize;
  }

  public synchronized void returnSongs(Vector songs) {
    if (songs != null) {
      for (int i = 0; i < songs.size(); i++) {
        Object obj = songs.elementAt(i);
        if (obj instanceof Song) {
          returnSong((Song) obj);
        }
      }
    }
  }

  public synchronized void clear() {
    availableSongs.removeAllElements();
  }

  public synchronized boolean isPoolFull() {
    return availableSongs.size() >= maxPoolSize;
  }

  public synchronized String getPoolStats() {
    return "Pool: " + availableSongs.size() + "/" + maxPoolSize + " available";
  }
}
