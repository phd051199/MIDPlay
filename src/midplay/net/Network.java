package midplay.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.util.Utils;

public class Network {
  private static final int BUFFER_SIZE = 4096;
  private static final long SOCKET_TIMEOUT_MS = 90000L;
  private static final Timer watchdogTimer = new Timer();

  private String userAgent;
  private volatile boolean cancelled = false;

  public static final long PLAYBACK_SETUP_TIMEOUT_MS = 60000L;

  public Network() {}

  public static HttpConnection openConnection(String url) throws IOException {
    return openConnection(url, Connector.READ);
  }

  public static HttpConnection openConnection(String url, int mode) throws IOException {
    if (Utils.isBlackberry) {
      int blackberryWifi = SettingsManager.getInstance().getCurrentBlackberryWifi();

      if (blackberryWifi == Configuration.BLACKBERRY_WIFI_ON) {
        url += ";deviceside=true;interface=wifi";
      }
    }
    return (HttpConnection) Connector.open(url, mode, true);
  }

  private String getUserAgent() {
    if (userAgent == null) {
      userAgent = System.getProperty("microedition.platform");
      if (userAgent == null) {
        userAgent = "GenericJ2ME";
      }
      userAgent += "/1.0 (MIDP-2.0; CLDC-1.1)";
    }
    return userAgent;
  }

  byte[] sendHttpGetBytes(String url) throws NetworkError {
    try {
      return sendHttpGetBytesOnce(url);
    } catch (NetworkError e) {
      if (cancelled) {
        throw e;
      }
      try {
        Thread.sleep(300);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw e;
      }
      if (cancelled) {
        throw e;
      }
      return sendHttpGetBytesOnce(url);
    }
  }

  byte[] sendHttpPostBytes(String url, String body) throws NetworkError {
    try {
      return sendHttpPostBytesOnce(url, body);
    } catch (NetworkError e) {
      if (cancelled) {
        throw e;
      }
      try {
        Thread.sleep(300);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw e;
      }
      if (cancelled) {
        throw e;
      }
      return sendHttpPostBytesOnce(url, body);
    }
  }

  private byte[] sendHttpPostBytesOnce(String url, String body) throws NetworkError {
    HttpConnection hcon = null;
    OutputStream os = null;
    DataInputStream dis = null;
    ByteArrayOutputStream response = null;
    TimerTask watchdog = null;
    try {
      if (cancelled) {
        throw new NetworkError("Operation cancelled");
      }
      hcon = openConnection(url, Connector.READ_WRITE);
      if (hcon == null) {
        throw new NetworkError("Failed to open connection");
      }
      hcon.setRequestProperty("User-Agent", getUserAgent());
      hcon.setRequestMethod(HttpConnection.POST);
      hcon.setRequestProperty("Content-Type", "application/json");
      byte[] bodyBytes = body.getBytes("UTF-8");
      hcon.setRequestProperty("Content-Length", Integer.toString(bodyBytes.length));
      os = hcon.openOutputStream();
      os.write(bodyBytes);
      os.flush();
      os.close();
      os = null;
      watchdog = armWatchdog(hcon);
      int responseCode = hcon.getResponseCode();
      if (responseCode != HttpConnection.HTTP_OK) {
        throw new NetworkError("HTTP Error: " + responseCode);
      }
      long declaredLen = -1;
      try {
        declaredLen = hcon.getLength();
      } catch (Exception e) {
      }
      if (declaredLen > 0 && declaredLen < 524288) {
        response = new ByteArrayOutputStream((int) declaredLen);
      } else {
        response = new ByteArrayOutputStream();
      }
      dis = new DataInputStream(hcon.openInputStream());
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = dis.read(buffer)) != -1) {
        if (cancelled) {
          throw new NetworkError("Operation cancelled");
        }
        response.write(buffer, 0, bytesRead);
      }
      return response.toByteArray();
    } catch (IOException e) {
      if (cancelled) {
        throw new NetworkError("Operation cancelled");
      }
      throw new NetworkError("Network error: " + e.getMessage());
    } finally {
      if (watchdog != null) {
        watchdog.cancel();
      }
      Utils.closeQuietly(os);
      closeResources(dis, hcon);
    }
  }

  private byte[] sendHttpGetBytesOnce(String url) throws NetworkError {
    HttpConnection hcon = null;
    DataInputStream dis = null;
    ByteArrayOutputStream response = null;
    TimerTask watchdog = null;
    try {
      if (cancelled) {
        throw new NetworkError("Operation cancelled");
      }
      hcon = openConnection(url);
      if (hcon == null) {
        throw new NetworkError("Failed to open connection");
      }
      hcon.setRequestProperty("User-Agent", getUserAgent());
      hcon.setRequestMethod(HttpConnection.GET);
      watchdog = armWatchdog(hcon);
      int responseCode = hcon.getResponseCode();
      if (responseCode != HttpConnection.HTTP_OK && responseCode != HttpConnection.HTTP_PARTIAL) {
        throw new NetworkError("HTTP Error: " + responseCode);
      }
      long declaredLen = -1;
      try {
        declaredLen = hcon.getLength();
      } catch (Exception e) {
      }
      if (declaredLen > 0 && declaredLen < 524288) {
        response = new ByteArrayOutputStream((int) declaredLen);
      } else {
        response = new ByteArrayOutputStream();
      }
      dis = new DataInputStream(hcon.openInputStream());
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = dis.read(buffer)) != -1) {
        if (cancelled) {
          throw new NetworkError("Operation cancelled");
        }
        response.write(buffer, 0, bytesRead);
      }
      return response.toByteArray();
    } catch (IOException e) {
      if (cancelled) {
        throw new NetworkError("Operation cancelled");
      }
      throw new NetworkError("Network error: " + e.getMessage());
    } finally {
      if (watchdog != null) {
        watchdog.cancel();
      }
      closeResources(dis, hcon);
    }
  }

  private TimerTask armWatchdog(final HttpConnection conn) {
    TimerTask task =
        new TimerTask() {
          public void run() {
            Utils.closeQuietly(conn);
          }
        };
    watchdogTimer.schedule(task, SOCKET_TIMEOUT_MS);
    return task;
  }

  public static TimerTask armPlaybackWatchdog(final HttpConnection conn, long delayMs) {
    TimerTask task =
        new TimerTask() {
          public void run() {
            Utils.closeQuietly(conn);
          }
        };
    watchdogTimer.schedule(task, delayMs);
    return task;
  }

  private void closeResources(DataInputStream dis, HttpConnection hcon) {
    Utils.closeQuietly(dis);
    Utils.closeQuietly(hcon);
  }

  public void cancel() {
    cancelled = true;
  }
}
