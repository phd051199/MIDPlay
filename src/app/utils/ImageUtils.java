package app.utils;

import app.common.RestClient;
import app.constants.Constants;
import java.io.IOException;
import javax.microedition.lcdui.Image;

public class ImageUtils {

  public static Image getImage(String url, int size) throws IOException {
    try {
      url =
          Constants.SERVICE_URL
              + "/proxy?url="
              + TextUtil.urlEncodeUTF8(
                  "https://wsrv.nl/?url=" + url + "&output=png&w=" + size + "&h=" + size);
      byte[] b = RestClient.getInstance().getBytes(url);

      return Image.createImage(b, 0, b.length);
    } catch (Exception e) {
    }

    return null;
  }
}
