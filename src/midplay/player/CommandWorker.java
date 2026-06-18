package midplay.player;

import java.util.Vector;

// Runs track-end and pending-change handling on a dedicated worker thread so the
// PlayerListener callback thread is never blocked. Owns its command queue and
// monitor, independent of the PlayerGUI playback lock. Each command is tagged
// with the playback session id it was issued under and dropped if stale.
final class CommandWorker {
  static final int TRACK_END = 1;
  static final int APPLY_PENDING = 2;

  private final PlayerGUI gui;
  private final Vector queue = new Vector();
  private Thread thread;
  private boolean stopRequested = false;

  CommandWorker(PlayerGUI gui) {
    this.gui = gui;
  }

  void start() {
    synchronized (queue) {
      if (thread != null && thread.isAlive()) {
        return;
      }

      stopRequested = false;
      thread =
          new Thread(
              new Runnable() {
                public void run() {
                  loop();
                }
              });
      thread.start();
    }
  }

  void stop() {
    synchronized (queue) {
      stopRequested = true;
      queue.removeAllElements();
      queue.notifyAll();
    }
  }

  void clearQueue() {
    synchronized (queue) {
      queue.removeAllElements();
    }
  }

  void enqueue(int commandType, int sessionId) {
    if (sessionId <= 0) {
      return;
    }
    start();
    synchronized (queue) {
      queue.addElement(new Task(commandType, sessionId));
      queue.notifyAll();
    }
  }

  private Task poll() {
    synchronized (queue) {
      while (queue.size() == 0 && !stopRequested) {
        try {
          queue.wait();
        } catch (InterruptedException e) {
        }
      }
      if (stopRequested) {
        return null;
      }

      Task command = (Task) queue.elementAt(0);
      queue.removeElementAt(0);
      return command;
    }
  }

  private void loop() {
    while (true) {
      Task command = poll();
      if (command == null) {
        synchronized (queue) {
          if (stopRequested) {
            stopRequested = false;
            thread = null;
            return;
          }
        }
        continue;
      }

      if (!gui.isCurrentSession(command.sessionId)) {
        if (command.type == TRACK_END) {
          gui.clearHandlingTrackEndFlag();
        }
        continue;
      }

      if (command.type == TRACK_END) {
        gui.processTrackEnd();
      } else if (command.type == APPLY_PENDING) {
        gui.applyPendingTrackChange();
      }
    }
  }

  private static final class Task {
    final int type;
    final int sessionId;

    Task(int type, int sessionId) {
      this.type = type;
      this.sessionId = sessionId;
    }
  }
}
