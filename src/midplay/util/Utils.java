package midplay.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

public class Utils {
  public static final boolean isBlackberry;
  public static final boolean isSamsung;

  static {
    String platform = System.getProperty("microedition.platform");
    isBlackberry = (platform != null && platform.toLowerCase().startsWith("blackberry"));
    isSamsung =
        platform != null
            && (platform.toLowerCase().indexOf("samsung") != -1
                || platform.toLowerCase().indexOf("sgh") != -1);
  }

  private static final String HEX_DIGITS = "0123456789ABCDEF";

  private static boolean isUrlSafeCharacter(char c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '_'
        || c == '.'
        || c == '~';
  }

  public static boolean hasClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean hasProperty(String propertyName) {
    return System.getProperty(propertyName) != null;
  }

  public static boolean isJ2MELoader() {
    return hasClass("javax.microedition.shell.MicroActivity");
  }

  public static String replace(String text, String search, String replacement) {
    if (text == null || search == null) {
      return text;
    }

    StringBuffer sb = new StringBuffer();
    int pos = 0, found;
    int searchLen = search.length();

    while ((found = text.indexOf(search, pos)) != -1) {
      sb.append(text.substring(pos, found)).append(replacement);
      pos = found + searchLen;
    }
    sb.append(text.substring(pos));
    return sb.toString();
  }

  public static int indexOfIgnoreCase(String haystack, String needle) {
    if (haystack == null || needle == null) {
      return -1;
    }
    int hLen = haystack.length();
    int nLen = needle.length();
    if (nLen == 0) {
      return 0;
    }
    for (int i = 0; i <= hLen - nLen; i++) {
      int j = 0;
      for (; j < nLen; j++) {
        char h = haystack.charAt(i + j);
        char n = needle.charAt(j);
        if (h >= 'A' && h <= 'Z') {
          h += 32;
        }
        if (n >= 'A' && n <= 'Z') {
          n += 32;
        }
        if (h != n) {
          break;
        }
      }
      if (j == nLen) {
        return i;
      }
    }
    return -1;
  }

  public static boolean containsAnyIgnoreCase(String haystack, String[] needles) {
    if (haystack == null || needles == null) {
      return false;
    }
    for (int i = 0; i < needles.length; i++) {
      if (indexOfIgnoreCase(haystack, needles[i]) != -1) {
        return true;
      }
    }
    return false;
  }

  public static byte[] utf8ToBytes(String s) {
    if (s == null) {
      return new byte[0];
    }
    try {
      return s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return s.getBytes();
    }
  }

  public static String bytesToUtf8(byte[] data) {
    if (data == null || data.length == 0) {
      return "";
    }
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  public static String urlEncode(String text) {
    if (text == null) {
      return "";
    }

    byte[] bytes = utf8ToBytes(text);
    StringBuffer result = new StringBuffer(bytes.length + (bytes.length >> 1));

    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i] & 0xFF;
      char c = (char) b;

      if (c == ' ') {
        result.append('+');
      } else if (isUrlSafeCharacter(c)) {
        result.append(c);
      } else {
        result
            .append('%')
            .append(HEX_DIGITS.charAt((b >> 4) & 0xF))
            .append(HEX_DIGITS.charAt(b & 0xF));
      }
    }
    return result.toString();
  }

  public interface Comparator {
    boolean shouldSwap(Object a, Object b);
  }

  public static void bubbleSort(Vector vector, Comparator comparator) {
    int size = vector.size();
    if (size < 2) {
      return;
    }

    for (int i = 0; i < size - 1; i++) {
      boolean swapped = false;
      for (int j = 0; j < size - 1 - i; j++) {
        Object obj1 = vector.elementAt(j);
        Object obj2 = vector.elementAt(j + 1);

        if (comparator.shouldSwap(obj1, obj2)) {
          vector.setElementAt(obj2, j);
          vector.setElementAt(obj1, j + 1);
          swapped = true;
        }
      }
      if (!swapped) {
        break;
      }
    }
  }

  public static String toHexRgb(int color) {
    String hex = Integer.toHexString(color & 0x00FFFFFF).toUpperCase();
    int pad = 6 - hex.length();
    if (pad <= 0) {
      return hex;
    }
    StringBuffer sb = new StringBuffer(6);
    for (int i = 0; i < pad; i++) {
      sb.append('0');
    }
    sb.append(hex);
    return sb.toString();
  }

  public static Image applyColor(Image img, int targetColor) {
    int width = img.getWidth();
    int height = img.getHeight();
    int[] rgbData = new int[width * height];

    img.getRGB(rgbData, 0, width, 0, 0, width, height);

    for (int i = 0; i < rgbData.length; i++) {
      int pixel = rgbData[i];
      int alpha = pixel & 0xFF000000;

      if (alpha != 0) {
        rgbData[i] = alpha | (targetColor & 0x00FFFFFF);
      }
    }

    return Image.createRGBImage(rgbData, width, height, true);
  }

  public static Image createImageFromHex(int hex, int width, int height) {
    Image image = Image.createImage(width, height);
    Graphics g = image.getGraphics();

    g.setColor(hex);
    g.fillRect(0, 0, width, height);

    return image;
  }

  public static Image resizeImageToFit(Image src, int targetWidth, int targetHeight) {
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();

    if (srcWidth == targetWidth && srcHeight == targetHeight) {
      return src;
    }

    double scaleX = (double) targetWidth / srcWidth;
    double scaleY = (double) targetHeight / srcHeight;
    double scale = scaleX > scaleY ? scaleX : scaleY;

    int scaledWidth = (int) (srcWidth * scale);
    int scaledHeight = (int) (srcHeight * scale);
    int offsetX = (scaledWidth - targetWidth) / 2;
    int offsetY = (scaledHeight - targetHeight) / 2;

    int[] dst = new int[targetWidth * targetHeight];
    int[] srcRow = new int[srcWidth];
    int dstIndex = 0;

    for (int dy = 0; dy < targetHeight; dy++) {
      int srcY = ((offsetY + dy) * srcHeight) / scaledHeight;
      src.getRGB(srcRow, 0, srcWidth, 0, srcY, srcWidth, 1);
      for (int dx = 0; dx < targetWidth; dx++) {
        int srcX = ((offsetX + dx) * srcWidth) / scaledWidth;
        dst[dstIndex++] = srcRow[srcX];
      }
    }

    return Image.createRGBImage(dst, targetWidth, targetHeight, true);
  }

  public static String formatTime(long microseconds) {
    return formatClock(microseconds / 1000000L, false);
  }

  public static String formatClock(long totalSeconds, boolean withHours) {
    long hours = totalSeconds / 3600;
    long minutes = withHours ? (totalSeconds % 3600) / 60 : totalSeconds / 60;
    long seconds = totalSeconds % 60;

    StringBuffer sb = new StringBuffer(withHours ? 8 : 5);
    if (withHours) {
      appendPadded(sb, hours);
      sb.append(':');
    }
    appendPadded(sb, minutes);
    sb.append(':');
    appendPadded(sb, seconds);
    return sb.toString();
  }

  private static void appendPadded(StringBuffer sb, long value) {
    if (value < 10) {
      sb.append('0');
    }
    sb.append(value);
  }

  public static void clampAndSelect(List list, int index) {
    int size = list.size();
    if (size <= 0) {
      return;
    }
    if (index < 0) {
      index = 0;
    } else if (index >= size) {
      index = size - 1;
    }
    list.setSelectedIndex(index, true);
  }

  public static void closeQuietly(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
      }
    }
  }

  public static void closeQuietly(HttpConnection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (IOException e) {
      }
    }
  }

  public static void closePlayerQuietly(Player player, PlayerListener listener) {
    if (player == null) {
      return;
    }
    if (listener != null) {
      try {
        player.removePlayerListener(listener);
      } catch (Exception e) {
      }
    }
    try {
      int state = player.getState();
      if (state != Player.CLOSED && state >= Player.REALIZED) {
        player.stop();
      }
    } catch (Exception e) {
    }
    try {
      player.close();
    } catch (Exception e) {
    }
  }

  private Utils() {}
}
