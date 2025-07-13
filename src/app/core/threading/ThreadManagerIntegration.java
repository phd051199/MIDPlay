package app.core.threading;

import app.core.data.DataLoader;
import app.core.data.LoadDataListener;
import app.core.network.RestCallback;
import app.core.network.RestClient;
import app.utils.ImageUtils;
import java.util.Vector;

public class ThreadManagerIntegration {

  private static final ThreadManager threadManager = ThreadManager.getInstance();
  private static final ThreadPool networkPool = threadManager.createThreadPool("NetworkPool", 5);
  private static ThreadPool dataPool = threadManager.createThreadPool("DataPool", 3);
  private static final ThreadPool uiPool = threadManager.createThreadPool("UIPool", 3);
  private static final PlayerThreadManager playerThreadManager = PlayerThreadManager.getInstance();
  private static final Object dataPoolLock = new Object();

  public static void executeNetworkRequest(final String url, final RestCallback callback) {
    networkPool.executeWithCallback(
        new Runnable() {
          public void run() {
            try {
              RestClient client = RestClient.getInstance();
              String response = client.get(url);
              callback.success(response);
            } catch (Exception e) {
              callback.error(e);
            }
          }
        },
        new ThreadCallback() {
          public void onSuccess() {}

          public void onError(Exception e) {
            callback.error(e);
          }
        });
  }

  public static void loadDataAsync(final DataLoader loader, final LoadDataListener listener) {
    ensureDataPoolAvailable();
    dataPool.executeWithCallback(
        new CancellableDataTask(loader, listener),
        new ThreadCallback() {
          public void onSuccess() {}

          public void onError(Exception e) {
            listener.loadError();
          }
        });
  }

  private static void ensureDataPoolAvailable() {
    synchronized (dataPoolLock) {
      if (dataPool == null || dataPool.isShutdown()) {
        try {
          dataPool = threadManager.createThreadPool("DataPool", 3);
        } catch (Exception e) {
          throw new RuntimeException("Failed to create DataPool: " + e.getMessage());
        }
      }
    }
  }

  public static void cancelPendingDataOperations() {
    CancellableDataTask.cancelAllPendingTasks();
    synchronized (dataPoolLock) {
      if (!dataPool.isShutdown()) {
        dataPool.clearQueue();
      }
    }
  }

  public static void executeUITask(Runnable task, String taskName) {
    uiPool.execute(task);
  }

  public static void executeBackgroundTask(Runnable task, String taskName) {
    threadManager.executeAsync(task, taskName);
  }

  public static void executeWithTimeout(
      final Runnable task, final String taskName, final long timeoutMs) {
    final Thread taskThread = threadManager.createAndStartThread(task, taskName);

    threadManager.executeAsync(
        new Runnable() {
          public void run() {
            try {
              Thread.sleep(timeoutMs);
              if (taskThread.isAlive()) {
                taskThread.interrupt();
              }
            } catch (InterruptedException e) {
            }
          }
        },
        taskName + "-Timeout");
  }

  public static void scheduleDelayedTask(
      final Runnable task, final String taskName, final long delayMs) {
    threadManager.executeAsync(
        new Runnable() {
          public void run() {
            try {
              Thread.sleep(delayMs);
              task.run();
            } catch (InterruptedException e) {
            }
          }
        },
        taskName + "-Delayed");
  }

  public static void executeImageLoading(final String imageUrl, final ImageLoadCallback callback) {
    executeImageLoading(imageUrl, 100, callback);
  }

  public static void executeImageLoading(
      final String imageUrl, final int size, final ImageLoadCallback callback) {
    uiPool.executeWithCallback(
        new Runnable() {
          public void run() {
            try {
              Object image = ImageUtils.getImage(imageUrl, size);
              callback.onImageLoaded(image);
            } catch (Exception e) {
              callback.onImageLoadError(e);
            }
          }
        },
        new ThreadCallback() {
          public void onSuccess() {}

          public void onError(Exception e) {
            callback.onImageLoadError(e);
          }
        });
  }

  public static void executeSettingsSave(final Runnable saveTask) {
    ThreadSafetyUtils.synchronizedExecute("settings-save", saveTask);
  }

  public static void shutdownAllPools() {
    networkPool.shutdown();
    dataPool.shutdown();
    uiPool.shutdown();
    playerThreadManager.shutdown();
  }

  public static void shutdownDataPool() {
    synchronized (dataPoolLock) {
      if (!dataPool.isShutdown()) {
        dataPool.shutdown();
      }
    }
  }

  public static void recreateDataPool() {
    synchronized (dataPoolLock) {
      if (dataPool == null || dataPool.isShutdown()) {
        try {
          dataPool = threadManager.createThreadPool("DataPool", 3);
        } catch (Exception e) {
          throw new RuntimeException("Failed to recreate DataPool: " + e.getMessage());
        }
      }
    }
  }

  public static ThreadPool getNetworkPool() {
    return networkPool;
  }

  public static ThreadPool getDataPool() {
    ensureDataPoolAvailable();
    return dataPool;
  }

  public static ThreadPool getUIPool() {
    return uiPool;
  }

  public static ThreadManager getThreadManager() {
    return threadManager;
  }

  public static PlayerThreadManager getPlayerThreadManager() {
    return playerThreadManager;
  }

  public static void executePlayerTask(Runnable task, String taskName) {
    playerThreadManager.executePlayerTask(task);
  }

  public static void executePlayerTaskWithCallback(Runnable task, ThreadCallback callback) {
    playerThreadManager.executePlayerTaskWithCallback(task, callback);
  }

  public static void executePlayerStateUpdate(Runnable task) {
    playerThreadManager.executeStateUpdate(task);
  }

  public static void executePlayerOperation(Runnable task, String operationName) {
    playerThreadManager.executePlayerOperation(task, operationName);
  }

  public static void executeEndOfMediaHandling(Runnable task) {
    playerThreadManager.executeEndOfMediaHandling(task);
  }

  public static void executePlayerInitialization(Runnable task, ThreadCallback callback) {
    playerThreadManager.executePlayerInitialization(task, callback);
  }

  public static void executePlayerBuffering(Runnable task) {
    playerThreadManager.executeBufferingTask(task);
  }

  public static void executePlayerImageLoading(
      final String imageUrl, final ImageLoadCallback callback) {
    executePlayerImageLoading(imageUrl, 100, callback);
  }

  public static void executePlayerImageLoading(
      final String imageUrl, final int size, final ImageLoadCallback callback) {
    playerThreadManager.executePlayerTaskWithCallback(
        new Runnable() {
          public void run() {
            try {
              Object image = ImageUtils.getImage(imageUrl, size);
              callback.onImageLoaded(image);
            } catch (Exception e) {
              callback.onImageLoadError(e);
            }
          }
        },
        new ThreadCallback() {
          public void onSuccess() {}

          public void onError(Exception e) {
            callback.onImageLoadError(e);
          }
        });
  }

  public static void clearPlayerQueues() {
    playerThreadManager.clearQueues();
  }

  public static Runnable wrapSafe(Runnable task, String taskName) {
    return new SafeRunnable(task, taskName);
  }

  private ThreadManagerIntegration() {}

  private static class CancellableDataTask implements Runnable {
    private static final Vector activeTasks = new Vector();
    private static final Object tasksLock = new Object();

    private static void cleanupOldTasks() {
      long currentTime = System.currentTimeMillis();
      for (int i = activeTasks.size() - 1; i >= 0; i--) {
        CancellableDataTask task = (CancellableDataTask) activeTasks.elementAt(i);
        if (currentTime - task.creationTime > 300000) {
          task.cancel();
          activeTasks.removeElementAt(i);
        }
      }
    }

    public static void cancelAllPendingTasks() {
      synchronized (tasksLock) {
        for (int i = 0; i < activeTasks.size(); i++) {
          CancellableDataTask task = (CancellableDataTask) activeTasks.elementAt(i);
          task.cancel();
        }
        activeTasks.removeAllElements();
      }
    }

    public static int getActiveTaskCount() {
      synchronized (tasksLock) {
        return activeTasks.size();
      }
    }

    private final DataLoader loader;
    private final LoadDataListener listener;
    private volatile boolean isCancelled = false;
    private final long creationTime;

    CancellableDataTask(DataLoader loader, LoadDataListener listener) {
      this.loader = loader;
      this.listener = listener;
      this.creationTime = System.currentTimeMillis();
      synchronized (tasksLock) {
        activeTasks.addElement(this);
        cleanupOldTasks();
      }
    }

    public void run() {
      try {
        if (isCancelled) {
          return;
        }

        Vector items = loader.load();

        if (isCancelled) {
          return;
        }

        if (items == null) {
          if (!isCancelled) {
            listener.loadError();
          }
        } else if (items.isEmpty()) {
          if (!isCancelled) {
            listener.noData();
          }
        } else {
          if (!isCancelled) {
            listener.loadDataCompleted(items);
          }
        }
      } catch (Exception e) {
        if (!isCancelled) {
          listener.loadError();
        }
      } finally {
        synchronized (tasksLock) {
          activeTasks.removeElement(this);
        }
      }
    }

    public void cancel() {
      isCancelled = true;
    }
  }

  public static class SafeRunnable implements Runnable {
    private final Runnable task;
    private final String taskName;

    public SafeRunnable(Runnable task, String taskName) {
      this.task = task;
      this.taskName = taskName;
    }

    public void run() {
      try {
        task.run();
      } catch (Exception e) {
      }
    }
  }

  public interface ImageLoadCallback {
    void onImageLoaded(Object image);

    void onImageLoadError(Exception e);
  }
}
