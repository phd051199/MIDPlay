package midplay.net;

public abstract class NetworkOperation {
  protected Thread thread;
  protected Network network;
  protected boolean stopped = false;

  public void start() {
    this.stopped = false;
    this.thread =
        new Thread() {
          public void run() {
            execute();
          }
        };
    this.thread.start();
  }

  public void stop() {
    this.stopped = true;
    if (this.network != null) {
      this.network.cancel();
    }
    if (this.thread != null) {
      this.thread.interrupt();
    }
  }

  protected boolean isStopped() {
    return this.stopped;
  }

  protected abstract void execute();

  // Fetches a text response on this operation's worker thread and routes it
  // through the standard onResponse/onError flow. Text operations call this
  // from execute(); binary operations use fetchBytes.
  protected void fetchText(String url) {
    network = new Network();
    try {
      onResponse(network.sendHttpGet(url));
    } catch (NetworkError e) {
      onError(e);
    }
  }

  // Fetches a binary response on this operation's worker thread, routing network
  // failures through onError and returning null. Binary operations (e.g. image
  // loads) call this from execute() instead of re-implementing the
  // Network + try/catch that fetchText already centralizes for the text path.
  protected byte[] fetchBytes(String url) {
    network = new Network();
    try {
      return network.sendHttpGetBytes(url);
    } catch (NetworkError e) {
      onError(e);
      return null;
    }
  }

  // No-op defaults so binary operations (which never receive text) need not
  // stub these out. Text operations override them.
  protected void processResponse(String response) {}

  protected void handleNoData() {}

  protected abstract void handleError(Exception e);

  public void onError(Exception e) {
    if (isStopped()) {
      return;
    }
    handleError(e);
  }

  public void onResponse(String response) {
    if (isStopped()) {
      return;
    }
    try {
      if (response != null && response.length() != 0) {
        processResponse(response);
      } else {
        handleNoData();
      }
    } catch (Exception e) {
      onError(e);
    }
  }
}
