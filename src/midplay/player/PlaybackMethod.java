package midplay.player;

import midplay.store.Configuration;

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

  static PlaybackMethod fromCode(String code) {
    return Configuration.PLAYER_METHOD_PASS_INPUTSTREAM.equals(code) ? INPUT_STREAM : URL;
  }
}
