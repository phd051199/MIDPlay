import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;
import model.Playlist;
import model.Playlists;
import model.Track;
import model.Tracks;

public class FavoritesManager {
  private static FavoritesManager instance;

  public static final int SUCCESS = 0;
  public static final int ALREADY_EXISTS = 1;
  public static final int FAILED = 2;

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

  public boolean removePlaylistWithTracks(Playlist playlist) {
    return removePlaylistInternal(playlist, true);
  }

  public boolean removePlaylist(Playlist playlist) {
    return removePlaylistInternal(playlist, false);
  }

  private boolean removePlaylistInternal(Playlist playlist, boolean removeTracks) {
    if (playlist == null) {
      return false;
    }
    int recordId = findPlaylistRecord(playlist);
    if (recordId == -1) {
      return false;
    }
    try {
      storage.deleteRecord(recordId);
      if (removeTracks && playlist.isCustom()) {
        removeCustomPlaylistTracks(playlist.getKey());
      }
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  public Playlists getPlaylists() {
    Vector playlistVector = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String data = storage.getRecordAsString(enumeration.nextRecordId());
          Playlist playlist = new Playlist().fromJSON(data);
          if (playlist != null) {
            playlistVector.addElement(playlist);
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      return new Playlists();
    }
    MIDPlay.bubbleSort(playlistVector, 1);
    return vectorToPlaylists(playlistVector);
  }

  public void close() {
    storage.closeRecordStore();
    tracksStorage.closeRecordStore();
  }

  public int addTrackToCustomPlaylist(Playlist playlist, Track track) {
    if (playlist == null || track == null || !playlist.isCustom()) {
      return FAILED;
    }
    try {
      Vector trackList = getPlaylistTracksList(playlist.getKey());
      for (int i = 0; i < trackList.size(); i++) {
        Track existingTrack = (Track) trackList.elementAt(i);
        if (existingTrack.isSame(track)) {
          return ALREADY_EXISTS;
        }
      }
      trackList.addElement(track);
      return savePlaylistTracks(playlist.getKey(), trackList) ? SUCCESS : FAILED;
    } catch (Exception e) {
      return FAILED;
    }
  }

  public boolean removeTrackFromCustomPlaylist(Playlist playlist, Track track) {
    if (playlist == null || track == null || !playlist.isCustom()) {
      return false;
    }
    try {
      Vector trackList = getPlaylistTracksList(playlist.getKey());
      for (int i = 0; i < trackList.size(); i++) {
        Track existingTrack = (Track) trackList.elementAt(i);
        if (existingTrack.isSame(track)) {
          trackList.removeElementAt(i);
          return savePlaylistTracks(playlist.getKey(), trackList);
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
      Vector trackList = getPlaylistTracksList(playlist.getKey());
      return vectorToTracks(trackList);
    } catch (Exception e) {
      return new Tracks();
    }
  }

  public int getCustomPlaylistTracksCount(Playlist playlist) {
    if (playlist == null || !playlist.isCustom()) {
      return 0;
    }
    Vector trackList = getPlaylistTracksList(playlist.getKey());
    return trackList.size();
  }

  private Vector getPlaylistTracksList(String playlistKey) {
    Vector tracks = new Vector();
    try {
      RecordEnumeration enumeration = tracksStorage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String data = tracksStorage.getRecordAsString(enumeration.nextRecordId());
          JSONObject json = JSON.getObject(data);
          if (playlistKey.equals(json.getString("playlistKey", ""))) {
            JSONArray tracksArray = json.getArray("tracks");
            for (int i = 0; i < tracksArray.size(); i++) {
              Track track = new Track().fromJSON(tracksArray.getObject(i).toString());
              if (track != null) {
                tracks.addElement(track);
              }
            }
            break;
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return tracks;
  }

  private boolean savePlaylistTracks(String playlistKey, Vector trackList) {
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
      int recordId = findPlaylistTracksRecord(playlistKey);
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

  private int findPlaylistTracksRecord(String playlistKey) {
    try {
      RecordEnumeration enumeration = tracksStorage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = tracksStorage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          if (playlistKey.equals(json.getString("playlistKey", ""))) {
            return recordId;
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return -1;
  }

  private void removeCustomPlaylistTracks(String playlistKey) {
    int recordId = findPlaylistTracksRecord(playlistKey);
    if (recordId != -1) {
      try {
        tracksStorage.deleteRecord(recordId);
      } catch (RecordStoreException e) {
      }
    }
  }

  private int findPlaylistRecord(Playlist playlist) {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          Playlist storedPlaylist = new Playlist().fromJSON(data);
          if (playlist.isSame(storedPlaylist)) {
            return recordId;
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
    return -1;
  }

  private Playlists vectorToPlaylists(Vector playlistVector) {
    Playlists result = new Playlists();
    Playlist[] playlistArray = new Playlist[playlistVector.size()];
    for (int i = 0; i < playlistVector.size(); i++) {
      playlistArray[i] = (Playlist) playlistVector.elementAt(i);
    }
    result.setPlaylists(playlistArray);
    return result;
  }

  private Tracks vectorToTracks(Vector trackVector) {
    Tracks result = new Tracks();
    if (trackVector.isEmpty()) {
      result.setTracks(new Track[0]);
      result.setHasMore(false);
      return result;
    }
    Track[] trackArray = new Track[trackVector.size()];
    for (int i = 0; i < trackVector.size(); i++) {
      trackArray[i] = (Track) trackVector.elementAt(i);
    }
    result.setTracks(trackArray);
    result.setHasMore(false);
    return result;
  }
}
