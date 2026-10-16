package midplay.store;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import midplay.model.Track;
import midplay.net.MediaHttpClient;
import midplay.net.Network;
import midplay.util.Utils;

public class DownloadManager {
  private static final int INDEX_RECORD_ID = 1;
  private static final int BUFFER_SIZE = 4096;
  private static final long DOWNLOAD_TIMEOUT_MS = 120000L;
  private static final int MAX_REDIRECTS = 5;

  private static DownloadManager instance;

  public static DownloadManager getInstance() {
    if (instance == null) {
      instance = new DownloadManager();
    }
    return instance;
  }

  public interface DownloadListener {
    void onStart();

    void onProgress(int percent);

    void onDone(String path);

    void onError(String message);
  }

  private RecordStore indexStore;
  private JSONArray downloads;
  private int indexRecordId = INDEX_RECORD_ID;
  private int backupRecordId = INDEX_RECORD_ID;
  private Vector diskPathsCache;
  private Hashtable linkedPathsCache;
  private final Vector activeKeys = new Vector();

  private DownloadManager() {}

  private synchronized RecordStore openIndexStore() throws RecordStoreException {
    if (indexStore == null) {
      indexStore = RecordStore.openRecordStore(Configuration.STORAGE_DOWNLOADS, true);
    }
    return indexStore;
  }

  private synchronized void closeIndexStore() {
    if (indexStore != null) {
      try {
        indexStore.closeRecordStore();
      } catch (RecordStoreException e) {
      } finally {
        indexStore = null;
      }
    }
  }

  private synchronized JSONArray getIndex() {
    if (downloads != null) {
      return downloads;
    }
    downloads = new JSONArray();
    try {
      RecordStore rs = openIndexStore();
      JSONArray main = readArray(rs, indexRecordId);
      if (main == null && indexRecordId != INDEX_RECORD_ID) {
        main = readArray(rs, INDEX_RECORD_ID);
        if (main != null) {
          indexRecordId = INDEX_RECORD_ID;
        }
      }
      if (main == null) {
        int found = findIndexRecordId(rs);
        if (found > 0) {
          indexRecordId = found;
          main = readArray(rs, found);
        }
      }
      if (main != null) {
        downloads = main;
      } else {
        JSONArray backup = readBackup();
        if (backup != null) {
          downloads = backup;
          saveIndex();
        }
      }
    } catch (Exception e) {
      downloads = new JSONArray();
    }
    return downloads;
  }

  private static JSONArray readArray(RecordStore rs, int id) {
    byte[] data = readRecord(rs, id);
    if (data == null || data.length == 0) {
      return null;
    }
    String json = Utils.bytesToUtf8(data);
    if (json.length() == 0) {
      return null;
    }
    try {
      return cc.nnproject.json.JSON.getArray(json);
    } catch (Exception e) {
      return null;
    }
  }

  private JSONArray readBackup() {
    RecordStore rs = null;
    try {
      rs = RecordStore.openRecordStore(Configuration.STORAGE_DOWNLOADS_BACKUP, false);
      JSONArray array = readArray(rs, backupRecordId);
      if (array == null && backupRecordId != INDEX_RECORD_ID) {
        array = readArray(rs, INDEX_RECORD_ID);
        if (array != null) {
          backupRecordId = INDEX_RECORD_ID;
        }
      }
      return array;
    } catch (Exception e) {
      return null;
    } finally {
      if (rs != null) {
        try {
          rs.closeRecordStore();
        } catch (RecordStoreException e) {
        }
      }
    }
  }

  private void writeBackup(String json) {
    byte[] bytes;
    try {
      bytes = Utils.utf8ToBytes(json);
    } catch (Exception e) {
      return;
    }
    RecordStore rs = null;
    try {
      rs = RecordStore.openRecordStore(Configuration.STORAGE_DOWNLOADS_BACKUP, true);
      int id = backupRecordId;
      try {
        rs.setRecord(id, bytes, 0, bytes.length);
      } catch (RecordStoreException e) {
        id = rs.addRecord(bytes, 0, bytes.length);
        backupRecordId = id;
      }
    } catch (RecordStoreException e) {
    } finally {
      if (rs != null) {
        try {
          rs.closeRecordStore();
        } catch (RecordStoreException e) {
        }
      }
    }
  }

  private static byte[] readRecord(RecordStore rs, int id) {
    try {
      return rs.getRecord(id);
    } catch (RecordStoreException e) {
      return null;
    }
  }

  private static int findIndexRecordId(RecordStore rs) {
    try {
      RecordEnumeration records = rs.enumerateRecords(null, null, false);
      int found = -1;
      while (records.hasNextElement()) {
        int id = records.nextRecordId();
        byte[] data = readRecord(rs, id);
        if (data == null || data.length == 0) {
          continue;
        }
        String json = Utils.bytesToUtf8(data);
        if (json.length() > 0 && json.charAt(0) == '[') {
          found = id;
        }
      }
      records.destroy();
      return found;
    } catch (Exception e) {
      return -1;
    }
  }

  private synchronized boolean saveIndex() {
    byte[] bytes;
    try {
      bytes = Utils.utf8ToBytes(downloads.toString());
    } catch (Exception e) {
      return false;
    }
    try {
      RecordStore rs = openIndexStore();
      int id = indexRecordId;
      try {
        rs.setRecord(id, bytes, 0, bytes.length);
      } catch (RecordStoreException e) {
        id = rs.addRecord(bytes, 0, bytes.length);
        indexRecordId = id;
      }
      if (!verifySaved(rs, id, bytes)) {
        return false;
      }
      closeIndexStore();
      writeBackup(downloads.toString());
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  private static boolean verifySaved(RecordStore rs, int id, byte[] bytes) {
    try {
      byte[] stored = rs.getRecord(id);
      if (stored == null || stored.length != bytes.length) {
        return false;
      }
      for (int i = 0; i < bytes.length; i++) {
        if (stored[i] != bytes[i]) {
          return false;
        }
      }
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  private synchronized void reloadIndex() {
    downloads = null;
    getIndex();
  }

  public static String makeTrackKey(Track track) {
    if (track == null) {
      return null;
    }
    String key = track.getKey();
    if (key != null && key.length() > 0) {
      return sanitize(key);
    }
    String name = track.getName() != null ? track.getName() : "";
    String artist = track.getArtist() != null ? track.getArtist() : "";
    String raw = name + "_" + artist + "_" + track.getDuration();
    return sanitize(raw);
  }

  private static String sanitize(String key) {
    StringBuffer sb = new StringBuffer(key.length());
    for (int i = 0; i < key.length() && sb.length() < 80; i++) {
      char c = key.charAt(i);
      if ((c >= 'A' && c <= 'Z')
          || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9')
          || c == '-'
          || c == '_') {
        sb.append(c);
      } else {
        sb.append('_');
      }
    }
    if (sb.length() == 0) {
      sb.append("track");
    }
    return sb.toString();
  }

  private String fileNameFor(Track track) {
    return makeTrackKey(track) + ".mp3";
  }

  public boolean isDownloaded(Track track) {
    if (track == null) {
      return false;
    }
    String key = makeTrackKey(track);
    String url = track.getUrl();
    if (key == null) {
      return false;
    }
    JSONArray index = getIndex();
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        if (key.equals(entry.getString("key", ""))
            || (url != null && url.length() > 0 && url.equals(entry.getString("url", "")))) {
          String path = entry.getString("path", "");
          if (path.length() > 0) {
            return exists(path);
          }
        }
      } catch (Exception e) {
      }
    }
    return false;
  }

  public String getDownloadedFilePath(Track track) {
    if (track == null) {
      return null;
    }
    String url = track.getUrl();
    String key = makeTrackKey(track);
    JSONArray index = getIndex();
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        String indexedUrl = entry.getString("url", "");
        // Downloads made before URL indexing used the track key, so retain that lookup.
        boolean sameUrl = url != null && url.length() > 0 && url.equals(indexedUrl);
        String path = entry.getString("path", "");
        boolean samePath = url != null && url.length() > 0 && url.equals(path);
        String indexedKey = entry.getString("key", "");
        boolean sameKey = key != null && indexedKey != null && key.equals(indexedKey);
        if (sameUrl || samePath || sameKey) {
          if (path.length() > 0 && exists(path)) {
            return path;
          }
        }
      } catch (Exception e) {
      }
    }
    return null;
  }

  public boolean isDownloadActive(Track track) {
    String key = makeTrackKey(track);
    if (key == null) {
      return false;
    }
    synchronized (activeKeys) {
      return activeKeys.contains(key);
    }
  }

  private void markActive(String key) {
    if (key == null) {
      return;
    }
    synchronized (activeKeys) {
      if (!activeKeys.contains(key)) {
        activeKeys.addElement(key);
      }
    }
  }

  private void unmarkActive(String key) {
    if (key == null) {
      return;
    }
    synchronized (activeKeys) {
      activeKeys.removeElement(key);
    }
  }

  public void downloadAsync(final Track track, final DownloadListener listener) {
    if (track == null) {
      notifyError(listener, "No track");
      return;
    }
    final String key = makeTrackKey(track);
    if (isDownloaded(track)) {
      notifyDone(listener, getDownloadedFilePath(track));
      return;
    }
    if (isDownloadActive(track)) {
      notifyError(listener, "Download already in progress");
      return;
    }
    synchronized (activeKeys) {
      markActive(key);
    }
    Thread t =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  performDownload(track, key, listener);
                } finally {
                  unmarkActive(key);
                }
              }
            });
    t.start();
  }

  private void performDownload(Track track, String key, DownloadListener listener) {
    String basePath = SettingsManager.getInstance().getCurrentDownloadPath();
    if (basePath == null || basePath.length() == 0) {
      basePath = Configuration.DEFAULT_DOWNLOAD_PATH;
    }
    if (!basePath.endsWith("/")) {
      basePath = basePath + "/";
    }
    String filePath = basePath + fileNameFor(track);

    HttpConnection connection = null;
    InputStream in = null;
    FileConnection file = null;
    OutputStream out = null;
    TimerTask watchdog = null;
    boolean completed = false;
    try {
      if (listener != null) {
        listener.onStart();
      }
      if (!ensureDirectories(basePath)) {
        notifyError(listener, "Unable to create download directory");
        return;
      }

      String finalUrl = resolveUrl(track.getUrl());
      if (finalUrl == null || finalUrl.length() == 0) {
        notifyError(listener, "Unable to resolve download URL");
        return;
      }

      connection = Network.openConnection(finalUrl);
      if (connection == null) {
        notifyError(listener, "Failed to open download connection");
        return;
      }
      connection.setRequestMethod(HttpConnection.GET);
      watchdog = Network.armPlaybackWatchdog(connection, DOWNLOAD_TIMEOUT_MS);

      int responseCode = connection.getResponseCode();
      if (responseCode != HttpConnection.HTTP_OK && responseCode != HttpConnection.HTTP_PARTIAL) {
        notifyError(listener, "HTTP error: " + responseCode);
        return;
      }

      long total = -1L;
      try {
        total = connection.getLength();
      } catch (Exception e) {
      }

      in = connection.openInputStream();
      file = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);
      if (file.exists()) {
        file.delete();
      }
      file.create();
      out = file.openOutputStream();

      byte[] buffer = new byte[BUFFER_SIZE];
      long written = 0L;
      int read;
      while ((read = in.read(buffer)) != -1) {
        if (read > 0) {
          out.write(buffer, 0, read);
          written += read;
        }
        if (total > 0 && listener != null) {
          listener.onProgress(percent(written, total));
        }
      }
      out.flush();
      out.close();
      out = null;
      file.close();
      file = null;

      addToIndex(track, key, filePath);
      completed = true;
      notifyDone(listener, filePath);
    } catch (Exception e) {
      if (!completed) {
        deleteFileIfExists(filePath);
        notifyError(listener, errorMessage(e));
      }
    } finally {
      if (watchdog != null) {
        watchdog.cancel();
      }
      Utils.closeQuietly(in);
      Utils.closeQuietly(out);
      closeConnection(file);
      Utils.closeQuietly(connection);
    }
  }

  private static int percent(long part, long total) {
    if (total <= 0) {
      return -1;
    }
    int p = (int) ((part * 100L) / total);
    if (p > 100) {
      p = 100;
    }
    if (p < 0) {
      p = 0;
    }
    return p;
  }

  private String resolveUrl(String url) {
    if (url == null || url.length() == 0) {
      return null;
    }
    MediaHttpClient client = new MediaHttpClient();
    try {
      return client.resolveRedirect(
          url,
          0,
          new MediaHttpClient.ResolveContext() {
            public boolean isSessionActive(int sessionId) {
              return true;
            }

            public void onResolveProgress(int progress, String status, int sessionId) {}
          });
    } catch (IOException e) {
      return null;
    }
  }

  private void addToIndex(Track track, String key, String filePath) {
    invalidateListCaches();
    JSONArray index = getIndex();
    try {
      String url = track.getUrl() != null ? track.getUrl() : "";
      String thumb = track.getImageUrl() != null ? track.getImageUrl() : "";
      String name = track.getName() != null ? track.getName() : "";
      String artist = track.getArtist() != null ? track.getArtist() : "";
      for (int i = 0; i < index.size(); i++) {
        JSONObject entry = index.getObject(i);
        if (url.equals(entry.getString("url", "")) || key.equals(entry.getString("key", ""))) {
          entry.put("path", filePath);
          entry.put("url", url);
          if (thumb.length() > 0) {
            entry.put("thumb", thumb);
          }
          if (name.length() > 0) {
            entry.put("name", name);
          }
          if (artist.length() > 0) {
            entry.put("artist", artist);
          }
          if (saveIndex()) {
            return;
          }
          reloadIndex();
          return;
        }
      }
      JSONObject entry = new JSONObject();
      entry.put("key", key);
      entry.put("url", url);
      entry.put("name", name);
      entry.put("artist", artist);
      entry.put("path", filePath);
      entry.put("thumb", thumb);
      index.add(entry);
      if (!saveIndex()) {
        reloadIndex();
      }
    } catch (Exception e) {
      reloadIndex();
    }
  }

  public boolean linkLocalFile(String path, String url, String thumb, String name, String artist) {
    invalidateListCaches();
    if (path == null || path.length() == 0 || !exists(path)) {
      return false;
    }
    if (url == null) {
      url = "";
    }
    if (thumb == null) {
      thumb = "";
    }
    if (name == null) {
      name = "";
    }
    if (artist == null) {
      artist = "";
    }
    String fileName = fileNameForPath(path);
    if (name.length() == 0) {
      name = baseNameOf(fileName);
    }
    JSONArray index = getIndex();
    try {
      for (int i = 0; i < index.size(); i++) {
        JSONObject entry = index.getObject(i);
        String entryPath = entry.getString("path", "");
        String entryUrl = entry.getString("url", "");
        if (path.equals(entryPath) || (url.length() > 0 && url.equals(entryUrl))) {
          entry.put("path", path);
          if (url.length() > 0) {
            entry.put("url", url);
          }
          if (thumb.length() > 0) {
            entry.put("thumb", thumb);
          }
          if (name.length() > 0) {
            entry.put("name", name);
          }
          if (artist.length() > 0) {
            entry.put("artist", artist);
          }
          if (entry.getString("key", "").length() == 0) {
            entry.put("key", linkKey(url, name, artist, fileName));
          }
          if (saveIndex()) {
            return true;
          }
          reloadIndex();
          return false;
        }
      }
      JSONObject entry = new JSONObject();
      entry.put("key", linkKey(url, name, artist, fileName));
      entry.put("url", url);
      entry.put("name", name);
      entry.put("artist", artist);
      entry.put("path", path);
      entry.put("thumb", thumb);
      index.add(entry);
      if (saveIndex()) {
        return true;
      }
      reloadIndex();
      return false;
    } catch (Exception e) {
      reloadIndex();
      return false;
    }
  }

  private static String linkKey(String url, String name, String artist, String fileName) {
    if (url != null && url.length() > 0) {
      return sanitize(url);
    }
    if (name != null && name.length() > 0) {
      return sanitize(name + "_" + (artist != null ? artist : ""));
    }
    return sanitize(baseNameOf(fileName));
  }

  private static String baseNameOf(String fileName) {
    if (fileName == null) {
      return "";
    }
    int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }

  public String fileNameForPath(String path) {
    String dir = getDownloadDirectory();
    if (path != null && path.startsWith(dir)) {
      return path.substring(dir.length());
    }
    if (path == null) {
      return "";
    }
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }

  public JSONObject getLinkedEntry(String path) {
    if (path == null) {
      return null;
    }
    JSONArray index = getIndex();
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        if (path.equals(entry.getString("path", ""))) {
          return entry;
        }
      } catch (Exception e) {
      }
    }
    return null;
  }

  public Hashtable getLinkedPaths() {
    Hashtable cached = linkedPathsCache;
    if (cached != null) {
      return cached;
    }
    healIndex(diskPaths());
    Hashtable table = new Hashtable();
    JSONArray index = getIndex();
    for (int i = 0; i < index.size(); i++) {
      try {
        String path = index.getObject(i).getString("path", "");
        if (path.length() > 0) {
          table.put(path, "1");
        }
      } catch (Exception e) {
      }
    }
    linkedPathsCache = table;
    return table;
  }

  public String suggestedKeyword(String path) {
    String name = baseNameOf(fileNameForPath(path));
    StringBuffer sb = new StringBuffer(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c == '_' || c == '-' || c == '.' || c == ' ') {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
          sb.append(' ');
        }
      } else {
        sb.append(c);
      }
    }
    while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  public String[] getLocalFilePaths() {
    Vector paths = diskPaths();
    String[] result = new String[paths.size()];
    paths.copyInto(result);
    return result;
  }

  public boolean removeDownload(Track track) {
    invalidateListCaches();
    if (track == null) {
      return false;
    }
    String key = makeTrackKey(track);
    String trackPath = track.getUrl();
    JSONArray index = getIndex();
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        String path = entry.getString("path", "");
        if (path.equals(trackPath)
            || ((trackPath == null || !trackPath.startsWith("file://"))
                && key.equals(entry.getString("key", "")))) {
          if (path.length() > 0) {
            deleteFileIfExists(path);
          }
          rebuildWithout(i);
          return true;
        }
      } catch (Exception e) {
      }
    }
    if (trackPath != null && trackPath.length() > 0 && exists(trackPath)) {
      deleteFileIfExists(trackPath);
      return true;
    }
    return false;
  }

  private void rebuildWithout(int removedIndex) {
    invalidateListCaches();
    JSONArray current = getIndex();
    JSONArray rebuilt = new JSONArray();
    for (int i = 0; i < current.size(); i++) {
      if (i == removedIndex) {
        continue;
      }
      try {
        rebuilt.add(current.getObject(i));
      } catch (Exception e) {
      }
    }
    downloads = rebuilt;
    if (!saveIndex()) {
      reloadIndex();
    }
  }

  public void clearAllDownloads() {
    invalidateListCaches();
    JSONArray index = getIndex();
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        String path = entry.getString("path", "");
        if (path.length() > 0) {
          deleteFileIfExists(path);
        }
      } catch (Exception e) {
      }
    }
    downloads = new JSONArray();
    if (!saveIndex()) {
      reloadIndex();
    }
    deleteFilesInDownloadDirectory();
  }

  public Track[] refreshDownloadedTracks() {
    JSONArray index = getIndex();
    Vector trackList = new Vector();
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        String path = entry.getString("path", "");
        if (path.length() == 0) {
          continue;
        }
        trackList.addElement(
            new Track(
                entry.getString("key", ""),
                entry.getString("name", ""),
                path,
                0,
                entry.getString("artist", ""),
                entry.getString("thumb", "")));
      } catch (Exception e) {
      }
    }
    Track[] result = new Track[trackList.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = (Track) trackList.elementAt(i);
    }
    return result;
  }

  private void healIndex(Vector diskPaths) {
    if (diskPaths == null || diskPaths.size() == 0) {
      return;
    }
    JSONArray index = getIndex();
    boolean changed = false;
    for (int i = 0; i < index.size(); i++) {
      try {
        JSONObject entry = index.getObject(i);
        String path = entry.getString("path", "");
        if (path.length() == 0 || diskPaths.contains(path)) {
          continue;
        }
        String name = fileNameForPath(path);
        if (name.length() == 0) {
          continue;
        }
        for (int j = 0; j < diskPaths.size(); j++) {
          String diskPath = (String) diskPaths.elementAt(j);
          if (name.equals(fileNameForPath(diskPath))) {
            entry.put("path", diskPath);
            changed = true;
            break;
          }
        }
      } catch (Exception e) {
      }
    }
    if (changed) {
      invalidateListCaches();
      if (!saveIndex()) {
        reloadIndex();
      }
    }
  }

  private Vector diskPaths() {
    Vector cached = diskPathsCache;
    if (cached == null) {
      cached = getDownloadFilePaths();
      diskPathsCache = cached;
    }
    return cached;
  }

  public void invalidateListCaches() {
    diskPathsCache = null;
    linkedPathsCache = null;
  }

  private Vector getDownloadFilePaths() {
    Vector paths = new Vector();
    String basePath = getDownloadDirectory();
    FileConnection directory = null;
    try {
      directory = (FileConnection) Connector.open(basePath, Connector.READ);
      if (!directory.exists() || !directory.isDirectory()) {
        return paths;
      }
      Enumeration files = directory.list();
      while (files.hasMoreElements()) {
        String fileName = (String) files.nextElement();
        if (!fileName.endsWith("/")) {
          paths.addElement(basePath + fileName);
        }
      }
    } catch (Exception e) {
    } finally {
      closeConnection(directory);
    }
    return paths;
  }

  private void deleteFilesInDownloadDirectory() {
    String basePath = getDownloadDirectory();
    FileConnection directory = null;
    try {
      directory = (FileConnection) Connector.open(basePath, Connector.READ);
      if (!directory.exists() || !directory.isDirectory()) {
        return;
      }
      Enumeration files = directory.list();
      while (files.hasMoreElements()) {
        String fileName = (String) files.nextElement();
        if (!fileName.endsWith("/")) {
          deleteFileIfExists(basePath + fileName);
        }
      }
    } catch (Exception e) {
    } finally {
      closeConnection(directory);
    }
  }

  private String getDownloadDirectory() {
    String path = SettingsManager.getInstance().getCurrentDownloadPath();
    if (path == null || path.length() == 0) {
      path = Configuration.DEFAULT_DOWNLOAD_PATH;
    }
    return path.endsWith("/") ? path : path + "/";
  }

  private boolean ensureDirectories(String basePath) {
    String rest = basePath;
    String prefix = "";
    if (rest.startsWith("file:///")) {
      rest = rest.substring(8);
      prefix = "file:///";
    } else if (rest.startsWith("file://")) {
      rest = rest.substring(7);
      prefix = "file:///";
    }
    String cumulative = prefix;
    StringBuffer token = new StringBuffer();
    for (int i = 0; i < rest.length(); i++) {
      char c = rest.charAt(i);
      if (c == '/') {
        if (token.length() > 0) {
          cumulative = cumulative + token.toString() + "/";
          if (!ensureDirExists(cumulative)) {
            return false;
          }
          token.setLength(0);
        }
      } else {
        token.append(c);
      }
    }
    if (token.length() > 0) {
      cumulative = cumulative + token.toString() + "/";
      if (!ensureDirExists(cumulative)) {
        return false;
      }
    }
    return true;
  }

  private boolean ensureDirExists(String dirUrl) {
    FileConnection conn = null;
    try {
      conn = (FileConnection) Connector.open(dirUrl, Connector.READ_WRITE);
      if (!conn.exists()) {
        conn.mkdir();
      }
      conn.close();
      conn = null;
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      closeConnection(conn);
    }
  }

  private static boolean exists(String path) {
    FileConnection conn = null;
    try {
      conn = (FileConnection) Connector.open(path, Connector.READ);
      return conn.exists();
    } catch (Exception e) {
      return false;
    } finally {
      closeConnection(conn);
    }
  }

  private static void deleteFileIfExists(String path) {
    FileConnection conn = null;
    try {
      conn = (FileConnection) Connector.open(path, Connector.READ_WRITE);
      if (conn.exists()) {
        conn.delete();
      }
    } catch (Exception e) {
    } finally {
      closeConnection(conn);
    }
  }

  private static void closeConnection(FileConnection conn) {
    if (conn != null) {
      try {
        conn.close();
      } catch (Exception e) {
      }
    }
  }

  private static String errorMessage(Exception e) {
    if (e == null) {
      return "Download failed";
    }
    String msg = e.getMessage();
    if (msg == null || msg.length() == 0) {
      msg = e.toString();
    }
    return msg;
  }

  private static void notifyDone(DownloadListener listener, String path) {
    if (listener != null) {
      listener.onDone(path);
    }
  }

  private static void notifyError(DownloadListener listener, String message) {
    if (listener != null) {
      listener.onError(message);
    }
  }

  public void cleanup() {
    closeIndexStore();
  }
}
