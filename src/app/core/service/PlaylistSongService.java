package app.core.service;

import app.core.storage.RecordStoreManager;
import app.models.Song;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import org.json.me.JSONObject;

public class PlaylistSongService {

  private static PlaylistSongService instance;
  private static final String PLAYLIST_SONGS_STORE = "playlist_songs";

  public static synchronized PlaylistSongService getInstance() {
    if (instance == null) {
      instance = new PlaylistSongService();
    }
    return instance;
  }

  private PlaylistSongService() {}

  public void addSongToPlaylist(String playlistId, Song song) throws Exception {
    String songId = song.getStreamUrl();
    String relationId = playlistId + "_" + songId;

    JSONObject songJson = new JSONObject();
    songJson.put("relationId", relationId);
    songJson.put("playlistId", playlistId);
    songJson.put("songId", songId);
    songJson.put("name", song.getSongName());
    songJson.put("artist", song.getArtistName());
    songJson.put("image", song.getImage());
    songJson.put("streamUrl", song.getStreamUrl());
    songJson.put("duration", new Integer(song.getDuration()));

    RecordStoreManager songRecordStore = new RecordStoreManager(PLAYLIST_SONGS_STORE);
    try {
      songRecordStore.openRecordStore();
      songRecordStore.addRecord(songJson.toString());
    } finally {
      songRecordStore.closeRecordStore();
    }
  }

  public Vector loadSongsFromCustomPlaylist(String playlistId) throws Exception {
    Vector customSongs = new Vector();
    RecordStoreManager songRecordStore = new RecordStoreManager(PLAYLIST_SONGS_STORE);
    RecordEnumeration re = null;

    try {
      songRecordStore.openRecordStore();
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = songRecordStore.getRecordAsString(recordId);

        if (record.trim().length() == 0) {
          continue;
        }

        JSONObject songJson = new JSONObject(record);
        if (songJson.has("playlistId") && songJson.getString("playlistId").equals(playlistId)) {
          Song song = createSongFromJson(songJson);
          customSongs.addElement(song);
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      songRecordStore.closeRecordStore();
    }

    return customSongs;
  }

  public boolean removeSongFromPlaylist(String playlistId, String songId) throws Exception {
    if (playlistId == null
        || songId == null
        || playlistId.trim().length() == 0
        || songId.trim().length() == 0) {
      return false;
    }

    String relationId = playlistId + "_" + songId;
    RecordStoreManager songRecordStore = new RecordStoreManager(PLAYLIST_SONGS_STORE);
    RecordEnumeration re = null;
    boolean removed = false;

    try {
      songRecordStore.openRecordStore();
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = songRecordStore.getRecordAsString(recordId);

        if (record == null || record.trim().length() == 0) {
          continue;
        }

        try {
          JSONObject songJson = new JSONObject(record);
          if (songJson.has("relationId") && songJson.getString("relationId").equals(relationId)) {
            songRecordStore.deleteRecord(recordId);
            removed = true;
            break;
          }
        } catch (Exception jsonException) {
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      songRecordStore.closeRecordStore();
    }

    return removed;
  }

  public boolean isSongInPlaylist(String playlistId, String songId) throws Exception {
    String relationId = playlistId + "_" + songId;
    RecordStoreManager songRecordStore = new RecordStoreManager(PLAYLIST_SONGS_STORE);
    RecordEnumeration re = null;

    try {
      songRecordStore.openRecordStore();
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = songRecordStore.getRecordAsString(recordId);

        JSONObject songJson = new JSONObject(record);
        if (songJson.has("relationId") && songJson.getString("relationId").equals(relationId)) {
          return true;
        }
      }
      return false;
    } finally {
      if (re != null) {
        re.destroy();
      }
      songRecordStore.closeRecordStore();
    }
  }

  public int getPlaylistSongCount(String playlistId) throws Exception {
    int count = 0;
    RecordStoreManager songRecordStore = new RecordStoreManager(PLAYLIST_SONGS_STORE);
    RecordEnumeration re = null;

    try {
      songRecordStore.openRecordStore();
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = songRecordStore.getRecordAsString(recordId);

        JSONObject songJson = new JSONObject(record);
        if (songJson.has("playlistId") && songJson.getString("playlistId").equals(playlistId)) {
          count++;
        }
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      songRecordStore.closeRecordStore();
    }

    return count;
  }

  private Song createSongFromJson(JSONObject songJson) throws Exception {
    Song song = new Song();
    song.setSongName(songJson.getString("name"));
    song.setArtistName(songJson.getString("artist"));
    song.setImage(songJson.getString("image"));
    song.setStreamUrl(songJson.getString("streamUrl"));
    song.setDuration(songJson.getInt("duration"));
    return song;
  }

  public void removeAllSongsFromPlaylist(String playlistId) throws Exception {
    RecordStoreManager songRecordStore = new RecordStoreManager(PLAYLIST_SONGS_STORE);
    RecordEnumeration re = null;
    Vector recordsToDelete = new Vector();

    try {
      songRecordStore.openRecordStore();
      re = songRecordStore.enumerateRecords();

      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = songRecordStore.getRecordAsString(recordId);

        JSONObject songJson = new JSONObject(record);
        if (songJson.has("playlistId") && songJson.getString("playlistId").equals(playlistId)) {
          recordsToDelete.addElement(new Integer(recordId));
        }
      }

      for (int i = 0; i < recordsToDelete.size(); i++) {
        int recordId = ((Integer) recordsToDelete.elementAt(i)).intValue();
        songRecordStore.deleteRecord(recordId);
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
      songRecordStore.closeRecordStore();
    }
  }
}
