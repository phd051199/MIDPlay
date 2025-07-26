public abstract class NetworkOperation implements Network.NetworkListener {
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

  protected abstract void processResponse(String response);

  public void onDataReceived(String response) {
    if (this.stopped) {
      return;
    }
    processResponse(response);
  }

  public void onNoDataReceived() {
    if (this.stopped) {
      return;
    }
    handleNoData();
  }

  public void onError(Exception e) {
    if (this.stopped) {
      return;
    }
    handleError(e);
  }

  protected abstract void handleNoData();

  protected abstract void handleError(Exception e);

  public void onResponse(String response) {
    if (this.stopped) {
      return;
    }
    try {
      if (response != null && response.length() != 0) {
        onDataReceived(response);
      } else {
        onNoDataReceived();
      }
    } catch (Exception e) {
      onError(e);
    }
  }
}
