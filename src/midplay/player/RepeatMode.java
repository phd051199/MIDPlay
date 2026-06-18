package midplay.player;

import midplay.store.Configuration;

// Type-safe helpers around the repeat-mode int (persisted as Configuration.PLAYER_REPEAT_*).
final class RepeatMode {
  static int next(int mode) {
    if (mode == Configuration.PLAYER_REPEAT_OFF) {
      return Configuration.PLAYER_REPEAT_ONE;
    }
    if (mode == Configuration.PLAYER_REPEAT_ONE) {
      return Configuration.PLAYER_REPEAT_ALL;
    }
    return Configuration.PLAYER_REPEAT_OFF;
  }

  static boolean isOff(int mode) {
    return mode == Configuration.PLAYER_REPEAT_OFF;
  }

  static boolean isOne(int mode) {
    return mode == Configuration.PLAYER_REPEAT_ONE;
  }

  static boolean isAll(int mode) {
    return mode == Configuration.PLAYER_REPEAT_ALL;
  }

  private RepeatMode() {}
}
