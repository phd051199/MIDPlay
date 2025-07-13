package app.core.storage;

import java.io.UnsupportedEncodingException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class RecordStoreManager {
  private static final String ENCODING = "UTF-8";

  private final String recordStoreName;
  private RecordStore recordStore;

  public RecordStoreManager(String recordStoreName) {
    this.recordStoreName = recordStoreName;
  }

  public void openRecordStore() throws RecordStoreException {
    if (recordStore == null) {
      recordStore = RecordStore.openRecordStore(recordStoreName, true);
    }
  }

  public void closeRecordStore() {
    if (recordStore != null) {
      try {
        recordStore.closeRecordStore();
      } catch (RecordStoreException e) {

      } finally {
        recordStore = null;
      }
    }
  }

  public int addRecord(byte[] data) throws RecordStoreException {
    openRecordStore();
    return recordStore.addRecord(data, 0, data.length);
  }

  public int addRecord(String data) throws RecordStoreException {
    try {
      return addRecord(data.getBytes(ENCODING));
    } catch (UnsupportedEncodingException e) {
      return addRecord(data.getBytes());
    }
  }

  public byte[] getRecord(int recordId) throws RecordStoreException {
    openRecordStore();
    return recordStore.getRecord(recordId);
  }

  public String getRecordAsString(int recordId) throws RecordStoreException {
    byte[] data = getRecord(recordId);
    if (data != null) {
      try {
        return new String(data, ENCODING);
      } catch (UnsupportedEncodingException e) {
        return new String(data);
      }
    }
    return null;
  }

  public void setRecord(int recordId, byte[] data) throws RecordStoreException {
    openRecordStore();
    recordStore.setRecord(recordId, data, 0, data.length);
  }

  public void setRecord(int recordId, String data) throws RecordStoreException {
    try {
      setRecord(recordId, data.getBytes(ENCODING));
    } catch (UnsupportedEncodingException e) {
      setRecord(recordId, data.getBytes());
    }
  }

  public void deleteRecord(int recordId) throws RecordStoreException {
    openRecordStore();
    recordStore.deleteRecord(recordId);
  }

  public RecordEnumeration enumerateRecords() throws RecordStoreException {
    openRecordStore();
    return recordStore.enumerateRecords(null, null, false);
  }

  public int getNumRecords() throws RecordStoreException {
    openRecordStore();
    return recordStore.getNumRecords();
  }

  public void deleteRecordStore() {
    closeRecordStore();
    try {
      RecordStore.deleteRecordStore(recordStoreName);
    } catch (RecordStoreException e) {

    }
  }

  public boolean recordStoreExists() {
    String[] recordStores = RecordStore.listRecordStores();
    if (recordStores != null) {
      for (int i = 0; i < recordStores.length; i++) {
        if (recordStoreName.equals(recordStores[i])) {
          return true;
        }
      }
    }
    return false;
  }

  public String[] getAllRecordsAsStrings() throws RecordStoreException {
    openRecordStore();
    int numRecords = recordStore.getNumRecords();
    if (numRecords == 0) {
      return new String[0];
    }

    String[] records = new String[numRecords];
    RecordEnumeration enumeration = recordStore.enumerateRecords(null, null, false);
    int index = 0;

    try {
      while (enumeration.hasNextElement()) {
        int recordId = enumeration.nextRecordId();
        byte[] data = recordStore.getRecord(recordId);
        try {
          records[index++] = new String(data, ENCODING);
        } catch (UnsupportedEncodingException e) {
          records[index++] = new String(data);
        }
      }
    } finally {
      enumeration.destroy();
    }

    return records;
  }

  public void clearAllRecords() throws RecordStoreException {
    openRecordStore();
    RecordEnumeration enumeration = recordStore.enumerateRecords(null, null, false);

    try {
      while (enumeration.hasNextElement()) {
        int recordId = enumeration.nextRecordId();
        recordStore.deleteRecord(recordId);
      }
    } finally {
      enumeration.destroy();
    }
  }
}
