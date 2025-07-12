package app.utils.image;

import app.utils.concurrent.ThreadManager;
import java.util.Vector;
import javax.microedition.lcdui.Image;

public class ImageLoader {
  private static ImageLoader instance;

  private static final int MAX_CONCURRENT_THREADS = 3;
  private static final int LOAD_DELAY_MS = 50;

  public static synchronized ImageLoader getInstance() {
    if (instance == null) {
      instance = new ImageLoader();
    }
    return instance;
  }

  private final ThreadManager.SimpleThreadPool threadPool;
  private final Vector activeRequests;
  private final Object requestLock;
  private volatile boolean isShutdown;

  private ImageLoader() {
    this.threadPool = new ThreadManager.SimpleThreadPool(MAX_CONCURRENT_THREADS);
    this.activeRequests = new Vector();
    this.requestLock = new Object();
    this.isShutdown = false;
  }

  public void loadImage(final ImageLoadRequest request) {
    if (request == null || isShutdown || request.isCancelled()) {
      return;
    }

    if (request.getCallback() != null && !request.getCallback().shouldContinueLoading()) {
      return;
    }

    synchronized (requestLock) {
      activeRequests.addElement(request);
    }

    boolean submitted =
        threadPool.execute(
            new Runnable() {
              public void run() {
                processImageLoad(request);
              }
            },
            "ImageLoader-" + request.getIndex());

    if (!submitted) {
      synchronized (requestLock) {
        activeRequests.removeElement(request);
      }

      if (request.getCallback() != null) {
        ThreadManager.runOnUiThread(
            new Runnable() {
              public void run() {
                request
                    .getCallback()
                    .onImageLoadFailed(
                        request.getIndex(),
                        request.getUrl(),
                        "Thread pool full",
                        request.getRequestId());
              }
            });
      }
    }
  }

  private void processImageLoad(final ImageLoadRequest request) {
    Image image = null;
    String errorMessage = null;
    try {
      if (request.isCancelled() || isShutdown) {
        return;
      }
      if (request.getCallback() != null && !request.getCallback().shouldContinueLoading()) {
        return;
      }
      image = ImageUtils.getImage(request.getUrl(), request.getSize());
      if (request.isCancelled() || isShutdown) {
        image = null;
        return;
      }
      if (request.getCallback() != null && !request.getCallback().shouldContinueLoading()) {
        image = null;
      }
    } catch (OutOfMemoryError e) {
      errorMessage = "Out of memory";
      System.gc();
    } catch (SecurityException e) {
      errorMessage = "Security error: " + e.getMessage();
    } catch (Exception e) {
      errorMessage = "Load error: " + e.getMessage();
    } finally {
      synchronized (requestLock) {
        activeRequests.removeElement(request);
      }

      if (!request.isCancelled() && !isShutdown && request.getCallback() != null) {
        final Image finalImage = image;
        final String finalError = errorMessage;

        ThreadManager.runOnUiThread(
            new Runnable() {
              public void run() {
                try {
                  if (request.getCallback().shouldContinueLoading()) {
                    if (finalImage != null) {
                      request
                          .getCallback()
                          .onImageLoaded(request.getIndex(), finalImage, request.getRequestId());
                    } else {
                      request
                          .getCallback()
                          .onImageLoadFailed(
                              request.getIndex(),
                              request.getUrl(),
                              finalError != null ? finalError : "Failed to load image",
                              request.getRequestId());
                    }
                  }
                } catch (Exception uiException) {
                }
              }
            });
      }

      try {
        Thread.sleep(LOAD_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void cancelAllRequests() {
    synchronized (requestLock) {
      for (int i = 0; i < activeRequests.size(); i++) {
        ImageLoadRequest request = (ImageLoadRequest) activeRequests.elementAt(i);
        request.cancel();
      }
    }
  }

  public void cancelRequestsForCallback(ImageLoadCallback callback) {
    if (callback == null) {
      return;
    }

    synchronized (requestLock) {
      for (int i = 0; i < activeRequests.size(); i++) {
        ImageLoadRequest request = (ImageLoadRequest) activeRequests.elementAt(i);
        if (request.getCallback() == callback) {
          request.cancel();
        }
      }
    }
  }

  public int getActiveRequestCount() {
    synchronized (requestLock) {
      return activeRequests.size();
    }
  }

  public int getActiveThreadCount() {
    return threadPool.getActiveThreadCount();
  }

  public void shutdown() {
    isShutdown = true;
    cancelAllRequests();
    threadPool.shutdown();
  }

  public boolean isShutdown() {
    return isShutdown || threadPool.isShutdown();
  }

  public void clearImageCache() {
    ImageUtils.clearImageCache();
  }

  public void forceGarbageCollection() {
    System.gc();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public LoaderStats getStats() {
    return new LoaderStats(
        getActiveRequestCount(), getActiveThreadCount(), ImageUtils.getCacheStats());
  }

  public static class LoaderStats {
    public final int activeRequests;
    public final int activeThreads;
    public final ImageCache.CacheStats cacheStats;

    public LoaderStats(int activeRequests, int activeThreads, ImageCache.CacheStats cacheStats) {
      this.activeRequests = activeRequests;
      this.activeThreads = activeThreads;
      this.cacheStats = cacheStats;
    }
  }
}
