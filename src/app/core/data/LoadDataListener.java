package app.core.data;

import java.util.Vector;

public interface LoadDataListener {

  void loadDataCompleted(Vector data);

  void loadError();

  void noData();
}
