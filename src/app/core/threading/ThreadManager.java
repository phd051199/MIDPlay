package app.core.threading;

import java.util.Hashtable;
import java.util.Vector;

public class ThreadManager {

  private static volatile ThreadManager instance;
  private static final Object instanceLock = new Object();

  private static final int MAX_POOL_SIZE = 5;
  private static final String DEFAULT_THREAD_PREFIX = "MIDPlay-Thread-";
  private static final int CLEANUP_INTERVAL_MS = 30000;
  private static final int MAX_THREAD_AGE_MS = 300000;

  public static ThreadManager getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new ThreadManager();
        }
      }
    }
    return instance;
  }

  private final Hashtable managedThreads;
  private final Vector threadPools;
  private final Object threadsLock;
  private volatile boolean isShuttingDown;
  private int nextThreadId;
  private long lastCleanupTime;

  private ThreadManager() {
    managedThreads = new Hashtable();
    threadPools = new Vector();
    threadsLock = new Object();
    nextThreadId = 1;
    isShuttingDown = false;
    lastCleanupTime = System.currentTimeMillis();

    addShutdownHook();
  }

  public Thread createThread(Runnable task, String name) {
    if (isShuttingDown) {
      throw new IllegalStateException("ThreadManager is shutting down");
    }

    performPeriodicCleanup();

    if (name == null || name.trim().length() == 0) {
      name = DEFAULT_THREAD_PREFIX + getNextThreadId();
    }

    Thread thread = new Thread(new ManagedRunnable(task, name), name);

    synchronized (threadsLock) {
      managedThreads.put(name, new ThreadInfo(thread, System.currentTimeMillis()));
    }

    return thread;
  }

  public Thread createAndStartThread(Runnable task, String name) {
    Thread thread = createThread(task, name);
    thread.start();
    return thread;
  }

  public ThreadPool createThreadPool(String poolName, int maxSize) {
    if (maxSize <= 0 || maxSize > MAX_POOL_SIZE) {
      maxSize = MAX_POOL_SIZE;
    }

    ThreadPool pool = new ThreadPool(poolName, maxSize, this);
    synchronized (threadsLock) {
      threadPools.addElement(pool);
    }
    return pool;
  }

  public void executeAsync(Runnable task, String threadName) {
    createAndStartThread(task, threadName);
  }

  public void executeWithCallback(
      final Runnable task, final ThreadCallback callback, String threadName) {
    createAndStartThread(
        new Runnable() {
          public void run() {
            try {
              task.run();
              if (callback != null) {
                callback.onSuccess();
              }
            } catch (Exception e) {
              if (callback != null) {
                callback.onError(e);
              }
            }
          }
        },
        threadName);
  }

  public boolean interruptThread(String threadName) {
    synchronized (threadsLock) {
      ThreadInfo info = (ThreadInfo) managedThreads.get(threadName);
      if (info != null && info.thread.isAlive()) {
        try {
          info.thread.interrupt();
          return true;
        } catch (Exception e) {
          return false;
        }
      }
    }
    return false;
  }

  public boolean isThreadAlive(String threadName) {
    synchronized (threadsLock) {
      ThreadInfo info = (ThreadInfo) managedThreads.get(threadName);
      return info != null && info.thread.isAlive();
    }
  }

  public int getActiveThreadCount() {
    synchronized (threadsLock) {
      int count = 0;
      java.util.Enumeration keys = managedThreads.keys();
      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        ThreadInfo info = (ThreadInfo) managedThreads.get(key);
        if (info.thread.isAlive()) {
          count++;
        }
      }
      return count;
    }
  }

  private void cleanupDeadThreads() {
    synchronized (threadsLock) {
      Vector keysToRemove = new Vector();
      java.util.Enumeration keys = managedThreads.keys();
      long currentTime = System.currentTimeMillis();

      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        ThreadInfo info = (ThreadInfo) managedThreads.get(key);

        if (!info.thread.isAlive() || (currentTime - info.createdTime) > MAX_THREAD_AGE_MS) {
          keysToRemove.addElement(key);
        }
      }

      for (int i = 0; i < keysToRemove.size(); i++) {
        String key = (String) keysToRemove.elementAt(i);
        ThreadInfo info = (ThreadInfo) managedThreads.get(key);
        if (info != null && info.thread.isAlive()) {
          try {
            info.thread.interrupt();
          } catch (Exception e) {
          }
        }
        managedThreads.remove(key);
      }

      lastCleanupTime = currentTime;
    }
  }

  private void performPeriodicCleanup() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
      cleanupDeadThreads();
    }
  }

  private synchronized int getNextThreadId() {
    return nextThreadId++;
  }

  private void addShutdownHook() {}

  public void shutdown() {
    if (isShuttingDown) {
      return;
    }

    isShuttingDown = true;

    shutdownAllThreadPools();
    interruptAllThreads();
    waitForThreadsToComplete(5000);
  }

  private void shutdownAllThreadPools() {
    synchronized (threadsLock) {
      for (int i = 0; i < threadPools.size(); i++) {
        ThreadPool pool = (ThreadPool) threadPools.elementAt(i);
        pool.shutdown();
      }
      threadPools.removeAllElements();
    }
  }

  private void interruptAllThreads() {
    synchronized (threadsLock) {
      java.util.Enumeration keys = managedThreads.keys();
      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        ThreadInfo info = (ThreadInfo) managedThreads.get(key);
        if (info.thread.isAlive()) {
          try {
            info.thread.interrupt();
          } catch (Exception e) {
          }
        }
      }
    }
  }

  private void waitForThreadsToComplete(long timeoutMs) {
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      if (getActiveThreadCount() == 0) {
        break;
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  void removeThread(String threadName) {
    synchronized (threadsLock) {
      managedThreads.remove(threadName);
    }
  }

  private static class ThreadInfo {
    final Thread thread;
    final long createdTime;

    ThreadInfo(Thread thread, long createdTime) {
      this.thread = thread;
      this.createdTime = createdTime;
    }
  }

  private class ManagedRunnable implements Runnable {
    private final Runnable originalTask;
    private final String threadName;

    ManagedRunnable(Runnable task, String name) {
      this.originalTask = task;
      this.threadName = name;
    }

    public void run() {
      try {
        originalTask.run();
      } finally {
        removeThread(threadName);
      }
    }
  }
}
