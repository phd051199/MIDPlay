package midplay.ui;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public final class ListReorder {
  private ListReorder() {}

  public static String stripMarker(String text, String marker) {
    if (text != null && text.startsWith(marker)) {
      return text.substring(marker.length());
    }
    return text;
  }

  public static void toggleMarker(List list, int index, String marker, boolean on) {
    String text = list.getString(index);
    boolean marked = text.startsWith(marker);
    if (on && !marked) {
      list.set(index, marker + text, list.getImage(index));
    } else if (!on && marked) {
      list.set(index, stripMarker(text, marker), list.getImage(index));
    }
  }

  public static void swapRows(List list, int a, int b, String marker) {
    String textA = stripMarker(list.getString(a), marker);
    String textB = stripMarker(list.getString(b), marker);
    Image imgA = list.getImage(a);
    Image imgB = list.getImage(b);
    list.set(a, textB, imgB);
    list.set(b, textA, imgA);
  }
}
