package midplay.net;

public final class MediaResolutionResult {
  public final String resolvedUrl;
  public final boolean processing;
  public final int progress;
  public final String status;
  public final int responseCode;
  public final boolean transientError;

  public MediaResolutionResult(
      String resolvedUrl, boolean processing, int progress, String status, int responseCode) {
    this(resolvedUrl, processing, progress, status, responseCode, false);
  }

  private MediaResolutionResult(
      String resolvedUrl,
      boolean processing,
      int progress,
      String status,
      int responseCode,
      boolean transientError) {
    this.resolvedUrl = resolvedUrl;
    this.processing = processing;
    this.progress = progress;
    this.status = status;
    this.responseCode = responseCode;
    this.transientError = transientError;
  }

  public static MediaResolutionResult transientError(String url, int responseCode) {
    return new MediaResolutionResult(url, false, -1, null, responseCode, true);
  }
}
