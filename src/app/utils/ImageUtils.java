package app.utils;

import app.constants.AppConstants;
import app.core.network.RestClient;
import java.io.IOException;
import javax.microedition.lcdui.Image;

public class ImageUtils {

  private static final int MAX_IMAGE_SIZE = 400;
  private static final int MAX_IMAGE_BYTES = 150 * 1024;
  private static final int MIN_IMAGE_SIZE = 16;

  public static Image getImage(String url, int size) throws IOException {
    if (url == null || url.trim().length() == 0) {
      throw new IllegalArgumentException("Image URL cannot be null or empty");
    }

    if (size < MIN_IMAGE_SIZE || size > MAX_IMAGE_SIZE) {
      throw new IllegalArgumentException(
          "Image size must be between " + MIN_IMAGE_SIZE + " and " + MAX_IMAGE_SIZE + " pixels");
    }

    try {
      String proxyUrl =
          AppConstants.SERVICE_URL
              + "/proxy?url="
              + TextUtils.urlEncodeUTF8(
                  "https://wsrv.nl/?url="
                      + url
                      + "&output=png&w="
                      + size
                      + "&h="
                      + size
                      + "&fit=cover");

      byte[] imageData = RestClient.getInstance().getBytes(proxyUrl);

      if (imageData == null || imageData.length == 0) {
        throw new IOException("No image data received");
      }

      if (imageData.length > MAX_IMAGE_BYTES) {
        throw new IOException(
            "Image too large: " + imageData.length + " bytes (max: " + MAX_IMAGE_BYTES + ")");
      }

      return Image.createImage(imageData, 0, imageData.length);
    } catch (OutOfMemoryError e) {
      throw new IOException("Out of memory loading image");
    } catch (Exception e) {
      throw new IOException("Failed to load image: " + e.getMessage());
    }
  }

  private ImageUtils() {}
}
