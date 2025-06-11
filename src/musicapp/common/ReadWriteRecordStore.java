package musicapp.common;

import java.util.Vector;
import javax.microedition.rms.RecordStore;

public class ReadWriteRecordStore {

    private RecordStore rs = null;

    public void openRecStore() {
        try {
            this.rs = RecordStore.openRecordStore("nct", true);
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
        if (RecordStore.listRecordStores() != null) {
            try {
                RecordStore.deleteRecordStore("nct");
            } catch (Exception var2) {
                var2.printStackTrace();
            }
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
            byte[] recData = new byte[5];

            for (int i = 1; i <= this.rs.getNumRecords(); ++i) {
                if (this.rs.getRecordSize(i) > recData.length) {
                    recData = new byte[this.rs.getRecordSize(i)];
                }

                int len = this.rs.getRecord(i, recData, 0);
                recordItems.addElement(new String(recData, 0, len));
            }

            return recordItems;
        } catch (Exception var5) {
            var5.printStackTrace();
            return null;
        }
    }
}
