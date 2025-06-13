package app.common;

import java.util.Vector;
import javax.microedition.rms.RecordStore;

public class ReadWriteRecordStore {

  private RecordStore rs = null;

  public void openRecStore() {
    try {
      this.rs = RecordStore.openRecordStore("musicapp", true);
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
          if ("musicapp".equals(stores[i])) {
            RecordStore.deleteRecordStore("musicapp");
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void writeRecord(String str) {
    byte[] rec = str.getBytes();

    try {
      this.rs.addRecord(rec, 0, rec.length);
    } catch (Exception var4) {
      var4.printStackTrace();
    }
  }

  public Vector readRecords() {
    Vector recordItems = new Vector();

    try {
      if (rs == null) {
        return recordItems;
      }
      byte[] recData = new byte[5];
      for (int i = 1; i <= rs.getNumRecords(); i++) {
        if (rs.getRecordSize(i) > recData.length) {
          recData = new byte[rs.getRecordSize(i)];
        }

        int len = rs.getRecord(i, recData, 0);
        recordItems.addElement(new String(recData, 0, len));
      }

      return recordItems;
    } catch (Exception e) {
      e.printStackTrace();
      return recordItems;
    }
  }
}
