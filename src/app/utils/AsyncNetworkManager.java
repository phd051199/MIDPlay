package app.utils;

import app.common.RestClient;
import java.io.IOException;

public class AsyncNetworkManager {

  private static AsyncNetworkManager instance = null;
  private final ThreadManager.SimpleThreadPool networkThreadPool;

  public interface NetworkCallback {
    void onSuccess(String result);

    void onError(Exception error);

    void onCancelled();
  }

  public interface ByteDataCallback {
    void onSuccess(byte[] data);

    void onError(Exception error);

    void onCancelled();
  }

  private AsyncNetworkManager() {
    this.networkThreadPool = new ThreadManager.SimpleThreadPool(4);
  }

  public static synchronized AsyncNetworkManager getInstance() {
    if (instance == null) {
      instance = new AsyncNetworkManager();
    }
    return instance;
  }

  public boolean getAsync(final String url, final NetworkCallback callback) {
    if (url == null || callback == null) {
      return false;
    }

    return networkThreadPool.execute(
        new Runnable() {
          public void run() {
            try {
              String result = RestClient.getInstance().get(url);
              callback.onSuccess(result);
            } catch (IOException e) {
              callback.onError(e);
            } catch (Exception e) {
              callback.onError(e);
            }
          }
        },
        "AsyncGet");
  }

  public boolean getBytesAsync(final String url, final ByteDataCallback callback) {
    if (url == null || callback == null) {
      return false;
    }

    return networkThreadPool.execute(
        new Runnable() {
          public void run() {
            try {
              byte[] data = RestClient.getInstance().getBytes(url);
              callback.onSuccess(data);
            } catch (IOException e) {
              callback.onError(e);
            } catch (Exception e) {
              callback.onError(e);
            }
          }
        },
        "AsyncGetBytes");
  }

  public int getMultipleAsync(final String[] urls, final MultipleRequestCallback callback) {
    if (urls == null || callback == null) {
      return 0;
    }

    int queued = 0;
    for (int i = 0; i < urls.length; i++) {
      final int index = i;
      final String url = urls[i];

      if (url != null) {
        boolean success =
            networkThreadPool.execute(
                new Runnable() {
                  public void run() {
                    try {
                      String result = RestClient.getInstance().get(url);
                      callback.onSingleSuccess(index, url, result);
                    } catch (Exception e) {
                      callback.onSingleError(index, url, e);
                    }
                  }
                },
                "AsyncMultiGet");

        if (success) {
          queued++;
        }
      }
    }

    return queued;
  }

  public interface MultipleRequestCallback {
    void onSingleSuccess(int index, String url, String result);

    void onSingleError(int index, String url, Exception error);
  }

  public boolean getWithRetryAsync(
      final String url,
      final int maxRetries,
      final int retryDelayMs,
      final NetworkCallback callback) {
    if (url == null || callback == null) {
      return false;
    }

    return networkThreadPool.execute(
        new Runnable() {
          public void run() {
            int attempts = 0;
            Exception lastError = null;

            while (attempts <= maxRetries) {
              try {
                String result = RestClient.getInstance().get(url);
                callback.onSuccess(result);
                return;
              } catch (Exception e) {
                lastError = e;
                attempts++;

                if (attempts <= maxRetries) {
                  try {
                    Thread.sleep(retryDelayMs);
                  } catch (InterruptedException ie) {
                    callback.onCancelled();
                    return;
                  }
                }
              }
            }

            callback.onError(lastError);
          }
        },
        "AsyncRetryGet");
  }

  public int getActiveRequestCount() {
    return networkThreadPool.getActiveThreadCount();
  }

  public boolean isAvailable() {
    return !networkThreadPool.isShutdown();
  }

  public void shutdown() {
    networkThreadPool.shutdown();
  }

  public static boolean isValidUrl(String url) {
    if (url == null || url.length() == 0) {
      return false;
    }

    return url.startsWith("http://") || url.startsWith("https://");
  }

  public static NetworkCallback createFireAndForgetCallback(
      final SuccessOnlyCallback successCallback) {
    return new NetworkCallback() {
      public void onSuccess(String result) {
        if (successCallback != null) {
          successCallback.onSuccess(result);
        }
      }

      public void onError(Exception error) {}

      public void onCancelled() {}
    };
  }

  public interface SuccessOnlyCallback {
    void onSuccess(String result);
  }
}
