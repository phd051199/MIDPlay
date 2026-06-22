package midplay.store;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStoreException;

public class JsonRecordStore {
  private final RecordStoreManager storage;
  private int recordId;
  private final String defaultValue;

  public JsonRecordStore(String storeName, int recordId, String defaultValue) {
    this.storage = new RecordStoreManager(storeName);
    this.recordId = recordId;
    this.defaultValue = defaultValue;
  }

  public String load() {
    try {
      return storage.getRecordAsString(recordId);
    } catch (RecordStoreException e) {
    }
    return defaultValue;
  }

  public void save(String json) throws RecordStoreException {
    try {
      storage.setRecord(recordId, json);
    } catch (InvalidRecordIDException e) {
      recordId = storage.addRecord(json);
    }
  }

  public void close() {
    storage.closeRecordStore();
  }
}
