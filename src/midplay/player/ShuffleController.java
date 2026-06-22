package midplay.player;

import java.util.Random;

final class ShuffleController {
  private int[] order;
  private int position;
  private final Random random = new Random();

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

  void append(int newSize) {
    if (order == null || newSize <= order.length) {
      return;
    }
    int[] grown = new int[newSize];
    System.arraycopy(order, 0, grown, 0, order.length);
    for (int i = order.length; i < newSize; i++) {
      grown[i] = i;
    }
    order = grown;
  }

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

  boolean hasNext() {
    if (order == null || order.length == 0) {
      return false;
    }
    return position < order.length - 1;
  }

  boolean hasPrev() {
    if (order == null || order.length == 0) {
      return false;
    }
    return position > 0;
  }

  void clear() {
    order = null;
    position = 0;
  }
}
