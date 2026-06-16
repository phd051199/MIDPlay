package midplay.net;

// Top-level checked exception for network failures (was a non-static inner
// class of Network, which needlessly captured the enclosing instance).
public class NetworkError extends Exception {
  public NetworkError(String message) {
    super(message);
  }
}
