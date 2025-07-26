import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
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

  public static final int ADD_TRACK_SUCCESS = 0;
  public static final int ADD_TRACK_ALREADY_EXISTS = 1;
  public static final int ADD_TRACK_FAILED = 2;

  public static FavoritesManager getInstance() {
    if (instance == null) {
      instance = new FavoritesManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;
  private final RecordStoreManager tracksStorage;

  private FavoritesManager() {
    storage = new RecordStoreManager(Configuration.StorageKeys.FAVORITES);
    tracksStorage = new RecordStoreManager(Configuration.StorageKeys.TRACKS);
  }

  public boolean addFavorite(Playlist playlist) {
    if (playlist == null) {
      return false;
    }
    try {
      if (isFavorite(playlist)) {
        return false;
      }
      if (playlist.getId() == 0) {
        playlist.setId(System.currentTimeMillis());
      }
      String jsonString = playlist.toJSON().toString();
      storage.addRecord(jsonString);
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  public boolean removeFavorite(Playlist playlist) {
    if (playlist == null) {
      return false;
    }
    int recordId = findPlaylistRecord(playlist.getKey());
    if (recordId == -1) {
      return false;
    }
    try {
      storage.deleteRecord(recordId);
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  public boolean isFavorite(Playlist playlist) {
    return playlist != null && findPlaylistRecord(playlist.getKey()) != -1;
  }

  public Playlists getFavorites() {
    Vector playlistVector = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String recordData = storage.getRecordAsString(enumeration.nextRecordId());
          Playlist playlist = new Playlist().fromJSON(recordData);
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
      return ADD_TRACK_FAILED;
    }
    try {
      Vector trackList = getPlaylistTracksList(playlist.getKey());
      for (int i = 0; i < trackList.size(); i++) {
        Track existingTrack = (Track) trackList.elementAt(i);
        if (existingTrack != null && existingTrack.getName().equals(track.getName())) {
          return ADD_TRACK_ALREADY_EXISTS;
        }
      }
      trackList.addElement(track);
      return savePlaylistTracks(playlist.getKey(), trackList)
          ? ADD_TRACK_SUCCESS
          : ADD_TRACK_FAILED;
    } catch (Exception e) {
      return ADD_TRACK_FAILED;
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
        if (existingTrack != null && existingTrack.getKey().equals(track.getKey())) {
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
    } catch (Exception e) {
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
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  private int findPlaylistRecord(String playlistKey) {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String recordData = storage.getRecordAsString(recordId);
          Playlist storedPlaylist = new Playlist().fromJSON(recordData);
          if (storedPlaylist != null && playlistKey.equals(storedPlaylist.getKey())) {
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
