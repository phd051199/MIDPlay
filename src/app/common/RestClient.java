package app.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class RestClient {

  private static final int MAX_REDIRECT_TIMES = 5;
  private static String USER_AGENT;
  private static RestClient instance;

  public static RestClient getInstance() {
    if (instance == null) {
      instance = new RestClient();
    }
    return instance;
  }

  private static String getUserAgent() {
    if (USER_AGENT == null) {
      USER_AGENT = System.getProperty("microedition.platform");
      if (USER_AGENT == null) {
        USER_AGENT = "GenericJ2ME";
      }
      USER_AGENT += "/1.0 (MIDP-2.0; CLDC-1.1)";
    }
    return USER_AGENT;
  }

  private static byte[] readAllBytes(InputStream in, int contentLength) throws IOException {
    if (contentLength > 0) {
      byte[] data = new byte[contentLength];
      int offset = 0;
      while (offset < contentLength) {
        int read = in.read(data, offset, contentLength - offset);
        if (read == -1) {
          break;
        }
        offset += read;
      }
      if (offset == contentLength) {
        return data;
      }
      byte[] trimmed = new byte[offset];
      System.arraycopy(data, 0, trimmed, 0, offset);
      return trimmed;
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
      byte[] buffer = new byte[1024];
      int read;
      while ((read = in.read(buffer)) != -1) {
        baos.write(buffer, 0, read);
      }
      return baos.toByteArray();
    }
  }

  private RestClient() {}

  public HttpConnection getStreamConnection(String url) throws IOException {
    HttpConnection conn = null;
    int redirectCount = 0;

    while (redirectCount < MAX_REDIRECT_TIMES) {
      conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
      conn.setRequestMethod(HttpConnection.GET);
      conn.setRequestProperty("Accept", "*/*");
      conn.setRequestProperty("User-Agent", getUserAgent());

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
      return readAllBytes(inputStream, contentLength);

    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
      }
      try {
        if (hcon != null) {
          hcon.close();
        }
      } catch (IOException e) {
      }
    }
  }

  public String get(String url) throws IOException {
    byte[] data = getBytes(url);
    return new String(data, "UTF-8");
  }
}
