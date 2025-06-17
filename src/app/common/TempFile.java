package app.common;

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public class TempFile {
  private static TempFile instance;
  private FileConnection fileConn;
  private String filePath;
  private boolean initialized = false;

  private TempFile() {}

  public static synchronized TempFile getInstance() {
    if (instance == null) {
      instance = new TempFile();
    }
    return instance;
  }

  public synchronized void initialize() throws IOException {
    if (!initialized) {
      String privateDir = System.getProperty("fileconn.dir.private");
      this.filePath = privateDir + "temp_audio.mp3";
      this.fileConn = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);

      if (fileConn.exists()) {
        fileConn.delete();
      }
      fileConn.create();

      initialized = true;
    }
  }

  public synchronized OutputStream openOutputStream() throws IOException {
    if (!initialized) {
      initialize();
    }
    return fileConn.openOutputStream();
  }

  public synchronized void clear() {
    if (fileConn != null) {
      try {
        if (fileConn.exists()) {
          fileConn.truncate(0);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public synchronized String getFilePath() {
    return filePath;
  }

  public synchronized void close() {
    if (fileConn != null) {
      try {
        fileConn.close();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        fileConn = null;
        initialized = false;
      }
    }
  }

  public synchronized void cleanup() {
    if (fileConn != null) {
      try {
        if (fileConn.exists()) {
          fileConn.delete();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        close();
      }
    }
  }
}
