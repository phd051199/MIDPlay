package app.utils;

import app.common.RestClient;
import app.constants.Constants;
import java.io.IOException;
import javax.microedition.lcdui.Image;

public class ImageUtils {

  public static Image getImage(String url, int size) {
    ImageCache cache = ImageCache.getInstance();
    return cache.getImage(url, size);
  }

  static Image getImageFromNetwork(String url, int size) throws IOException {
    try {
      String imageServiceUrl = buildImageServiceUrl(url, size);
      String encodedUrl = TextUtil.urlEncodeUTF8(imageServiceUrl);

      String finalUrl = StringUtils.concat(Constants.SERVICE_URL, "/proxy?url=", encodedUrl);

      byte[] imageData = RestClient.getInstance().getBytes(finalUrl);
      return Image.createImage(imageData, 0, imageData.length);
    } catch (Exception e) {
      throw new IOException("Failed to load image: " + e.getMessage());
    }
  }

  private static String buildImageServiceUrl(String originalUrl, int size) {
    String sizeStr = String.valueOf(size);
    return StringUtils.buildUrl(
        "https://wsrv.nl/",
        new String[] {
          "url", originalUrl,
          "output", "png",
          "w", sizeStr,
          "h", sizeStr,
          "fit", "cover"
        });
  }

  public static void preloadImage(String url, int size) {
    ImageCache cache = ImageCache.getInstance();
    cache.preloadImageAsync(url, size);
  }

  public static void clearImageCache() {
    ImageCache cache = ImageCache.getInstance();
    cache.clearCache();
  }

  public static ImageCache.CacheStats getCacheStats() {
    ImageCache cache = ImageCache.getInstance();
    return cache.getStats();
  }

  private ImageUtils() {}
}
