package app.common;

import java.io.ByteArrayOutputStream;
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

  private static void configureConncetion(HttpConnection conn) throws IOException {
    String locale = System.getProperty("microedition.locale");
    if (locale == null) {
      locale = "en-US";
    }

    String platform = System.getProperty("microedition.platform");
    if (platform == null) {
      platform = "GenericJ2ME";
    }
    String userAgent = platform + "/1.0 (MIDP-2.1; CLDC-1.1)";
    conn.setRequestProperty("User-Agent", userAgent);
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("Accept-Language", locale);
  }

  public HttpConnection getConnection(String url) throws IOException {
    HttpConnection conn = (HttpConnection) Connector.open(url);
    configureConncetion(conn);
    return conn;
  }

  public HttpConnection getConnection(String url, int access) throws IOException {
    HttpConnection conn = (HttpConnection) Connector.open(url, access);
    configureConncetion(conn);
    return conn;
  }

  public String get(String url) throws IOException {
    HttpConnection hcon = null;
    InputStream inputStream = null;
    ByteArrayOutputStream baos = null;
    String data = null;

    try {
      int redirectTimes = 0;
      boolean redirect;

      do {
        redirect = false;
        hcon = getConnection(url);
        inputStream = hcon.openInputStream();
        baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[256];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
          baos.write(buffer, 0, len);
        }

        byte[] responseData = baos.toByteArray();
        data = new String(responseData, "UTF-8");

        int status = hcon.getResponseCode();
        switch (status) {
          case HttpConnection.HTTP_OK:
            break;
          case HttpConnection.HTTP_MOVED_PERM:
          case HttpConnection.HTTP_MOVED_TEMP:
          case HttpConnection.HTTP_SEE_OTHER:
            url = hcon.getHeaderField("location");
            inputStream.close();
            hcon.close();
            hcon = null;
            inputStream = null;
            ++redirectTimes;
            redirect = true;
            break;
          default:
            throw new IOException("Response status not OK: " + status);
        }
      } while (redirect && redirectTimes < MAX_REDIRECT_TIMES);

      if (redirectTimes == MAX_REDIRECT_TIMES) {
        throw new IOException("Too many redirects");
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (inputStream != null) inputStream.close();
        if (hcon != null) hcon.close();
      } catch (IOException e) {
      }
    }

    return data;
  }
}
