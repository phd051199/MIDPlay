package app.core.network;

public interface RestCallback {

  void error(Exception e);

  void success(String e);
}
