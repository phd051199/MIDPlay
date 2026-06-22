package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordStoreException;
import midplay.model.Playlist;
import midplay.model.Playlists;
import midplay.model.Track;
import midplay.model.Tracks;
import midplay.util.Utils;

public class FavoritesManager {
  private static FavoritesManager instance;

  public static final int SUCCESS = 0;
  public static final int ALREADY_EXISTS = 1;
  public static final int FAILED = 2;

  private static final int MAX_TRACKS_PER_PLAYLIST = 200;

  public static FavoritesManager getInstance() {
    if (instance == null) {
      instance = new FavoritesManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;
  private final RecordStoreManager tracksStorage;

  private FavoritesManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_FAVORITES);
    tracksStorage = new RecordStoreManager(Configuration.STORAGE_TRACKS);
  }

  public int addPlaylist(Playlist playlist) {
    if (playlist == null) {
      return FAILED;
    }
    try {
      if (findPlaylistRecord(playlist) != -1) {
        return ALREADY_EXISTS;
      }
      if (playlist.getId() == 0) {
        playlist.setId(System.currentTimeMillis());
      }
      String jsonString = playlist.toJSON().toString();
      storage.addRecord(jsonString);
      return SUCCESS;
    } catch (RecordStoreException e) {
      return FAILED;
    }
  }

  public boolean removePlaylistAndTracks(Playlist playlist) {
    if (playlist == null) {
      return false;
    }
    int recordId = findPlaylistRecord(playlist);
    if (recordId == -1) {
      return false;
    }
    try {
      storage.deleteRecord(recordId);
      if (playlist.isCustom()) {
        removeCustomPlaylistTracks(playlist.getKey());
      }
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  public boolean updatePlaylist(Playlist playlist) {
    if (playlist == null) {
      return false;
    }
    int recordId = findPlaylistRecord(playlist);
    if (recordId == -1) {
      return false;
    }
    try {
      storage.setRecord(recordId, playlist.toJSON().toString());
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  public Playlists getPlaylists() {
    final Vector playlistVector = new Vector();
    storage.forEachRecord(
        new RecordStoreManager.RecordHandler() {
          public boolean handle(int recordId, String data) {
            Playlist playlist = Playlist.fromJSON(data);
            if (playlist != null) {
              playlistVector.addElement(playlist);
            }
            return true;
          }
        });
    sortPlaylistsById(playlistVector);
    Playlist[] playlistArray = new Playlist[playlistVector.size()];
    for (int i = 0; i < playlistVector.size(); i++) {
      playlistArray[i] = (Playlist) playlistVector.elementAt(i);
    }
    Playlists result = new Playlists();
    result.setPlaylists(playlistArray);
    return result;
  }

  private static void sortPlaylistsById(Vector vector) {
    Utils.bubbleSort(
        vector,
        new Utils.Comparator() {
          public boolean shouldSwap(Object a, Object b) {
            return ((Playlist) a).getId() < ((Playlist) b).getId();
          }
        });
  }

  public void close() {
    storage.closeRecordStore();
    tracksStorage.closeRecordStore();
  }

  public int addTrackToPlaylist(Playlist playlist, Track track) {
    if (playlist == null || track == null || !playlist.isCustom()) {
      return FAILED;
    }
    try {
      TracksRecord record = loadTracksRecord(playlist.getKey());
      for (int i = 0; i < record.tracks.size(); i++) {
        Track existingTrack = (Track) record.tracks.elementAt(i);
        if (existingTrack.isSame(track)) {
          return ALREADY_EXISTS;
        }
      }
      if (record.tracks.size() >= MAX_TRACKS_PER_PLAYLIST) {
        return FAILED;
      }
      record.tracks.addElement(track);
      return savePlaylistTracks(playlist.getKey(), record.tracks, record.recordId)
          ? SUCCESS
          : FAILED;
    } catch (Exception e) {
      return FAILED;
    }
  }

  public boolean removeTrackFromPlaylist(Playlist playlist, Track track) {
    if (playlist == null || track == null || !playlist.isCustom()) {
      return false;
    }
    try {
      TracksRecord record = loadTracksRecord(playlist.getKey());
      for (int i = 0; i < record.tracks.size(); i++) {
        Track existingTrack = (Track) record.tracks.elementAt(i);
        if (existingTrack.isSame(track)) {
          record.tracks.removeElementAt(i);
          return savePlaylistTracks(playlist.getKey(), record.tracks, record.recordId);
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  public Tracks getCustomPlaylistTracks(Playlist playlist) {
    if (playlist == null || !playlist.isCustom()) {
      return null;
    }
    try {
      Vector trackList = loadTracksRecord(playlist.getKey()).tracks;
      Track[] trackArray = new Track[trackList.size()];
      for (int i = 0; i < trackList.size(); i++) {
        trackArray[i] = (Track) trackList.elementAt(i);
      }
      Tracks result = new Tracks();
      result.setTracks(trackArray);
      result.setHasMore(false);
      return result;
    } catch (Exception e) {
      return new Tracks();
    }
  }

  public int getCustomPlaylistTracksCount(Playlist playlist) {
    if (playlist == null || !playlist.isCustom()) {
      return 0;
    }
    return countTracks(playlist.getKey());
  }

  private static final class TracksRecord {
    int recordId = -1;
    final Vector tracks = new Vector();
  }

  private static final class TracksRecordEntry {
    final int recordId;
    final JSONObject json;

    TracksRecordEntry(int recordId, JSONObject json) {
      this.recordId = recordId;
      this.json = json;
    }
  }

  private TracksRecordEntry findTracksRecordByKey(final String playlistKey) {
    final TracksRecordEntry[] result = {null};
    tracksStorage.forEachRecord(
        new RecordStoreManager.RecordHandler() {
          public boolean handle(int recordId, String data) {
            if (playlistKey != null
                && playlistKey.length() > 0
                && data.indexOf(playlistKey) == -1) {
              return true;
            }
            JSONObject json = JSON.getObject(data);
            if (playlistKey.equals(json.getString("playlistKey", ""))) {
              result[0] = new TracksRecordEntry(recordId, json);
              return false;
            }
            return true;
          }
        });
    return result[0];
  }

  private TracksRecord loadTracksRecord(final String playlistKey) {
    TracksRecord record = new TracksRecord();
    TracksRecordEntry entry = findTracksRecordByKey(playlistKey);
    if (entry != null) {
      record.recordId = entry.recordId;
      JSONArray tracksArray = entry.json.getArray("tracks");
      for (int i = 0; i < tracksArray.size(); i++) {
        Track track = Track.fromJSON(tracksArray.getObject(i));
        if (track != null) {
          record.tracks.addElement(track);
        }
      }
    }
    return record;
  }

  private int countTracks(final String playlistKey) {
    TracksRecordEntry entry = findTracksRecordByKey(playlistKey);
    return entry == null ? 0 : entry.json.getArray("tracks").size();
  }

  private boolean savePlaylistTracks(String playlistKey, Vector trackList, int recordId) {
    try {
      JSONObject data = new JSONObject();
      data.put("playlistKey", playlistKey);
      JSONArray tracks = new JSONArray();
      for (int i = 0; i < trackList.size(); i++) {
        Track track = (Track) trackList.elementAt(i);
        if (track != null) {
          tracks.add(track.toJSON());
        }
      }
      data.put("tracks", tracks);
      if (recordId != -1) {
        tracksStorage.setRecord(recordId, data.toString());
      } else {
        tracksStorage.addRecord(data.toString());
      }
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  private void removeCustomPlaylistTracks(String playlistKey) {
    TracksRecordEntry entry = findTracksRecordByKey(playlistKey);
    if (entry != null) {
      try {
        tracksStorage.deleteRecord(entry.recordId);
      } catch (RecordStoreException e) {
      }
    }
  }

  private int findPlaylistRecord(final Playlist playlist) {
    final int[] found = {-1};
    storage.forEachRecord(
        new RecordStoreManager.RecordHandler() {
          public boolean handle(int recordId, String data) {
            Playlist storedPlaylist = Playlist.fromJSON(data);
            if (playlist.isSame(storedPlaylist)) {
              found[0] = recordId;
              return false;
            }
            return true;
          }
        });
    return found[0];
  }
}
