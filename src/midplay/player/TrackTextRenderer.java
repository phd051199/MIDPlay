package midplay.player;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import midplay.net.BinaryImageLoadOperation;
import midplay.net.URLProvider;

public final class TrackTextRenderer {

  private final PlayerScreen screen;

  TextImageSlot trackNameTextImage = new TextImageSlot();
  TextImageSlot artistTextImage = new TextImageSlot();

  TrackTextRenderer(PlayerScreen screen) {
    this.screen = screen;
  }

  void resetTrackTextImages() {
    trackNameTextImage.reset();
    artistTextImage.reset();
  }

  void drawTrackTextImage(Graphics g, Image image, int x, int y, boolean isLargeScreen) {
    if (image == null) {
      return;
    }
    int anchor = isLargeScreen ? (Graphics.HCENTER | Graphics.TOP) : (Graphics.LEFT | Graphics.TOP);
    g.drawImage(image, x, y, anchor);
  }

  String truncateTextForWidth(TextImageSlot slot, String text, Font font, int maxWidth) {
    if (text == slot.truncSrc
        && font == slot.truncFont
        && maxWidth == slot.truncWidth
        && slot.truncResult != null) {
      return slot.truncResult;
    }
    String result = computeTruncatedText(text, font, maxWidth);
    slot.truncSrc = text;
    slot.truncFont = font;
    slot.truncWidth = maxWidth;
    slot.truncResult = result;
    return result;
  }

  private String computeTruncatedText(String text, Font font, int maxWidth) {
    if (text == null || text.length() == 0) {
      return "";
    }
    if (maxWidth <= 0 || font.stringWidth(text) <= maxWidth) {
      return text;
    }

    int ellipsisWidth = font.stringWidth("...");
    int availableWidth = maxWidth - ellipsisWidth;
    if (availableWidth <= 0) {
      return "...";
    }

    int lo = 1;
    int hi = text.length() - 1;
    int best = 0;
    while (lo <= hi) {
      int mid = (lo + hi) >> 1;
      if (font.stringWidth(text.substring(0, mid)) <= availableWidth) {
        best = mid;
        lo = mid + 1;
      } else {
        hi = mid - 1;
      }
    }
    if (best == 0) {
      return "...";
    }
    return text.substring(0, best) + "...";
  }

  boolean shouldRenderWithTakumi(TextImageSlot slot, String text) {
    if (text == slot.cjkText || (text != null && text.equals(slot.cjkText))) {
      return slot.cjkResult;
    }
    slot.cjkText = text;
    slot.cjkResult = containsCjkCharacter(text);
    return slot.cjkResult;
  }

  boolean containsCjkCharacter(String text) {
    if (text == null || text.length() == 0) {
      return false;
    }
    int length = text.length();
    for (int i = 0; i < length; i++) {
      char ch = text.charAt(i);
      if (isCjkCharacter(ch)) {
        return true;
      }
    }
    return false;
  }

  boolean isCjkCharacter(char ch) {
    return (ch >= 0x3400 && ch <= 0x4DBF)
        || (ch >= 0x4E00 && ch <= 0x9FFF)
        || (ch >= 0xF900 && ch <= 0xFAFF)
        || (ch >= 0x3040 && ch <= 0x309F)
        || (ch >= 0x30A0 && ch <= 0x30FF)
        || (ch >= 0x31F0 && ch <= 0x31FF)
        || (ch >= 0x1100 && ch <= 0x11FF)
        || (ch >= 0x3130 && ch <= 0x318F)
        || (ch >= 0xAC00 && ch <= 0xD7AF)
        || (ch >= 0x3000 && ch <= 0x303F)
        || (ch >= 0xFF66 && ch <= 0xFF9D);
  }

  Image resolveTrackTextImage(
      TextImageSlot slot,
      String text,
      int maxWidth,
      int fontSize,
      int color,
      boolean isLargeScreen) {
    if (slot == null || text == null || text.length() == 0 || maxWidth <= 0) {
      if (slot != null) {
        slot.clearImage();
      }
      return null;
    }

    int safeWidth = maxWidth > 0 ? maxWidth : 1;
    int safeFontSize = fontSize > 0 ? fontSize : 1;
    int colorRgb = color & 0x00FFFFFF;
    String align = isLargeScreen ? "center" : "";

    if (slot.keyText == null
        || !slot.keyText.equals(text)
        || slot.keyWidth != safeWidth
        || slot.keyFontSize != safeFontSize
        || slot.keyColor != colorRgb
        || !align.equals(slot.keyAlign)) {
      slot.keyText = text;
      slot.keyWidth = safeWidth;
      slot.keyFontSize = safeFontSize;
      slot.keyColor = colorRgb;
      slot.keyAlign = align;
      String requestKey = buildTrackTextImageKey(text, safeWidth, safeFontSize, colorRgb, align);
      if (!requestKey.equals(slot.requestKey)) {
        slot.clearImage();
      }
      slot.requestKey = requestKey;
    }

    if (slot.image != null) {
      return slot.image;
    }

    if (!slot.loading && !slot.failed) {
      startTrackTextImageLoad(slot, text, safeWidth, safeFontSize, color, align);
    }

    return null;
  }

  private String buildTrackTextImageKey(
      String text, int width, int fontSize, int colorRgb, String align) {
    return new StringBuffer(text.length() + 32)
        .append(text)
        .append('|')
        .append(width)
        .append('|')
        .append(fontSize)
        .append('|')
        .append(colorRgb)
        .append('|')
        .append(align)
        .toString();
  }

  private void startTrackTextImageLoad(
      final TextImageSlot slot, String text, int width, int fontSize, int color, String align) {
    String imageUrl = URLProvider.getTakumiTextImage(text, width, fontSize, color, align);
    if (imageUrl == null || imageUrl.length() == 0) {
      slot.failed = true;
      return;
    }

    slot.loading = true;
    slot.failed = false;
    final String expectedKey = slot.requestKey;
    slot.operation =
        new BinaryImageLoadOperation(
            imageUrl,
            new BinaryImageLoadOperation.Listener() {
              public void onImageLoaded(final Image image) {
                screen.navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (!isTrackTextRequestCurrent(slot, expectedKey)) {
                          return;
                        }
                        slot.operation = null;
                        slot.loading = false;
                        slot.image = image;
                        slot.failed = image == null;
                        if (slot.image != null) {
                          screen.updateDisplayAsync();
                        }
                      }
                    });
              }

              public void onImageLoadError(final Exception e) {
                screen.navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (!isTrackTextRequestCurrent(slot, expectedKey)) {
                          return;
                        }
                        slot.operation = null;
                        slot.loading = false;
                        slot.image = null;
                        slot.failed = true;
                      }
                    });
              }
            });
    slot.operation.start();
  }

  boolean isTrackTextRequestCurrent(TextImageSlot slot, String expectedKey) {
    return slot != null
        && expectedKey != null
        && slot.requestKey != null
        && expectedKey.equals(slot.requestKey);
  }

  class TextImageSlot {
    Image image;
    BinaryImageLoadOperation operation;
    String requestKey;
    boolean loading;
    boolean failed;

    String cjkText;
    boolean cjkResult;
    String truncSrc;
    Font truncFont;
    int truncWidth;
    String truncResult;
    String keyText;
    int keyWidth;
    int keyFontSize;
    int keyColor;
    String keyAlign;

    void reset() {
      stop();
      image = null;
      requestKey = null;
      failed = false;
      cjkText = null;
      truncSrc = null;
      truncResult = null;
      keyText = null;
      keyAlign = null;
    }

    void clearImage() {
      stop();
      image = null;
      requestKey = null;
      failed = false;
    }

    void stop() {
      if (operation != null) {
        operation.stop();
        operation = null;
      }
      loading = false;
    }
  }
}
