package midplay.store;

import java.io.UnsupportedEncodingException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class RecordStoreManager {
  private static final String ENCODING = "UTF-8";

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

  public synchronized int addRecord(String data) throws RecordStoreException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    byte[] bytes = encodeString(data);
    openRecordStore();
    return recordStore.addRecord(bytes, 0, bytes.length);
  }

  public synchronized String getRecordAsString(int recordId) throws RecordStoreException {
    if (recordId <= 0) {
      throw new IllegalArgumentException("Record ID must be positive");
    }
    openRecordStore();
    return decodeBytes(recordStore.getRecord(recordId));
  }

  public synchronized void setRecord(int recordId, String data) throws RecordStoreException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    if (recordId <= 0) {
      throw new IllegalArgumentException("Record ID must be positive");
    }
    byte[] bytes = encodeString(data);
    openRecordStore();
    recordStore.setRecord(recordId, bytes, 0, bytes.length);
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

  // Iterates every record, invoking the handler for each. The handler returns
  // true to continue or false to stop. Records that fail to parse inside the
  // handler are skipped (logged) rather than blanking the whole scan, and the
  // enumeration is always destroyed. RMS-level errors stop iteration.
  // Synchronized so the enumerate-then-iterate sequence is atomic w.r.t. other
  // ops on this manager (add/delete from another thread). Handlers re-enter the
  // monitor (reentrant) via the synchronized getRecordAsString call per record.
  public synchronized void forEachRecord(RecordHandler handler) {
    try {
      RecordEnumeration enumeration = enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = getRecordAsString(recordId);
          try {
            if (!handler.handle(recordId, data)) {
              break;
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  // Callback for visiting records stored under this manager.
  public interface RecordHandler {
    boolean handle(int recordId, String data);
  }
}
