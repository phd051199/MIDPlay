import java.io.UnsupportedEncodingException;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class RecordStoreManager {
  private static final String ENCODING = "UTF-8";
  private static final int INITIAL_VECTOR_SIZE = 5;

  private static byte[] encodeString(String str) {
    if (str == null) {
      return new byte[0];
    }
    try {
      return str.getBytes(ENCODING);
    } catch (UnsupportedEncodingException e) {
      return str.getBytes();
    }
  }

  private static String decodeBytes(byte[] data) {
    if (data == null || data.length == 0) {
      return "";
    }
    try {
      return new String(data, ENCODING);
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  private final String recordStoreName;
  private RecordStore recordStore;

  public RecordStoreManager(String recordStoreName) {
    if (recordStoreName == null || recordStoreName.length() == 0) {
      throw new IllegalArgumentException("Record store name cannot be null or empty");
    }
    this.recordStoreName = recordStoreName;
  }

  private synchronized void openRecordStore() throws RecordStoreException {
    if (recordStore == null) {
      recordStore = RecordStore.openRecordStore(recordStoreName, true);
    }
  }

  public synchronized void closeRecordStore() {
    if (recordStore != null) {
      try {
        recordStore.closeRecordStore();
      } catch (RecordStoreException e) {
        e.printStackTrace();
      } finally {
        recordStore = null;
      }
    }
  }

  public synchronized int addRecord(byte[] data) throws RecordStoreException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    openRecordStore();
    return recordStore.addRecord(data, 0, data.length);
  }

  public synchronized int addRecord(String data) throws RecordStoreException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    return addRecord(encodeString(data));
  }

  public synchronized byte[] getRecord(int recordId) throws RecordStoreException {
    if (recordId <= 0) {
      throw new IllegalArgumentException("Record ID must be positive");
    }
    openRecordStore();
    return recordStore.getRecord(recordId);
  }

  public synchronized String getRecordAsString(int recordId) throws RecordStoreException {
    byte[] data = getRecord(recordId);
    return decodeBytes(data);
  }

  public synchronized boolean recordExists(int recordId) {
    try {
      openRecordStore();
      recordStore.getRecord(recordId);
      return true;
    } catch (RecordStoreException e) {
      return false;
    }
  }

  public synchronized void setRecord(int recordId, byte[] data) throws RecordStoreException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    if (recordId <= 0) {
      throw new IllegalArgumentException("Record ID must be positive");
    }
    openRecordStore();
    recordStore.setRecord(recordId, data, 0, data.length);
  }

  public synchronized void setRecord(int recordId, String data) throws RecordStoreException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    setRecord(recordId, encodeString(data));
  }

  public synchronized void deleteRecord(int recordId) throws RecordStoreException {
    if (recordId <= 0) {
      throw new IllegalArgumentException("Record ID must be positive");
    }
    openRecordStore();
    recordStore.deleteRecord(recordId);
  }

  public synchronized RecordEnumeration enumerateRecords() throws RecordStoreException {
    openRecordStore();
    return recordStore.enumerateRecords(null, null, false);
  }

  public synchronized void safeEnumerateRecords(RecordEnumerationCallback callback)
      throws RecordStoreException {
    if (callback == null) {
      throw new IllegalArgumentException("Callback cannot be null");
    }
    openRecordStore();
    RecordEnumeration enumeration = null;
    try {
      enumeration = recordStore.enumerateRecords(null, null, false);
      callback.processEnumeration(enumeration);
    } finally {
      if (enumeration != null) {
        enumeration.destroy();
      }
    }
  }

  public synchronized int getNumRecords() throws RecordStoreException {
    openRecordStore();
    return recordStore.getNumRecords();
  }

  public synchronized void deleteRecordStore() {
    closeRecordStore();
    try {
      RecordStore.deleteRecordStore(recordStoreName);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public synchronized boolean recordStoreExists() {
    RecordStore rs = null;
    try {
      rs = RecordStore.openRecordStore(recordStoreName, false);
      return true;
    } catch (RecordStoreException e) {
      return false;
    } finally {
      if (rs != null) {
        try {
          rs.closeRecordStore();
        } catch (RecordStoreException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public synchronized String[] getAllRecordsAsStrings() throws RecordStoreException {
    openRecordStore();
    int numRecords = recordStore.getNumRecords();
    if (numRecords == 0) {
      return new String[0];
    }
    Vector recordsVector = new Vector(Math.min(numRecords, INITIAL_VECTOR_SIZE));
    RecordEnumeration enumeration = null;
    try {
      enumeration = recordStore.enumerateRecords(null, null, false);
      while (enumeration.hasNextElement()) {
        int recordId = enumeration.nextRecordId();
        try {
          byte[] data = recordStore.getRecord(recordId);
          recordsVector.addElement(decodeBytes(data));
        } catch (RecordStoreException e) {
          e.printStackTrace();
        }
      }
    } finally {
      if (enumeration != null) {
        enumeration.destroy();
      }
    }
    String[] records = new String[recordsVector.size()];
    recordsVector.copyInto(records);
    return records;
  }

  public synchronized void processAllRecords(RecordProcessor processor)
      throws RecordStoreException {
    if (processor == null) {
      throw new IllegalArgumentException("Processor cannot be null");
    }
    openRecordStore();
    RecordEnumeration enumeration = null;
    try {
      enumeration = recordStore.enumerateRecords(null, null, false);
      while (enumeration.hasNextElement()) {
        int recordId = enumeration.nextRecordId();
        try {
          byte[] data = recordStore.getRecord(recordId);
          String stringData = decodeBytes(data);
          if (!processor.processRecord(recordId, stringData)) {
            break;
          }
        } catch (RecordStoreException e) {
          if (!processor.handleError(recordId, e)) {
            throw e;
          }
        }
      }
    } finally {
      if (enumeration != null) {
        enumeration.destroy();
      }
    }
  }

  public synchronized void clearAllRecords() throws RecordStoreException {
    openRecordStore();
    RecordEnumeration enumeration = null;
    try {
      enumeration = recordStore.enumerateRecords(null, null, false);
      Vector recordIds = new Vector();
      while (enumeration.hasNextElement()) {
        recordIds.addElement(new Integer(enumeration.nextRecordId()));
      }
      for (int i = 0; i < recordIds.size(); i++) {
        int recordId = ((Integer) recordIds.elementAt(i)).intValue();
        try {
          recordStore.deleteRecord(recordId);
        } catch (RecordStoreException e) {
          e.printStackTrace();
        }
      }
    } finally {
      if (enumeration != null) {
        enumeration.destroy();
      }
    }
  }

  public synchronized int[] addRecords(String[] dataArray) throws RecordStoreException {
    if (dataArray == null) {
      throw new IllegalArgumentException("Data array cannot be null");
    }
    openRecordStore();
    int[] recordIds = new int[dataArray.length];
    for (int i = 0; i < dataArray.length; i++) {
      if (dataArray[i] != null) {
        byte[] encodedData = encodeString(dataArray[i]);
        recordIds[i] = recordStore.addRecord(encodedData, 0, encodedData.length);
      }
    }
    return recordIds;
  }

  public synchronized int getRecordSize(int recordId) throws RecordStoreException {
    if (recordId <= 0) {
      throw new IllegalArgumentException("Record ID must be positive");
    }
    openRecordStore();
    return recordStore.getRecordSize(recordId);
  }

  public synchronized int getTotalSize() throws RecordStoreException {
    openRecordStore();
    return recordStore.getSize();
  }

  public synchronized int getSizeAvailable() throws RecordStoreException {
    openRecordStore();
    return recordStore.getSizeAvailable();
  }

  public static interface RecordEnumerationCallback {
    void processEnumeration(RecordEnumeration enumeration) throws RecordStoreException;
  }

  public static interface RecordProcessor {
    boolean processRecord(int recordId, String data);

    boolean handleError(int recordId, RecordStoreException error);
  }
}
