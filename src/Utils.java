import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import model.MenuItem;
import model.Playlist;

public class Utils {
  public static final boolean isBlackberry;

  static {
    String platform = System.getProperty("microedition.platform");
    isBlackberry = (platform != null && platform.toLowerCase().startsWith("blackberry"));
  }

  private static String playerHttpMethod;

  private static final String HEX_DIGITS = "0123456789ABCDEF";

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

  public static void drawScaledImage(Graphics g, Image image, int x, int y, int width, int height) {
    if (image.getWidth() == width && image.getHeight() == height) {
      g.drawImage(image, x, y, Graphics.LEFT | Graphics.TOP);
    } else {
      g.drawImage(resizeImageToFit(image, width, height), x, y, Graphics.LEFT | Graphics.TOP);
    }
  }

  public static Image resizeImageToFit(Image src, int targetWidth, int targetHeight) {
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();

    if (srcWidth == targetWidth && srcHeight == targetHeight) {
      return src;
    }

    double scaleX = (double) targetWidth / srcWidth;
    double scaleY = (double) targetHeight / srcHeight;
    double scale = Math.max(scaleX, scaleY);

    int scaledWidth = (int) (srcWidth * scale);
    int scaledHeight = (int) (srcHeight * scale);

    Image scaledImage = resizeImageExact(src, scaledWidth, scaledHeight);
    Image resizedImage =
        cropImageToCenter(scaledImage, scaledWidth, scaledHeight, targetWidth, targetHeight);

    return resizedImage;
  }

  public static Image cropImageToCenter(
      Image scaledImage, int scaledWidth, int scaledHeight, int targetWidth, int targetHeight) {
    int[] dst = new int[targetWidth * targetHeight];
    int[] srcPixels = new int[scaledWidth * scaledHeight];
    scaledImage.getRGB(srcPixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight);

    int offsetX = (scaledWidth - targetWidth) / 2;
    int offsetY = (scaledHeight - targetHeight) / 2;

    for (int y = 0; y < targetHeight; y++) {
      for (int x = 0; x < targetWidth; x++) {
        int srcX = x + offsetX;
        int srcY = y + offsetY;
        if (srcX >= 0 && srcX < scaledWidth && srcY >= 0 && srcY < scaledHeight) {
          dst[y * targetWidth + x] = srcPixels[srcY * scaledWidth + srcX];
        }
      }
    }
    return Image.createRGBImage(dst, targetWidth, targetHeight, true);
  }

  public static Image resizeImageExact(Image src, int targetWidth, int targetHeight) {
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();

    int[] resizedPixels = new int[targetWidth * targetHeight];
    int[] srcRowBuffer = new int[srcWidth];

    int dstPixelIndex = 0;
    int srcYAccumulator = 0;

    for (int dstY = 0; dstY < targetHeight; dstY++) {
      int srcY = srcYAccumulator / targetHeight;
      int srcXAccumulator = 0;
      src.getRGB(srcRowBuffer, 0, srcWidth, 0, srcY, srcWidth, 1);

      for (int dstX = 0; dstX < targetWidth; dstX++) {
        int srcX = srcXAccumulator / targetWidth;
        resizedPixels[dstPixelIndex++] = srcRowBuffer[srcX];
        srcXAccumulator += srcWidth;
      }
      srcYAccumulator += srcHeight;
    }

    return Image.createRGBImage(resizedPixels, targetWidth, targetHeight, true);
  }

  public static String truncateText(String text, Font font, int maxWidth) {
    if (text == null || text.length() == 0) {
      return "";
    }
    if (font.stringWidth(text) <= maxWidth) {
      return text;
    }

    int ellipsisWidth = font.stringWidth("...");
    int availableWidth = maxWidth - ellipsisWidth;
    if (availableWidth <= 0) {
      return "...";
    }

    for (int i = text.length() - 1; i > 0; i--) {
      if (font.stringWidth(text.substring(0, i)) <= availableWidth) {
        return text.substring(0, i) + "...";
      }
    }
    return "...";
  }

  public static String formatTime(long microseconds) {
    long totalSeconds = microseconds / 1000000L;
    long minutes = totalSeconds / 60L;
    long seconds = totalSeconds % 60L;

    return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
  }

  private Utils() {}
}
