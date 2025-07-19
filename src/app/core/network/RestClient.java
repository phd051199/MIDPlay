package app.core.network;

import app.core.platform.DeviceInfo;
import app.core.threading.ThreadManagerIntegration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class RestClient {

  private static final int MAX_REDIRECT_TIMES = 5;
  private static final int BUFFER_SIZE = 8192;
  private static final String CHARSET = "UTF-8";

  private static volatile String userAgent;
  private static volatile RestClient instance;
  private static final Object instanceLock = new Object();
  private static final Object userAgentLock = new Object();

  public static RestClient getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new RestClient();
        }
      }
    }
    return instance;
  }

  private static String getUserAgent() {
    if (userAgent == null) {
      synchronized (userAgentLock) {
        if (userAgent == null) {
          userAgent = System.getProperty("microedition.platform");
          if (userAgent == null) {
            userAgent = "GenericJ2ME";
          }
          userAgent += "/1.0 (MIDP-2.0; CLDC-1.1)";
        }
      }
    }

    return userAgent;
  }

  private static byte[] readAllBytes(InputStream in, int contentLength) throws IOException {
    if (in == null) {
      return new byte[0];
    }

    final int MAX_CONTENT_SIZE = 300 * 1024;

    if (contentLength > MAX_CONTENT_SIZE) {
      throw new IOException(
          "Content too large: " + contentLength + " bytes (max: " + MAX_CONTENT_SIZE + ")");
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
    int totalBytesRead = 0;

    try {
      while ((bytesRead = in.read(buffer)) != -1) {
        totalBytesRead += bytesRead;

        if (totalBytesRead > MAX_CONTENT_SIZE) {
          throw new IOException(
              "Content too large: " + totalBytesRead + " bytes (max: " + MAX_CONTENT_SIZE + ")");
        }

        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    } catch (OutOfMemoryError e) {
      throw new IOException("Out of memory reading response");
    } finally {
      try {
        baos.close();
      } catch (Exception e) {
      }
    }
  }

  private RestClient() {}

  public HttpConnection createConnection(String url, String method) throws IOException {
    if (url == null || url.trim().length() == 0) {
      throw new IllegalArgumentException("URL cannot be null or empty");
    }

    if (method == null || method.trim().length() == 0) {
      throw new IllegalArgumentException("HTTP method cannot be null or empty");
    }

    HttpConnection conn = null;
    try {
      conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
      conn.setRequestMethod(method);
      conn.setRequestProperty("user-agent", getUserAgent());
      conn.setRequestProperty("x-device-id", DeviceInfo.getDeviceId());
      return conn;
    } catch (Exception e) {
      if (conn != null) {
        try {
          conn.close();
        } catch (Exception closeEx) {
        }
      }
      throw new IOException("Failed to create connection: " + e.getMessage());
    }
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
    ThreadManagerIntegration.executeNetworkRequest(
        url,
        new RestCallback() {
          public void success(String response) {
            callback.success(response);
          }

          public void error(Exception e) {
            callback.error(e);
          }
        });
  }

  private void closeQuietly(Object closeable) {
    if (closeable != null) {
      try {
        if (closeable instanceof InputStream) {
          ((InputStream) closeable).close();
        } else if (closeable instanceof OutputStream) {
          ((OutputStream) closeable).close();
        } else if (closeable instanceof Connection) {
          ((Connection) closeable).close();
        }
      } catch (IOException ignore) {
      }
    }
  }
}
