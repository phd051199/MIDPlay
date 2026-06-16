package midplay.net;

import midplay.store.Configuration;
import midplay.store.SettingsManager;
import midplay.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class Network {
  private static final int BUFFER_SIZE = 4096;
  // Hard deadline for a single GET. Network responses here are small (JSON +
  // album art/text images), so this only ever fires on a genuinely hung socket
  // (half-open connection where the tower never sends FIN) — the #1 cause of
  // silent socket-pool exhaustion on S40 (pools are often 4–8 sockets). Closing
  // the connection from the watchdog forces the blocking getResponseCode()/read
  // to throw, releasing the slot.
  private static final long SOCKET_TIMEOUT_MS = 90000L;
  // Single shared timer so per-request watchdogs don't each spawn a thread.
  // CLDC 1.1's java.util.Timer has no daemon constructor; this is consistent
  // with the non-daemon worker threads the app already creates (NetworkOperation),
  // and the AMS terminates the VM on MIDlet exit.
  private static final Timer watchdogTimer = new Timer();

  private String userAgent;
  private volatile boolean cancelled = false;

  public Network() {
  }

  public static HttpConnection openConnection(String url) throws IOException {
    if (Utils.isBlackberry) {
      int blackberryWifi = SettingsManager.getInstance().getCurrentBlackberryWifi();

      if (blackberryWifi == Configuration.BLACKBERRY_WIFI_ON) {
        url += ";deviceside=true;interface=wifi";
      }
    }
    // NOTE: do NOT append ";ConnectionTimeout=..." here. That is a BlackBerry
    // Connector parameter; on Nokia/S40 (and other platforms) it is not a
    // recognized Connector param and leaks into the request URL, making the
    // server receive "...?query;ConnectionTimeout=15000" and return HTTP 500.
    // Use READ mode with the timeouts hint (3rd arg = true) instead: READ is
    // enough for GET, and `true` asks the implementation to throw
    // InterruptedIOException on connect stalls where supported.
    return (HttpConnection) Connector.open(url, Connector.READ, true);
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

  String sendHttpGet(String url) throws NetworkError {
    byte[] data = sendHttpGetBytes(url);
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  byte[] sendHttpGetBytes(String url) throws NetworkError {
    try {
      return sendHttpGetBytesOnce(url);
    } catch (NetworkError e) {
      if (cancelled) {
        throw e;
      }
      // One retry with a short backoff for transient 2G/EDGE failures.
      try {
        Thread.sleep(300);
      } catch (InterruptedException ie) {
        // Preserve interrupt status so a concurrent cancel() is honored.
        Thread.currentThread().interrupt();
        throw e;
      }
      if (cancelled) {
        throw e;
      }
      return sendHttpGetBytesOnce(url);
    }
  }

  private byte[] sendHttpGetBytesOnce(String url) throws NetworkError {
    HttpConnection hcon = null;
    DataInputStream dis = null;
    ByteArrayOutputStream response = new ByteArrayOutputStream();
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
      // Arm before the first blocking call (getResponseCode): if the socket
      // stalls past the deadline, the task closes the connection so the block
      // throws instead of holding the slot forever.
      watchdog = armWatchdog(hcon);
      int responseCode = hcon.getResponseCode();
      // Accept 200 and 206 (partial content) — some carrier/proxy stacks
      // transform GET responses into 206. Treat only >= 400 as an error.
      if (responseCode != HttpConnection.HTTP_OK
          && responseCode != HttpConnection.HTTP_PARTIAL) {
        throw new NetworkError("HTTP Error: " + responseCode);
      }
      // Size the buffer from Content-Length when the server provides it, so the
      // ByteArrayOutputStream doesn't grow (copy the whole buffer) repeatedly.
      long declaredLen = -1;
      try {
        declaredLen = hcon.getLength();
      } catch (Exception e) {
      }
      if (declaredLen > 0 && declaredLen < 524288) {
        response = new ByteArrayOutputStream((int) declaredLen);
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
        // CLDC Timer has no purge(); cancelled tasks self-evict from the timer
        // queue at their scheduled fire time, so nothing leaks long-term.
      }
      closeResources(dis, hcon);
    }
  }

  private TimerTask armWatchdog(final HttpConnection conn) {
    TimerTask task =
        new TimerTask() {
          public void run() {
            try {
              if (conn != null) {
                conn.close();
              }
            } catch (IOException e) {
            }
          }
        };
    watchdogTimer.schedule(task, SOCKET_TIMEOUT_MS);
    return task;
  }

  private void closeResources(DataInputStream dis, HttpConnection hcon) {
    if (dis != null) {
      try {
        dis.close();
      } catch (IOException e) {
      }
    }
    if (hcon != null) {
      try {
        hcon.close();
      } catch (IOException e) {
      }
    }
  }

  public void cancel() {
    cancelled = true;
  }
}
