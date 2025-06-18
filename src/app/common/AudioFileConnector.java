package app.common;

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public class AudioFileConnector {
  private static AudioFileConnector instance;
  private FileConnection fileConn;
  private String filePath;
  private boolean initialized = false;

  private AudioFileConnector() {}

  public static synchronized AudioFileConnector getInstance() {
    if (instance == null) {
      instance = new AudioFileConnector();
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
}
