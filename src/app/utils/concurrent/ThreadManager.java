package app.utils.concurrent;

import app.MIDPlay;
import java.util.Vector;

public class ThreadManager {

  private static final int DEFAULT_TIMEOUT_MS = 2000;
  private static final int INTERRUPT_TIMEOUT_MS = 1000;

  public static boolean cleanupThread(Thread thread, int timeoutMs) {
    if (thread == null) {
      return true;
    }

    if (!thread.isAlive()) {
      return true;
    }

    try {
      thread.interrupt();

      long startTime = System.currentTimeMillis();
      while (thread.isAlive() && (System.currentTimeMillis() - startTime) < timeoutMs) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          break;
        }
      }

      return !thread.isAlive();

    } catch (Exception e) {
      return false;
    }
  }

  public static boolean cleanupThread(Thread thread) {
    return cleanupThread(thread, DEFAULT_TIMEOUT_MS);
  }

  public static int cleanupThreads(Thread[] threads, int timeoutMs) {
    if (threads == null) {
      return 0;
    }

    int cleanedUp = 0;
    for (int i = 0; i < threads.length; i++) {
      if (cleanupThread(threads[i], timeoutMs)) {
        cleanedUp++;
      }
    }
    return cleanedUp;
  }

  public static int cleanupThreads(Thread[] threads) {
    return cleanupThreads(threads, DEFAULT_TIMEOUT_MS);
  }

  public static Thread createThread(Runnable runnable, String name) {
    Thread thread = new Thread(runnable);
    return thread;
  }

  public static Thread createLowPriorityThread(Runnable runnable, String name) {
    Thread thread = new Thread(runnable);
    try {
      thread.setPriority(Thread.MIN_PRIORITY);
    } catch (Exception e) {
    }
    return thread;
  }

  public static boolean safeStartThread(Thread thread) {
    if (thread == null) {
      return false;
    }

    try {
      thread.start();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static void runOnUiThread(final Runnable runnable) {
    if (runnable == null) {
      return;
    }

    try {
      MIDPlay.getInstance().getDisplay().callSerially(runnable);
    } catch (Exception e) {
      runnable.run();
    }
  }

  public static boolean runInBackground(final Runnable runnable, String name) {
    if (runnable == null) {
      return false;
    }

    Thread thread = createThread(runnable, name);
    return safeStartThread(thread);
  }

  public static boolean runInBackground(final Runnable runnable) {
    return runInBackground(runnable, "BackgroundTask");
  }

  private ThreadManager() {}

  public static class SimpleThreadPool {
    private final Vector activeThreads;
    private final int maxThreads;
    private boolean isShutdown;

    public SimpleThreadPool(int maxThreads) {
      this.maxThreads = maxThreads;
      this.activeThreads = new Vector(maxThreads);
      this.isShutdown = false;
    }

    public synchronized boolean execute(Runnable runnable, String name) {
      if (isShutdown) {
        return false;
      }

      cleanupFinishedThreads();

      if (activeThreads.size() >= maxThreads) {
        return false;
      }

      Thread thread = createThread(runnable, name);
      if (safeStartThread(thread)) {
        activeThreads.addElement(thread);
        return true;
      }

      return false;
    }

    private void cleanupFinishedThreads() {
      for (int i = activeThreads.size() - 1; i >= 0; i--) {
        Thread thread = (Thread) activeThreads.elementAt(i);
        if (!thread.isAlive()) {
          activeThreads.removeElementAt(i);
        }
      }
    }

    public synchronized void shutdown() {
      isShutdown = true;

      Thread[] threads = new Thread[activeThreads.size()];
      for (int i = 0; i < activeThreads.size(); i++) {
        threads[i] = (Thread) activeThreads.elementAt(i);
      }

      cleanupThreads(threads, INTERRUPT_TIMEOUT_MS);

      activeThreads.removeAllElements();
    }

    public synchronized int getActiveThreadCount() {
      cleanupFinishedThreads();
      return activeThreads.size();
    }

    public synchronized boolean isShutdown() {
      return isShutdown;
    }
  }
}
