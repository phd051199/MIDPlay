package midplay.player;

import midplay.store.Configuration;

// The two media-loading strategies, replacing the "pass_inputstream"/"pass_url"
// string type-code that was branched on across PlayerGUI / MediaResolver /
// PlaybackRecovery. Persisted as its code() string (SettingsManager), so this
// only changes the in-memory representation — not the storage format.
final class PlaybackMethod {
  static final PlaybackMethod INPUT_STREAM =
      new PlaybackMethod(Configuration.PLAYER_METHOD_PASS_INPUTSTREAM, true);
  static final PlaybackMethod URL =
      new PlaybackMethod(Configuration.PLAYER_METHOD_PASS_URL, false);

  private final String code;
  private final boolean inputStream;

  private PlaybackMethod(String code, boolean inputStream) {
    this.code = code;
    this.inputStream = inputStream;
  }

  // The persisted identifier (matches Configuration.PLAYER_METHOD_PASS_*).
  String code() {
    return code;
  }

  boolean isInputStream() {
    return inputStream;
  }

  // The alternate method, used when this one fails (PlaybackRecovery fallback).
  PlaybackMethod fallback() {
    return inputStream ? URL : INPUT_STREAM;
  }

  // Resolve a stored code to a method; unknown values default to URL, matching
  // the former getConfiguredPlayerHttpMethod default.
  static PlaybackMethod fromCode(String code) {
    return Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(code) ? INPUT_STREAM : URL;
  }
}
