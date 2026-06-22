package midplay.net;

import midplay.util.Utils;

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

  protected void fetchText(String url) {
    network = new Network();
    try {
      onResponse(Utils.bytesToUtf8(network.sendHttpGetBytes(url)));
    } catch (NetworkError e) {
      onError(e);
    }
  }

  protected byte[] fetchBytes(String url) {
    network = new Network();
    try {
      return network.sendHttpGetBytes(url);
    } catch (NetworkError e) {
      onError(e);
      return null;
    }
  }

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
