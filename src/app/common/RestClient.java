package app.common;

import app.utils.Utils;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class RestClient {

  private static final int MAX_REDIRECT_TIMES = 5;
  private static RestClient instance;

  private RestClient() {}

  public static RestClient getInstance() {
    if (instance == null) {
      instance = new RestClient();
    }
    return instance;
  }

  private static void configureConnection(HttpConnection conn) throws IOException {
    String platform = System.getProperty("microedition.platform");
    if (platform == null) {
      platform = "GenericJ2ME";
    }
    String userAgent = platform + "/1.0 (MIDP-2.0; CLDC-1.1)";
    conn.setRequestProperty("User-Agent", userAgent);
  }

  public HttpConnection getStreamConnection(String url) throws IOException {
    HttpConnection conn = null;
    int redirectCount = 0;

    while (redirectCount < MAX_REDIRECT_TIMES) {
      conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
      configureConnection(conn);
      conn.setRequestMethod(HttpConnection.GET);
      conn.setRequestProperty("Accept", "*/*");

      int status = conn.getResponseCode();

      if (status == HttpConnection.HTTP_OK) {
        return conn;
      } else if (status == HttpConnection.HTTP_MOVED_PERM
          || status == HttpConnection.HTTP_MOVED_TEMP
          || status == 307) {
        url = conn.getHeaderField("Location");
        conn.close();

        if (url == null) {
          throw new IOException("Redirect location is null");
        }
        redirectCount++;
      } else {
        throw new IOException("Server response code: " + status);
      }
    }
    throw new IOException("Too many redirects");
  }

  public byte[] getBytes(String url) throws IOException {
    HttpConnection hcon = null;
    InputStream inputStream = null;

    try {
      hcon = getStreamConnection(url);
      int responseCode = hcon.getResponseCode();
      if (responseCode >= 400) {
        throw new IOException("HTTP " + responseCode);
      }

      inputStream = hcon.openInputStream();
      int contentLength = (int) hcon.getLength();
      return Utils.readBytes(inputStream, contentLength, 1024, 2048);

    } finally {
      try {
        if (inputStream != null) inputStream.close();
      } catch (IOException e) {
      }
      try {
        if (hcon != null) hcon.close();
      } catch (IOException e) {
      }
    }
  }

  public String get(String url) throws IOException {
    byte[] data = getBytes(url);
    return new String(data, "UTF-8");
  }
}
