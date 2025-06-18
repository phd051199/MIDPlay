package app.common;

import java.util.Vector;
import javax.microedition.rms.RecordStore;

public class ReadWriteRecordStore {

  private RecordStore rs = null;
  private String name;

  public ReadWriteRecordStore(String name) {
    this.name = name;
  }

  public void openRecStore() {
    try {
      this.rs = RecordStore.openRecordStore(this.name, true);
    } catch (Exception var2) {
      var2.printStackTrace();
    }
  }

  public void closeRecStore() {
    try {
      this.rs.closeRecordStore();
    } catch (Exception var2) {
      var2.printStackTrace();
    }
  }

  public void deleteRecStore() {
    try {
      String[] stores = RecordStore.listRecordStores();
      if (stores != null) {
        for (int i = 0; i < stores.length; i++) {
          if (this.name.equals(stores[i])) {
            RecordStore.deleteRecordStore(this.name);
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void writeRecord(String str) {
    try {

      byte[] rec = str.getBytes("UTF-8");
      this.rs.addRecord(rec, 0, rec.length);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Vector readRecords() {
    Vector recordItems = new Vector();

    try {
      if (this.rs == null) {
        return recordItems;
      }

      for (int i = 1; i <= this.rs.getNumRecords(); i++) {
        byte[] recData = new byte[this.rs.getRecordSize(i)];
        int len = this.rs.getRecord(i, recData, 0);

        String record = new String(recData, 0, len, "UTF-8");
        recordItems.addElement(record);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return recordItems;
  }

  public boolean deleteRecord(int index) {
    try {
      if (this.rs != null && index >= 0 && index < this.rs.getNumRecords()) {

        int recordId = index + 1;
        this.rs.deleteRecord(recordId);
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
