package midplay.player;

import midplay.store.Configuration;

// The two media-loading strategies (inputstream vs url). Persisted as a code
// string in SettingsManager; this only names the in-memory representation.
final class PlaybackMethod {
  static final PlaybackMethod INPUT_STREAM = new PlaybackMethod(true);
  static final PlaybackMethod URL = new PlaybackMethod(false);

  private final boolean inputStream;

  private PlaybackMethod(boolean inputStream) {
    this.inputStream = inputStream;
  }

  boolean isInputStream() {
    return inputStream;
  }

  // Resolve a stored code to a method; unknown values default to URL.
  static PlaybackMethod fromCode(String code) {
    return Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(code) ? INPUT_STREAM : URL;
  }
}
