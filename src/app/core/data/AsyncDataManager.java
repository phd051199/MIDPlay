package app.core.data;

import app.core.network.RestClient;
import app.utils.concurrent.ThreadManager;
import java.util.Vector;

public class AsyncDataManager {

  private static AsyncDataManager instance = null;

  public static synchronized AsyncDataManager getInstance() {
    if (instance == null) {
      instance = new AsyncDataManager();
    }
    return instance;
  }

  public static DataCallback adaptListener(final LoadDataListener listener) {
    return new DataCallback() {
      public void onDataLoaded(Vector data) {
        listener.loadDataCompleted(data);
      }

      public void onNoData() {
        listener.noData();
      }

      public void onError(Exception error) {
        listener.loadError();
      }
    };
  }

  private final ThreadManager.SimpleThreadPool dataThreadPool;

  private AsyncDataManager() {
    this.dataThreadPool = new ThreadManager.SimpleThreadPool(3);
  }

  public boolean loadDataAsync(final DataLoader loader, final LoadDataListener listener) {
    if (loader == null || listener == null) {
      return false;
    }

    return dataThreadPool.execute(
        new Runnable() {
          public void run() {
            try {
              Vector items = loader.load();
              if (items == null) {
                listener.loadError();
              } else if (items.isEmpty()) {
                listener.noData();
              } else {
                listener.loadDataCompleted(items);
              }
            } catch (Exception e) {
              listener.loadError();
            }
          }
        },
        "DataLoader");
  }

  public boolean loadDataAsync(final DataLoader loader, final DataCallback callback) {
    if (loader == null || callback == null) {
      return false;
    }

    return dataThreadPool.execute(
        new Runnable() {
          public void run() {
            try {
              Vector items = loader.load();
              if (items == null) {
                callback.onError(new Exception("No data returned"));
              } else if (items.isEmpty()) {
                callback.onNoData();
              } else {
                callback.onDataLoaded(items);
              }
            } catch (Exception e) {
              callback.onError(e);
            }
          }
        },
        "DataLoader");
  }

  public boolean executeAsync(final Runnable operation, final SimpleCallback callback) {
    if (operation == null) {
      return false;
    }

    return dataThreadPool.execute(
        new Runnable() {
          public void run() {
            try {
              operation.run();
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
        "AsyncOperation");
  }

  public boolean executeAsync(final Runnable operation) {
    return executeAsync(operation, null);
  }

  public boolean getAsync(final String url, final NetworkCallback callback) {
    if (url == null || callback == null) {
      return false;
    }

    return dataThreadPool.execute(
        new Runnable() {
          public void run() {
            try {
              final String result = RestClient.getInstance().get(url);
              if (result != null && result.length() > 0) {
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        callback.onSuccess(result);
                      }
                    });
              } else {
                ThreadManager.runOnUiThread(
                    new Runnable() {
                      public void run() {
                        callback.onError(new Exception("Empty response"));
                      }
                    });
              }
            } catch (final Exception e) {
              ThreadManager.runOnUiThread(
                  new Runnable() {
                    public void run() {
                      callback.onError(e);
                    }
                  });
            }
          }
        },
        "AsyncNetworkGet");
  }

  public int getActiveOperationCount() {
    return dataThreadPool.getActiveThreadCount();
  }

  public boolean isAvailable() {
    return !dataThreadPool.isShutdown();
  }

  public void shutdown() {
    dataThreadPool.shutdown();
  }

  public interface DataCallback {
    void onDataLoaded(Vector data);

    void onNoData();

    void onError(Exception error);
  }

  public interface SimpleCallback {
    void onSuccess();

    void onError(Exception error);
  }

  public interface NetworkCallback {
    void onSuccess(String result);

    void onError(Exception error);

    void onCancelled();
  }
}
