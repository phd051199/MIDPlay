import java.io.UnsupportedEncodingException;
import java.util.Vector;
import javax.microedition.lcdui.Image;
import model.MenuItem;
import model.Playlist;

public class Utils {

  private static String playerHttpMethod;

  // reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  public static void setPlayerHttpMethod() {
    String platform = System.getProperty("microedition.platform");

    // Check Symbian variants
    boolean symbianJrt = platform != null && platform.indexOf("platform=S60") != -1;
    boolean symbian =
        symbianJrt
            || hasProperty("com.symbian.midp.serversocket.support")
            || hasProperty("com.symbian.default.to.suite.icon")
            || hasClass("com.symbian.midp.io.protocol.http.Protocol")
            || hasClass("com.symbian.lcdjava.io.File");

    String method = Configuration.PLAYER_METHOD_PASS_URL; // default

    // Nokia S40 detection
    if (hasClass("com.nokia.mid.impl.isa.jam.Jam")) {
      // S40v1 uses sun impl for media and i/o so it should work fine with URL
      // S40v2+ breaks http locator parsing so needs InputStream
      method =
          hasClass("com.sun.mmedia.protocol.CommonDS")
              ? Configuration.PLAYER_METHOD_PASS_URL
              : Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    }
    // Symbian-specific logic
    else if (symbian) {
      if (symbianJrt
          && platform != null
          && (platform.indexOf("java_build_version=2.") != -1
              || platform.indexOf("java_build_version=1.4") != -1)) {
        // EMC (S60v5+) supports mp3 streaming - keep default URL method
      } else if (hasClass("com.symbian.mmapi.PlayerImpl")) {
        // UIQ - use InputStream
        method = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
      } else {
        // MMF (S60v3.2-) - use InputStream
        method = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
      }
    }
    // J2ME Loader
    else if (isJ2MELoader()) {
      method = Configuration.PLAYER_METHOD_PASS_INPUTSTREAM;
    }

    playerHttpMethod = method;
  }

  private static boolean isUrlSafeCharacter(char c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '_'
        || c == '.'
        || c == '~';
  }

  private static boolean hasClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean hasProperty(String propertyName) {
    return System.getProperty(propertyName) != null;
  }

  public static String getDefaultPlayerHttpMethod() {
    setPlayerHttpMethod();
    return playerHttpMethod;
  }

  public static String getPlayerHttpMethod() {
    return playerHttpMethod;
  }

  public static boolean isJ2MELoader() {
    return hasClass("javax.microedition.shell.MicroActivity");
  }

  public static String replace(String text, String searchString, String replacement) {
    StringBuffer sb = new StringBuffer();
    int pos = 0;
    int found;
    int searchLength = searchString.length();
    while ((found = text.indexOf(searchString, pos)) != -1) {
      sb.append(text.substring(pos, found)).append(replacement);
      pos = found + searchLength;
    }
    sb.append(text.substring(pos));
    return sb.toString();
  }

  private static final String HEX_DIGITS = "0123456789ABCDEF";

  public static String urlEncode(String text) {
    if (text == null) {
      return "";
    }
    try {
      byte[] bytes = text.getBytes("UTF-8");
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
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }

  public static void sort(Vector vector, int sortType) {
    int size = vector.size();
    if (size < 2) {
      return;
    }

    for (int i = 0; i < size - 1; i++) {
      boolean swapped = false;
      for (int j = 0; j < size - 1 - i; j++) {
        Object obj1 = vector.elementAt(j);
        Object obj2 = vector.elementAt(j + 1);

        boolean shouldSwap =
            (sortType == 1)
                ? ((Playlist) obj1).getId() < ((Playlist) obj2).getId()
                : (sortType == 2) && ((MenuItem) obj1).order > ((MenuItem) obj2).order;

        if (shouldSwap) {
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

  public static Image invertImageColors(Image img) {
    int width = img.getWidth();
    int height = img.getHeight();
    int[] rgbData = new int[width * height];

    img.getRGB(rgbData, 0, width, 0, 0, width, height);

    for (int i = 0; i < rgbData.length; i++) {
      int pixel = rgbData[i];
      rgbData[i] = invertColor(pixel);
    }

    return Image.createRGBImage(rgbData, width, height, true);
  }

  public static int invertColor(int color) {
    return (color & 0xFF000000) | (0x00FFFFFF ^ (color & 0x00FFFFFF));
  }

  private Utils() {}
}
