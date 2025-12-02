import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class Network implements Runnable {
  private static final int BUFFER_SIZE = 8192;
  private final NetworkListener listener;
  private String url;
  private Thread thread;
  private String userAgent;
  private volatile boolean cancelled = false;

  public Network(NetworkListener listener) {
    this.listener = listener;
  }

  public Network() {
    this(null);
  }

  private static final isBlackberry;

  static {
    String platform = System.getProperty("microedition.platform");
    isBlackberry = (platform != null && platform.toLowerCase().startsWith("blackberry"));
  }

  public static HttpConnection openConnection(String url) throws IOException {
    if (isBlackberry) {
      int blackberryWifi = SettingsManager.getInstance().getCurrentBlackberryWifi();

      if (blackberryWifi == Configuration.BLACKBERRY_WIFI_ON) {
        url += ";deviceside=true;interface=wifi";
      }
    }
    return (HttpConnection) Connector.open(url);
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

  private String sendHttpGet(String url) throws NetworkError {
    byte[] data = sendHttpGetBytes(url);
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  private byte[] sendHttpGetBytes(String url) throws NetworkError {
    HttpConnection hcon = null;
    DataInputStream dis = null;
    ByteArrayOutputStream response = new ByteArrayOutputStream();
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
      int responseCode = hcon.getResponseCode();
      if (responseCode != HttpConnection.HTTP_OK) {
        throw new NetworkError("HTTP Error: " + responseCode);
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
      closeResources(dis, hcon);
    }
  }

  public void run() {
    try {
      String response = this.sendHttpGet(this.url);
      if (!cancelled) {
        this.listener.onResponse(response);
      }
    } catch (NetworkError e) {
      if (!cancelled) {
        this.listener.onError(e);
      }
    }
  }

  public void startHttpGet(String url) {
    this.url = url;
    this.cancelled = false;
    this.thread = new Thread(this);
    this.thread.start();
  }

  public void startHttpGetBytes(String url, final BinaryNetworkListener binaryListener) {
    this.url = url;
    this.cancelled = false;
    this.thread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  byte[] response = sendHttpGetBytes(Network.this.url);
                  if (!cancelled) {
                    binaryListener.onBinaryResponse(response);
                  }
                } catch (NetworkError e) {
                  if (!cancelled) {
                    binaryListener.onError(e);
                  }
                }
              }
            });
    this.thread.start();
  }

  private void closeResources(DataInputStream dis, HttpConnection hcon) {
    if (dis != null) {
      try {
        dis.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (hcon != null) {
      try {
        hcon.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void cancel() {
    cancelled = true;
    if (thread != null) {
      thread.interrupt();
    }
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public interface NetworkListener {
    void onResponse(String var);

    void onError(Exception e);
  }

  public interface BinaryNetworkListener {
    void onBinaryResponse(byte[] data);

    void onError(Exception e);
  }

  public class NetworkError extends Exception {
    public NetworkError(String message) {
      super(message);
    }
  }
}
