package app.utils.image;

import app.core.threading.ThreadManagerIntegration;
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

  private final Vector activeRequests;
  private final Object requestLock;
  private volatile boolean isShutdown;

  private ImageLoader() {
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

    ThreadManagerIntegration.executeImageLoading(
        request.getUrl(),
        request.getSize(),
        new ThreadManagerIntegration.ImageLoadCallback() {
          public void onImageLoaded(Object image) {
            synchronized (requestLock) {
              activeRequests.removeElement(request);
            }

            if (!request.isCancelled() && !isShutdown && request.getCallback() != null) {
              final Image finalImage = (Image) image;

              ThreadManagerIntegration.executeUITask(
                  new Runnable() {
                    public void run() {
                      try {
                        if (request.getCallback().shouldContinueLoading()) {
                          if (finalImage != null) {
                            request
                                .getCallback()
                                .onImageLoaded(
                                    request.getIndex(), finalImage, request.getRequestId());
                          } else {
                            request
                                .getCallback()
                                .onImageLoadFailed(
                                    request.getIndex(),
                                    request.getUrl(),
                                    "Failed to load image",
                                    request.getRequestId());
                          }
                        }
                      } catch (Exception uiException) {
                      }
                    }
                  },
                  "ImageLoader-UI-" + request.getIndex());
            }

            try {
              Thread.sleep(LOAD_DELAY_MS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }

          public void onImageLoadError(Exception e) {
            synchronized (requestLock) {
              activeRequests.removeElement(request);
            }

            if (!request.isCancelled() && !isShutdown && request.getCallback() != null) {
              final String errorMessage = e != null ? e.getMessage() : "Unknown error";

              ThreadManagerIntegration.executeUITask(
                  new Runnable() {
                    public void run() {
                      try {
                        if (request.getCallback().shouldContinueLoading()) {
                          request
                              .getCallback()
                              .onImageLoadFailed(
                                  request.getIndex(),
                                  request.getUrl(),
                                  errorMessage,
                                  request.getRequestId());
                        }
                      } catch (Exception uiException) {
                      }
                    }
                  },
                  "ImageLoader-Error-" + request.getIndex());
            }
          }
        });
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

  public void shutdown() {
    isShutdown = true;
    cancelAllRequests();
  }

  public boolean isShutdown() {
    return isShutdown;
  }

  public void forceGarbageCollection() {
    System.gc();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
