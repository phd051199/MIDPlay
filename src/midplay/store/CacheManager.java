package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.rms.RecordStoreException;
import midplay.model.Track;
import midplay.util.Utils;

public class CacheManager {
  private static final String HEX_DIGITS = "0123456789ABCDEF";
  private static final int BUFFER_SIZE = 4096;

  private static CacheManager instance;

  public static CacheManager getInstance() {
    if (instance == null) {
      instance = new CacheManager();
    }
    return instance;
  }

  private final JsonRecordStore indexStorage;
  private final RecordStoreManager audioStorage;
  private JSONArray cachedEntries;

  private CacheManager() {
    indexStorage = new JsonRecordStore(Configuration.STORAGE_CACHE, 1, "[]");
    audioStorage = new RecordStoreManager(Configuration.STORAGE_CACHE_AUDIO);
  }

  private boolean cacheEnabled() {
    return isEnabled();
  }

  public boolean isEnabled() {
    return SettingsManager.getInstance().getCurrentCacheEnabled() == Configuration.CACHE_ON;
  }

  private String storageMode() {
    String mode = SettingsManager.getInstance().getCurrentCacheStorage();
    return Configuration.CACHE_STORAGE_FILE.equals(mode)
        ? Configuration.CACHE_STORAGE_FILE
        : Configuration.CACHE_STORAGE_RMS;
  }

  private JSONArray entries() {
    if (cachedEntries == null) {
      try {
        cachedEntries = JSON.getArray(indexStorage.load());
      } catch (Exception e) {
        cachedEntries = new JSONArray();
      }
    }
    return cachedEntries;
  }

  private static String urlKey(String url) {
    if (url == null) {
      return "";
    }
    return url;
  }

  // Simple stable string hash -> hex, so we get a safe file name for any URL.
  private static String fileNameFor(String url) {
    String key = urlKey(url);
    int hash = 0x811C9DC5;
    for (int i = 0; i < key.length(); i++) {
      hash ^= key.charAt(i) & 0xFF;
      hash *= 0x01000193;
    }
    if (hash < 0) {
      hash = -hash;
    }
    StringBuffer sb = new StringBuffer(16);
    int value = hash;
    for (int i = 0; i < 8; i++) {
      sb.append(HEX_DIGITS.charAt(value & 0xF));
      value >>>= 4;
    }
    return sb.toString() + ".mp3";
  }

  private JSONObject findEntry(String url) {
    JSONArray arr = entries();
    for (int i = 0; i < arr.size(); i++) {
      try {
        JSONObject entry = arr.getObject(i);
        if (urlKey(url).equals(entry.getString("url", ""))) {
          return entry;
        }
      } catch (Exception e) {
      }
    }
    return null;
  }

  public boolean isCached(Track track) {
    if (track == null || !cacheEnabled()) {
      return false;
    }
    JSONObject entry = findEntry(track.getUrl());
    if (entry == null) {
      return false;
    }
    String mode = storageMode();
    JSONObject found = findEntryInMode(track.getUrl(), mode);
    if (found == null) {
      return false;
    }
    // Verify the actual data is still readable.
    if (Configuration.CACHE_STORAGE_FILE.equals(mode)) {
      return fileExists(found.getString("location", ""));
    }
    return rmsExists(found.getInt("location", -1));
  }

  private JSONObject findEntryInMode(String url, String mode) {
    JSONArray arr = entries();
    for (int i = 0; i < arr.size(); i++) {
      try {
        JSONObject entry = arr.getObject(i);
        if (urlKey(url).equals(entry.getString("url", ""))
            && mode.equals(entry.getString("storage", ""))) {
          return entry;
        }
      } catch (Exception e) {
      }
    }
    return null;
  }

  public byte[] getTrackBytes(Track track) {
    if (track == null || !cacheEnabled()) {
      return null;
    }
    String mode = storageMode();
    JSONObject entry = findEntryInMode(track.getUrl(), mode);
    if (entry == null) {
      return null;
    }
    if (Configuration.CACHE_STORAGE_FILE.equals(mode)) {
      return readFile(entry.getString("location", ""));
    }
    return readRms(entry.getInt("location", -1));
  }

  public void cacheTrack(Track track, byte[] data) {
    if (track == null || data == null || data.length == 0) {
      return;
    }
    if (data.length > Configuration.MAX_CACHE_ENTRY_BYTES) {
      // Too large to hold safely on a CLDC device; skip caching.
      return;
    }
    String mode = storageMode();
    String url = track.getUrl();
    JSONObject stale = findEntryInMode(url, mode);
    try {
      if (Configuration.CACHE_STORAGE_FILE.equals(mode)) {
        String fileName = fileNameFor(url);
        writeFile(fileName, data);
        removeEntry(url, mode);
        addEntry(track, mode, fileName);
      } else {
        int recordId;
        if (stale != null) {
          recordId = stale.getInt("location", -1);
          audioStorage.setRecordBytes(recordId, data);
        } else {
          recordId = audioStorage.addRecordBytes(data);
        }
        removeEntry(url, mode);
        addEntry(track, mode, String.valueOf(recordId));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addEntry(Track track, String mode, String location) {
    JSONObject entry = new JSONObject();
    entry.put("url", track.getUrl());
    entry.put("name", track.getName());
    entry.put("artist", track.getArtist());
    entry.put("duration", track.getDuration());
    entry.put("image", track.getImageUrl() != null ? track.getImageUrl() : "");
    entry.put("storage", mode);
    entry.put("location", location);
    entries().add(entry);
    saveIndex();
  }

  private void removeEntry(String url, String mode) {
    JSONArray arr = entries();
    JSONArray next = new JSONArray();
    for (int i = 0; i < arr.size(); i++) {
      try {
        JSONObject e = arr.getObject(i);
        if (urlKey(url).equals(e.getString("url", ""))
            && mode.equals(e.getString("storage", ""))) {
          continue;
        }
        next.add(e);
      } catch (Exception ex) {
      }
    }
    cachedEntries = next;
  }

  public Track[] getAllCached() {
    String mode = storageMode();
    JSONArray arr = entries();
    Track[] result = new Track[arr.size()];
    int n = 0;
    for (int i = 0; i < arr.size(); i++) {
      try {
        JSONObject entry = arr.getObject(i);
        if (mode.equals(entry.getString("storage", ""))) {
          String url = entry.getString("url", "");
          String name = entry.getString("name", "");
          String artist = entry.getString("artist", "");
          int duration = entry.getInt("duration", 0);
          String image = entry.getString("image", "");
          result[n++] = new Track("", name, url, duration, artist, image);
        }
      } catch (Exception e) {
      }
    }
    if (n != result.length) {
      Track[] trimmed = new Track[n];
      System.arraycopy(result, 0, trimmed, 0, n);
      return trimmed;
    }
    return result;
  }

  public void removeTrack(Track track) {
    if (track == null) {
      return;
    }
    String mode = storageMode();
    JSONObject entry = findEntryInMode(track.getUrl(), mode);
    if (entry == null) {
      return;
    }
    try {
      if (Configuration.CACHE_STORAGE_FILE.equals(mode)) {
        deleteFile(entry.getString("location", ""));
      } else {
        audioStorage.deleteRecord(entry.getInt("location", -1));
      }
    } catch (Exception e) {
    }
    removeEntry(track.getUrl(), mode);
    saveIndex();
  }

  public void clearAll() {
    String mode = storageMode();
    JSONArray arr = entries();
    JSONArray next = new JSONArray();
    for (int i = 0; i < arr.size(); i++) {
      try {
        JSONObject entry = arr.getObject(i);
        if (mode.equals(entry.getString("storage", ""))) {
          if (Configuration.CACHE_STORAGE_FILE.equals(mode)) {
            deleteFile(entry.getString("location", ""));
          } else {
            audioStorage.deleteRecord(entry.getInt("location", -1));
          }
        } else {
          next.add(entry);
        }
      } catch (Exception e) {
      }
    }
    cachedEntries = next;
    saveIndex();
  }

  public int getSize() {
    return getAllCached().length;
  }

  private void saveIndex() {
    try {
      indexStorage.save(cachedEntries.toString());
    } catch (RecordStoreException e) {
    }
  }

  public void close() {
    indexStorage.close();
    audioStorage.closeRecordStore();
  }

  // --- FileConnection helpers -------------------------------------------------

  private boolean fileExists(String fileName) {
    FileConnection fc = null;
    try {
      String dir = SettingsManager.getInstance().getCurrentCacheDirectory();
      fc = (FileConnection) Connector.open(dir + fileName, Connector.READ);
      return fc.exists();
    } catch (IOException e) {
      return false;
    } catch (Exception e) {
      return false;
    } finally {
      closeQuietly(fc);
    }
  }

  private byte[] readFile(String fileName) {
    InputStream in = null;
    FileConnection fc = null;
    try {
      String dir = SettingsManager.getInstance().getCurrentCacheDirectory();
      fc = (FileConnection) Connector.open(dir + fileName, Connector.READ);
      if (!fc.exists()) {
        return null;
      }
      in = fc.openInputStream();
      long len = fc.fileSize();
      if (len > Configuration.MAX_CACHE_ENTRY_BYTES) {
        return null;
      }
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream((int) len);
      byte[] buffer = new byte[BUFFER_SIZE];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    } catch (Exception e) {
      return null;
    } finally {
      closeQuietly(in);
      closeQuietly(fc);
    }
  }

  private void writeFile(String fileName, byte[] data) {
    OutputStream out = null;
    FileConnection fc = null;
    try {
      String dir = SettingsManager.getInstance().getCurrentCacheDirectory();
      fc = (FileConnection) Connector.open(dir + fileName, Connector.WRITE);
      if (!fc.exists()) {
        fc.create();
      }
      out = fc.openOutputStream();
      out.write(data);
      out.flush();
    } catch (Exception e) {
    } finally {
      closeQuietly(out);
      closeQuietly(fc);
    }
  }

  private void deleteFile(String fileName) {
    FileConnection fc = null;
    try {
      String dir = SettingsManager.getInstance().getCurrentCacheDirectory();
      fc = (FileConnection) Connector.open(dir + fileName, Connector.WRITE);
      if (fc.exists()) {
        fc.delete();
      }
    } catch (Exception e) {
    } finally {
      closeQuietly(fc);
    }
  }

  // --- RMS helpers ------------------------------------------------------------

  private boolean rmsExists(int recordId) {
    if (recordId <= 0) {
      return false;
    }
    try {
      return audioStorage.getRecordBytes(recordId) != null;
    } catch (Exception e) {
      return false;
    }
  }

  private byte[] readRms(int recordId) {
    if (recordId <= 0) {
      return null;
    }
    try {
      return audioStorage.getRecordBytes(recordId);
    } catch (Exception e) {
      return null;
    }
  }

  private static void closeQuietly(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
      }
    }
  }

  private static void closeQuietly(OutputStream out) {
    if (out != null) {
      try {
        out.close();
      } catch (IOException e) {
      }
    }
  }

  private static void closeQuietly(FileConnection fc) {
    if (fc != null) {
      try {
        fc.close();
      } catch (IOException e) {
      }
    }
  }
}
