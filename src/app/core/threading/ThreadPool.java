package app.core.threading;

import java.util.Vector;

public class ThreadPool {

  private final String poolName;
  private final int maxSize;
  private final ThreadManager threadManager;
  private final Vector taskQueue;
  private final Vector workerThreads;
  private final Object poolLock;
  private volatile boolean isShutdown;
  private int nextWorkerId;

  ThreadPool(String poolName, int maxSize, ThreadManager threadManager) {
    this.poolName = poolName;
    this.maxSize = maxSize;
    this.threadManager = threadManager;
    this.taskQueue = new Vector();
    this.workerThreads = new Vector();
    this.poolLock = new Object();
    this.isShutdown = false;
    this.nextWorkerId = 1;

    initializeWorkers();
  }

  private void initializeWorkers() {
    for (int i = 0; i < maxSize; i++) {
      createWorker();
    }
  }

  private void createWorker() {
    String workerName = poolName + "-Worker-" + nextWorkerId++;
    WorkerThread worker = new WorkerThread(workerName);

    synchronized (poolLock) {
      workerThreads.addElement(worker);
    }

    Thread thread = threadManager.createThread(worker, workerName);
    worker.setThread(thread);
    thread.start();
  }

  public void execute(Runnable task) {
    if (isShutdown) {
      throw new IllegalStateException("ThreadPool is shutdown");
    }

    if (task == null) {
      throw new IllegalArgumentException("Task cannot be null");
    }

    synchronized (poolLock) {
      taskQueue.addElement(task);
      poolLock.notifyAll();
    }
  }

  public void executeWithCallback(final Runnable task, final ThreadCallback callback) {
    execute(
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
        });
  }

  public int getQueueSize() {
    synchronized (poolLock) {
      return taskQueue.size();
    }
  }

  public void clearQueue() {
    synchronized (poolLock) {
      taskQueue.removeAllElements();
    }
  }

  public int getActiveWorkerCount() {
    synchronized (poolLock) {
      int count = 0;
      for (int i = 0; i < workerThreads.size(); i++) {
        WorkerThread worker = (WorkerThread) workerThreads.elementAt(i);
        if (worker.isActive()) {
          count++;
        }
      }
      return count;
    }
  }

  public String getPoolName() {
    return poolName;
  }

  public int getMaxSize() {
    return maxSize;
  }

  public boolean isShutdown() {
    return isShutdown;
  }

  void shutdown() {
    if (isShutdown) {
      return;
    }

    isShutdown = true;

    synchronized (poolLock) {
      poolLock.notifyAll();

      for (int i = 0; i < workerThreads.size(); i++) {
        WorkerThread worker = (WorkerThread) workerThreads.elementAt(i);
        worker.shutdown();
      }
    }

    waitForWorkersToComplete(3000);
  }

  private void waitForWorkersToComplete(long timeoutMs) {
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      boolean allComplete = true;

      synchronized (poolLock) {
        for (int i = 0; i < workerThreads.size(); i++) {
          WorkerThread worker = (WorkerThread) workerThreads.elementAt(i);
          if (worker.getThread() != null && worker.getThread().isAlive()) {
            allComplete = false;
            break;
          }
        }
      }

      if (allComplete) {
        break;
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  private class WorkerThread implements Runnable {
    private final String workerName;
    private Thread thread;
    private volatile boolean isActive;
    private volatile boolean shouldStop;

    WorkerThread(String workerName) {
      this.workerName = workerName;
      this.isActive = false;
      this.shouldStop = false;
    }

    void setThread(Thread thread) {
      this.thread = thread;
    }

    Thread getThread() {
      return thread;
    }

    boolean isActive() {
      return isActive;
    }

    void shutdown() {
      shouldStop = true;
      if (thread != null) {
        try {
          thread.interrupt();
        } catch (Exception e) {
        }
      }
    }

    public void run() {
      while (!shouldStop && !isShutdown) {
        Runnable task = null;

        synchronized (poolLock) {
          while (taskQueue.isEmpty() && !shouldStop && !isShutdown) {
            try {
              poolLock.wait();
            } catch (InterruptedException e) {
              if (shouldStop || isShutdown) {
                break;
              }
            }
          }

          if (!taskQueue.isEmpty() && !shouldStop && !isShutdown) {
            task = (Runnable) taskQueue.elementAt(0);
            taskQueue.removeElementAt(0);
          }
        }

        if (task != null) {
          try {
            isActive = true;
            task.run();
          } catch (Exception e) {
          } finally {
            isActive = false;
          }
        }
      }
    }
  }
}
