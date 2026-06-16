package midplay.store;

import cc.nnproject.json.JSON;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStoreException;

// Single-record JSON document store: one JSON blob (object or array) persisted
// at a fixed record id, with a default value used when the record is missing.
// Backs the one-document-per-store managers (settings, menu). Favorites use a
// multi-record store directly (see RecordStoreManager.forEachRecord).
public class JsonRecordStore {
  private final RecordStoreManager storage;
  private final int recordId;
  private final String defaultValue;

  public JsonRecordStore(String storeName, int recordId, String defaultValue) {
    this.storage = new RecordStoreManager(storeName);
    this.recordId = recordId;
    this.defaultValue = defaultValue;
  }

  // Returns the stored JSON text, or the default value when the record is
  // missing or cannot be read. Parsing is left to the caller so the same store
  // can hold either a JSON object or a JSON array.
  public String load() {
    try {
      return storage.getRecordAsString(recordId);
    } catch (RecordStoreException e) {
      // Record missing (fresh install) or unreadable — fall back to the default.
    }
    return defaultValue;
  }

  // Write through: update the record in place, and create it on first write.
  // Avoids the previous recordExists() probe (a separate RMS round-trip that
  // used exception-driven control flow on the cold install path). Only an
  // invalid/missing record id triggers the create fallback; other failures
  // (e.g. store full) propagate so callers don't silently lose the write to a
  // duplicate record at a different id.
  public void save(String json) throws RecordStoreException {
    try {
      storage.setRecord(recordId, json);
    } catch (InvalidRecordIDException e) {
      // Record doesn't exist yet (fresh install) — create it. RMS assigns id 1
      // to the first record in a new store, matching the fixed recordId.
      storage.addRecord(json);
    }
  }

  public void close() {
    storage.closeRecordStore();
  }
}
