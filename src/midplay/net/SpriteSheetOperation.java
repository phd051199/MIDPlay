package midplay.net;

import javax.microedition.lcdui.Image;

public class SpriteSheetOperation extends NetworkOperation {

  private final String[] urls;
  private final int cellW;
  private final int cellH;
  private final int cols;
  private final Listener listener;

  public SpriteSheetOperation(String[] urls, int cellW, int cellH, int cols, Listener listener) {
    this.urls = urls;
    this.cellW = cellW;
    this.cellH = cellH;
    this.cols = cols;
    this.listener = listener;
  }

  protected void execute() {
    String body = buildBody(urls, cellW, cellH, cols);
    byte[] data = fetchPostBytes(URLProvider.getSpriteSheetUrl(), body);
    if (data == null) {
      return; // handleError already routed to listener.onError
    }
    try {
      Image sheet = Image.createImage(data, 0, data.length);
      listener.onSheet(slice(sheet));
    } catch (Exception e) {
      listener.onError(e);
    }
  }

  private Image[] slice(Image sheet) {
    Image[] out = new Image[urls.length];
    for (int i = 0; i < urls.length; i++) {
      int x = (i % cols) * cellW;
      int y = (i / cols) * cellH;
      try {
        out[i] = Image.createImage(sheet, x, y, cellW, cellH, 0); // 0 == Sprite.TRANS_NONE
      } catch (Exception e) {
        out[i] = null;
      }
    }
    return out;
  }

  protected void handleError(Exception e) {
    listener.onError(e);
  }

  private static String buildBody(String[] urls, int w, int h, int cols) {
    StringBuffer sb = new StringBuffer(urls.length * 80 + 40);
    sb.append("{\"urls\":[");
    for (int i = 0; i < urls.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append('"').append(escape(urls[i])).append('"');
    }
    sb.append("],\"width\":")
        .append(w)
        .append(",\"height\":")
        .append(h)
        .append(",\"cols\":")
        .append(cols)
        .append('}');
    return sb.toString();
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    if (s.indexOf('"') < 0 && s.indexOf('\\') < 0) {
      return s;
    }
    StringBuffer sb = new StringBuffer(s.length() + 4);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"' || c == '\\') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  public interface Listener {
    void onSheet(Image[] cells);

    void onError(Exception e);
  }
}
