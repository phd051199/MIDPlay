package app.core.settings;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;
import org.json.me.JSONObject;

public class SettingsStore {

  private final Hashtable openStores = new Hashtable();
  private String cachedEncoding = null;

  public SettingsStore() {

    detectEncoding();
  }

  private void detectEncoding() {
    if (cachedEncoding == null) {
      try {
        "test".getBytes("UTF-8");
        cachedEncoding = "UTF-8";
      } catch (UnsupportedEncodingException e) {
        cachedEncoding = "ISO-8859-1";
      }
    }
  }

  public JSONObject loadSettings(int category) {
    RecordStore rs = null;
    try {
      rs = openRecordStore(SettingsManager.getStoreName(category));
      RecordEnumeration re = rs.enumerateRecords(null, null, false);

      if (re.hasNextElement()) {
        byte[] recordBytes = re.nextRecord();
        String settingsJson = decodeRecordData(recordBytes);
        re.destroy();
        return new JSONObject(settingsJson);
      }

      if (re != null) {
        re.destroy();
      }
      return null;

    } catch (Exception e) {
      return null;
    } finally {
      closeRecordStore(rs);
    }
  }

  public void saveSettings(int category, JSONObject settings)
      throws RecordStoreException, UnsupportedEncodingException {
    RecordStore rs = null;
    try {
      rs = openRecordStore(SettingsManager.getStoreName(category));
      byte[] recordBytes = settings.toString().getBytes(cachedEncoding);

      RecordEnumeration re = rs.enumerateRecords(null, null, false);
      if (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        rs.setRecord(recordId, recordBytes, 0, recordBytes.length);
      } else {
        rs.addRecord(recordBytes, 0, recordBytes.length);
      }
      re.destroy();

    } finally {
      closeRecordStore(rs);
    }
  }

  public void writeRecord(String storeName, String data)
      throws RecordStoreException, UnsupportedEncodingException {
    if (data == null) {
      throw new IllegalArgumentException("Input string cannot be null");
    }

    RecordStore rs = null;
    try {
      rs = openRecordStore(storeName);
      byte[] rec = data.getBytes(cachedEncoding);
      rs.addRecord(rec, 0, rec.length);
    } finally {
      closeRecordStore(rs);
    }
  }

  public Vector readRecords(String storeName)
      throws RecordStoreException, UnsupportedEncodingException {
    RecordStore rs = null;
    RecordEnumeration re = null;
    try {
      rs = openRecordStore(storeName);
      int recordCount = rs.getNumRecords();
      Vector recordItems = new Vector(recordCount > 0 ? recordCount : 10);

      re = rs.enumerateRecords(null, null, false);
      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = getRecord(rs, recordId);
        recordItems.addElement(record);
      }

      return recordItems;

    } finally {
      if (re != null) {
        re.destroy();
      }
      closeRecordStore(rs);
    }
  }

  public String getRecord(String storeName, int recordId)
      throws RecordStoreNotOpenException,
          InvalidRecordIDException,
          RecordStoreException,
          UnsupportedEncodingException {
    RecordStore rs = null;
    try {
      rs = openRecordStore(storeName);
      return getRecord(rs, recordId);
    } finally {
      closeRecordStore(rs);
    }
  }

  private String getRecord(RecordStore rs, int recordId)
      throws RecordStoreNotOpenException,
          InvalidRecordIDException,
          RecordStoreException,
          UnsupportedEncodingException {
    if (rs == null) {
      throw new RecordStoreNotOpenException("Record store is not open");
    }
    byte[] recordData = rs.getRecord(recordId);
    return decodeRecordData(recordData);
  }

  public void setRecord(String storeName, int recordId, byte[] newData, int offset, int numBytes)
      throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
    RecordStore rs = null;
    try {
      rs = openRecordStore(storeName);
      rs.setRecord(recordId, newData, offset, numBytes);
    } finally {
      closeRecordStore(rs);
    }
  }

  public void deleteRecord(String storeName, int recordId)
      throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
    RecordStore rs = null;
    try {
      rs = openRecordStore(storeName);
      rs.deleteRecord(recordId);
    } finally {
      closeRecordStore(rs);
    }
  }

  public RecordEnumeration enumerateRecords(
      String storeName, RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
      throws RecordStoreNotOpenException, RecordStoreException {
    RecordStore rs = openRecordStore(storeName);
    return rs.enumerateRecords(filter, comparator, keepUpdated);
  }

  public void deleteRecordStore(String storeName) throws RecordStoreException {
    try {
      RecordStore.deleteRecordStore(storeName);
    } catch (Exception e) {
    }
  }

  public boolean recordStoreExists(String storeName) {
    try {
      String[] stores = RecordStore.listRecordStores();
      if (stores != null) {
        for (int i = 0; i < stores.length; i++) {
          if (storeName.equals(stores[i])) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private RecordStore openRecordStore(String storeName) throws RecordStoreException {
    return RecordStore.openRecordStore(storeName, true);
  }

  private void closeRecordStore(RecordStore rs) {
    if (rs != null) {
      try {
        rs.closeRecordStore();
      } catch (RecordStoreException e) {

      }
    }
  }

  private String decodeRecordData(byte[] recordData) throws UnsupportedEncodingException {
    if (recordData == null || recordData.length == 0) {
      return "";
    }

    try {
      return new String(recordData, cachedEncoding);
    } catch (UnsupportedEncodingException e) {

      return new String(recordData);
    }
  }

  public StoreStats getStoreStats(String storeName) {
    RecordStore rs = null;
    try {
      rs = openRecordStore(storeName);
      int numRecords = rs.getNumRecords();
      int size = rs.getSize();
      int sizeAvailable = rs.getSizeAvailable();
      return new StoreStats(numRecords, size, sizeAvailable);
    } catch (Exception e) {
      return new StoreStats(0, 0, 0);
    } finally {
      closeRecordStore(rs);
    }
  }

  public void cleanup() {

    openStores.clear();
  }

  public static class StoreStats {
    public final int numRecords;
    public final int size;
    public final int sizeAvailable;

    public StoreStats(int numRecords, int size, int sizeAvailable) {
      this.numRecords = numRecords;
      this.size = size;
      this.sizeAvailable = sizeAvailable;
    }

    public String toString() {
      return "StoreStats{records="
          + numRecords
          + ", size="
          + size
          + ", available="
          + sizeAvailable
          + "}";
    }
  }
}
