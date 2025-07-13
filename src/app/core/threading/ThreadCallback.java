package app.core.threading;

public interface ThreadCallback {

  void onSuccess();

  void onError(Exception e);
}
