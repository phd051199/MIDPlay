package app.common;

import java.io.UnsupportedEncodingException;
import java.util.Vector;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class ReadWriteRecordStore {

  private RecordStore rs = null;
  private final String name;
  private String cachedEncoding = null;

  public ReadWriteRecordStore(String name) {
    this.name = name;
  }

  public void openRecStore() throws RecordStoreException {
    this.rs = RecordStore.openRecordStore(this.name, true);
  }

  public void closeRecStore() throws RecordStoreException {
    if (rs != null) {
      rs.closeRecordStore();
    }
  }

  public void writeRecord(String str) throws RecordStoreException, UnsupportedEncodingException {
    if (str == null) {
      throw new IllegalArgumentException("Input string cannot be null");
    }

    byte[] rec = str.getBytes("UTF-8");
    rs.addRecord(rec, 0, rec.length);
  }

  public Vector readRecords() throws RecordStoreException, UnsupportedEncodingException {
    int recordCount = rs != null ? rs.getNumRecords() : 10;
    Vector recordItems = new Vector(recordCount);
    RecordEnumeration re = null;
    try {
      re = this.rs.enumerateRecords(null, null, false);
      while (re.hasNextElement()) {
        int recordId = re.nextRecordId();
        String record = getRecord(recordId);
        recordItems.addElement(record);
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
    }
    return recordItems;
  }

  public Vector readRecordsBatch() throws RecordStoreException, UnsupportedEncodingException {
    if (rs == null) {
      throw new RecordStoreNotOpenException("Record store is not open");
    }

    int recordCount = rs.getNumRecords();
    Vector recordItems = new Vector(recordCount);
    RecordEnumeration re = null;

    try {
      re = rs.enumerateRecords(null, null, false);

      while (re.hasNextElement()) {
        byte[] recordData = re.nextRecord();
        String record = decodeRecordData(recordData);
        recordItems.addElement(record);
      }
    } finally {
      if (re != null) {
        re.destroy();
      }
    }

    return recordItems;
  }

  public void writeRecordsBatch(String[] records)
      throws RecordStoreException, UnsupportedEncodingException {
    if (records == null || records.length == 0) {
      return;
    }

    for (int i = 0; i < records.length; i++) {
      if (records[i] != null) {
        writeRecord(records[i]);
      }
    }
  }

  public void deleteRecord(int recordId) throws RecordStoreException {
    rs.deleteRecord(recordId);
  }

  public void setRecord(int recordId, byte[] data, int offset, int length)
      throws RecordStoreException {
    rs.setRecord(recordId, data, offset, length);
  }

  public RecordEnumeration enumerateRecords(
      RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
      throws RecordStoreNotOpenException, RecordStoreException {
    if (rs == null) {
      throw new RecordStoreNotOpenException("Record store is not open");
    }
    return rs.enumerateRecords(filter, comparator, keepUpdated);
  }

  public String getRecord(int recordId)
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

  private String decodeRecordData(byte[] recordData) throws UnsupportedEncodingException {
    if (recordData == null || recordData.length == 0) {
      return "";
    }

    if (cachedEncoding != null) {
      return new String(recordData, cachedEncoding);
    }

    String encoding = detectEncoding(recordData);
    cachedEncoding = encoding;

    if ("UTF-8".equals(encoding)
        && recordData.length >= 3
        && (recordData[0] & 0xFF) == 0xEF
        && (recordData[1] & 0xFF) == 0xBB
        && (recordData[2] & 0xFF) == 0xBF) {
      return new String(recordData, 3, recordData.length - 3, encoding);
    }

    return new String(recordData, encoding);
  }

  private String detectEncoding(byte[] recordData) {
    if (recordData.length >= 3
        && (recordData[0] & 0xFF) == 0xEF
        && (recordData[1] & 0xFF) == 0xBB
        && (recordData[2] & 0xFF) == 0xBF) {
      return "UTF-8";
    }

    if (recordData.length > 0 && (recordData[0] == 0xFF || recordData[0] == 0xFE)) {
      return "UTF-16";
    }

    return "UTF-8";
  }

  public void clearEncodingCache() {
    cachedEncoding = null;
  }
}
