package midplay.net;

// Checked exception for network failures.
public class NetworkError extends Exception {
  public NetworkError(String message) {
    super(message);
  }
}
