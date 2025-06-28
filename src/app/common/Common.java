package app.common;

import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import java.util.Vector;

public class Common {

  public static String replace(String _text, String _searchStr, String _replacementStr) {
    StringBuffer sb = new StringBuffer();
    int searchStringPos = _text.indexOf(_searchStr);
    int startPos = 0;

    for (int searchStringLength = _searchStr.length();
        searchStringPos != -1;
        searchStringPos = _text.indexOf(_searchStr, startPos)) {
      sb.append(_text.substring(startPos, searchStringPos)).append(_replacementStr);
      startPos = searchStringPos + searchStringLength;
    }

    sb.append(_text.substring(startPos, _text.length()));
    return sb.toString();
  }

  public static Object[] vectorToArray(Vector v) {
    Object[] result = new Object[v.size()];

    for (int i = 0; i < result.length; ++i) {
      result[i] = v.elementAt(i);
    }

    return result;
  }

  public static void loadDataAsync(
      final DataLoader loader, final LoadDataListener loadDataListener, Thread mLoadDataThread) {
    mLoadDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  Vector items = loader.load();
                  if (items == null) {
                    loadDataListener.loadError();
                  } else if (items.isEmpty()) {
                    loadDataListener.noData();
                  } else {
                    loadDataListener.loadDataCompleted(items);
                  }
                } catch (Exception e) {
                  loadDataListener.loadError();
                }
              }
            });
    mLoadDataThread.start();
  }

  private Common() {}
}
