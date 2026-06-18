package midplay.player;

import java.util.Random;

// Shuffle-order state for PlayerGUI: owns the shuffled index sequence and the
// cursor into it. Pure stateful helper — PlayerGUI holds the playback lock at
// every call site, so no internal synchronization is needed.
final class ShuffleController {
  private int[] order;
  private int position;
  private final Random random = new Random();

  // Advance the shuffle cursor. Returns the new current-track index, or -1 if the
  // track did not change (single-track list, or OFF-repeat at the boundary).
  // Lazily (re)builds the order when empty; on an ALL-repeat wrap it rebuilds a
  // fresh order from position 0.
  int advance(boolean forward, boolean repeatOff, int size, int currentIndex) {
    if (order == null || order.length == 0) {
      rebuild(size, currentIndex);
      if (order == null || order.length == 0) {
        return -1;
      }
    }

    if (order.length == 1) {
      return -1;
    }

    if (forward) {
      position++;
      if (position >= order.length) {
        if (repeatOff) {
          position = order.length - 1;
          return -1;
        }
        rebuild(size, currentIndex);
        position = 0;
      }
    } else {
      position--;
      if (position < 0) {
        if (repeatOff) {
          position = 0;
          return -1;
        }
        position = order.length - 1;
      }
    }

    return order[position];
  }

  // Build a fresh Fisher-Yates shuffle of [0, size) and point the cursor at the
  // current track. Clears when size <= 0.
  void rebuild(int size, int currentIndex) {
    if (size <= 0) {
      order = null;
      position = 0;
      return;
    }

    order = new int[size];
    for (int i = 0; i < size; i++) {
      order[i] = i;
    }

    for (int i = size - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int temp = order[i];
      order[i] = order[j];
      order[j] = temp;
    }

    position = 0;
    for (int i = 0; i < size; i++) {
      if (order[i] == currentIndex) {
        position = i;
        break;
      }
    }
  }

  // True if a forward step remains before the OFF-repeat boundary.
  boolean hasNext() {
    if (order == null || order.length == 0) {
      return false;
    }
    return position < order.length - 1;
  }

  void clear() {
    order = null;
    position = 0;
  }
}
