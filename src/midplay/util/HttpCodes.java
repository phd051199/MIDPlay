package midplay.util;

import javax.microedition.io.HttpConnection;

public final class HttpCodes {
  public static final int PROCESSING = 202;

  public static boolean isRedirect(int code) {
    return code == HttpConnection.HTTP_MOVED_PERM || code == HttpConnection.HTTP_MOVED_TEMP;
  }

  public static boolean isProcessing(int code) {
    return code == PROCESSING;
  }

  public static boolean isNotFound(int code) {
    return code == HttpConnection.HTTP_NOT_FOUND;
  }

  // 5xx
  public static boolean isServerError(int code) {
    return code >= HttpConnection.HTTP_INTERNAL_ERROR;
  }

  // 4xx or 5xx
  public static boolean isError(int code) {
    return code >= HttpConnection.HTTP_BAD_REQUEST;
  }

  private HttpCodes() {}
}
