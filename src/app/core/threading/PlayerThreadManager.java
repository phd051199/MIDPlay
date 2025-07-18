package app.core.threading;

public class PlayerThreadManager {

  private static volatile PlayerThreadManager instance;
  private static final Object instanceLock = new Object();

  private static final int PLAYER_POOL_SIZE = 3;
  private static final int STATE_POOL_SIZE = 2;

  public static PlayerThreadManager getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new PlayerThreadManager();
        }
      }
    }
    return instance;
  }

  private final ThreadPool playerPool;
  private final ThreadPool statePool;
  private final ThreadManager threadManager;
  private volatile boolean isShutdown;

  private PlayerThreadManager() {
    threadManager = ThreadManager.getInstance();
    playerPool = threadManager.createThreadPool("PlayerPool", PLAYER_POOL_SIZE);
    statePool = threadManager.createThreadPool("PlayerStatePool", STATE_POOL_SIZE);
    isShutdown = false;
  }

  public void executePlayerTask(Runnable task) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    playerPool.execute(task);
  }

  public void executePlayerTaskWithCallback(Runnable task, ThreadCallback callback) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    playerPool.executeWithCallback(task, callback);
  }

  public void executeStateUpdate(Runnable task) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    statePool.execute(task);
  }

  public void executeStateUpdateWithCallback(Runnable task, ThreadCallback callback) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    statePool.executeWithCallback(task, callback);
  }

  public void executePlayerOperation(final Runnable task, final String operationName) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }

    playerPool.executeWithCallback(
        new Runnable() {
          public void run() {
            try {
              task.run();
            } catch (Exception e) {
            }
          }
        },
        new ThreadCallback() {
          public void onSuccess() {}

          public void onError(Exception e) {}
        });
  }

  public void executeEndOfMediaHandling(Runnable task) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    statePool.execute(task);
  }

  public void executeBufferingTask(Runnable task) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    playerPool.execute(task);
  }

  public void executePlayerInitialization(Runnable task, ThreadCallback callback) {
    if (isShutdown) {
      throw new IllegalStateException("PlayerThreadManager is shutdown");
    }
    playerPool.executeWithCallback(task, callback);
  }

  public int getPlayerPoolQueueSize() {
    return playerPool.getQueueSize();
  }

  public int getStatePoolQueueSize() {
    return statePool.getQueueSize();
  }

  public int getActivePlayerThreads() {
    return playerPool.getActiveWorkerCount();
  }

  public int getActiveStateThreads() {
    return statePool.getActiveWorkerCount();
  }

  public boolean isShutdown() {
    return isShutdown;
  }

  public void shutdown() {
    if (!isShutdown) {
      isShutdown = true;
      playerPool.shutdown();
      statePool.shutdown();
    }
  }

  public void forceShutdown() {
    shutdown();
  }

  public void clearQueues() {
    if (!isShutdown) {
      while (playerPool.getQueueSize() > 0) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          break;
        }
      }
      while (statePool.getQueueSize() > 0) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          break;
        }
      }
    }
  }
}
