package app.common;

import app.interfaces.RestCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class RestClient {

  private static final int MAX_REDIRECT_TIMES = 5;
  private static final int DEFAULT_TIMEOUT = 30000;
  private static final int BUFFER_SIZE = 2048;
  private static final String CHARSET = "UTF-8";

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
    if (in == null) {
      return new byte[0];
    }

    ByteArrayOutputStream baos;
    byte[] buffer;

    if (contentLength > 0) {

      buffer = new byte[Math.min(contentLength, BUFFER_SIZE)];
      baos = new ByteArrayOutputStream(contentLength);
    } else {

      buffer = new byte[BUFFER_SIZE];
      baos = new ByteArrayOutputStream(BUFFER_SIZE);
    }

    int bytesRead;
    try {
      while ((bytesRead = in.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    } finally {
      baos.close();
    }
  }

  private RestClient() {}

  public HttpConnection createConnection(String url, String method) throws IOException {
    HttpConnection conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
    conn.setRequestMethod(method);
    conn.setRequestProperty("User-Agent", getUserAgent());
    return conn;
  }

  public HttpConnection getStreamConnection(String url) throws IOException {
    HttpConnection conn = null;
    int redirectCount = 0;
    String currentUrl = url;

    while (redirectCount < MAX_REDIRECT_TIMES) {
      try {
        conn = createConnection(currentUrl, HttpConnection.GET);
        int status = conn.getResponseCode();

        if (status == HttpConnection.HTTP_OK) {
          return conn;
        } else if (status == HttpConnection.HTTP_MOVED_PERM
            || status == HttpConnection.HTTP_MOVED_TEMP) {

          String location = conn.getHeaderField("Location");
          conn.close();
          conn = null;

          if (location == null) {
            throw new IOException("Redirect location is null");
          }
          currentUrl = location;
          redirectCount++;
        } else {
          throw new IOException("Server response code: " + status);
        }
      } catch (IOException e) {
        if (conn != null) {
          try {
            conn.close();
          } catch (IOException ignore) {
          }
        }
        throw e;
      }
    }
    throw new IOException("Too many redirects");
  }

  public byte[] getBytes(String url) throws IOException {
    HttpConnection conn = null;
    InputStream inputStream = null;

    try {
      conn = getStreamConnection(url);
      int responseCode = conn.getResponseCode();

      if (responseCode >= 400) {
        throw new IOException("HTTP error: " + responseCode + " " + conn.getResponseMessage());
      }

      inputStream = conn.openInputStream();
      int contentLength = (int) conn.getLength();
      return readAllBytes(inputStream, contentLength);
    } finally {
      closeQuietly(inputStream);
      closeQuietly(conn);
    }
  }

  public String get(String url) throws IOException {
    byte[] data = getBytes(url);
    return new String(data, CHARSET);
  }

  public void getAsync(final String url, final RestCallback callback) {
    new Thread(
            new Runnable() {
              public void run() {
                try {
                  final String response = get(url);
                  callback.success(response);
                } catch (final IOException e) {
                  callback.error(e);
                }
              }
            })
        .start();
  }

  private void closeQuietly(Object closeable) {
    if (closeable != null) {
      try {
        if (closeable instanceof InputStream) {
          ((InputStream) closeable).close();
        } else if (closeable instanceof OutputStream) {
          ((OutputStream) closeable).close();
        } else if (closeable instanceof HttpConnection) {
          ((HttpConnection) closeable).close();
        }
      } catch (IOException ignore) {

      }
    }
  }
}
