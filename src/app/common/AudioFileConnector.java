package app.common;

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public class AudioFileConnector {
  private static AudioFileConnector instance;

  public static synchronized AudioFileConnector getInstance() {
    if (instance == null) {
      instance = new AudioFileConnector();
    }
    return instance;
  }

  private FileConnection fileConn;
  private String filePath;
  private boolean initialized = false;

  private AudioFileConnector() {}

  public synchronized void initialize() throws IOException {
    if (initialized) {
      return;
    }
    String privateDir = System.getProperty("fileconn.dir.private");
    filePath = privateDir + "temp.mp3";
    fileConn = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);
    if (fileConn.exists()) {
      fileConn.delete();
    }
    fileConn.create();
    initialized = true;
  }

  public synchronized OutputStream openOutputStream() throws IOException {
    if (!initialized) {
      initialize();
    }
    return fileConn.openOutputStream();
  }

  public synchronized void clear() {
    if (fileConn != null && initialized) {
      try {
        if (fileConn.exists()) {
          fileConn.truncate(0);
        }
      } catch (IOException e) {
      }
    }
  }

  public synchronized void close() {
    if (fileConn != null) {
      try {
        fileConn.close();
      } catch (IOException e) {
      }
      fileConn = null;
      initialized = false;
    }
  }

  public synchronized String getFilePath() {
    return filePath;
  }
}
