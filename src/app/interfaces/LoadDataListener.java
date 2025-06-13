package app.interfaces;

import java.util.Vector;

public interface LoadDataListener {

  void loadDataCompleted(Vector var1);

  void loadError();

  void noData();
}
