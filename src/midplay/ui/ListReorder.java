package midplay.ui;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

// Shared row-text primitives for the two "two-tap reorder with a marker prefix"
// modes (MainMenuScreen sort and QueueTrackListScreen sort). Only the marker /
// swap mechanics live here; each screen keeps its own two-tap state machine and
// save strategy, which differ too much to share cleanly.
public final class ListReorder {
  private ListReorder() {}

  public static String stripMarker(String text, String marker) {
    if (text != null && text.startsWith(marker)) {
      return text.substring(marker.length());
    }
    return text;
  }

  // Prepend/remove the marker on a row, preserving its icon. No-op if the row
  // is already in the requested state.
  public static void toggleMarker(List list, int index, String marker, boolean on) {
    String text = list.getString(index);
    boolean marked = text.startsWith(marker);
    if (on && !marked) {
      list.set(index, marker + text, list.getImage(index));
    } else if (!on && marked) {
      list.set(index, stripMarker(text, marker), list.getImage(index));
    }
  }

  // Swap two rows' text (stripped of marker) and icons. Both rows are captured
  // before either is written back.
  public static void swapRows(List list, int a, int b, String marker) {
    String textA = stripMarker(list.getString(a), marker);
    String textB = stripMarker(list.getString(b), marker);
    Image imgA = list.getImage(a);
    Image imgB = list.getImage(b);
    list.set(a, textB, imgB);
    list.set(b, textA, imgA);
  }
}
