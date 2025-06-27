package app.utils;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.midlet.MIDlet;

public class I18N {
  private static final Hashtable resources = new Hashtable();
  private static String currentLanguage =
      System.getProperty("microedition.locale").startsWith("vi") ? "vi" : "en";
  private static MIDlet midlet;

  public static void initialize(MIDlet midlet) {
    I18N.midlet = midlet;
    loadLanguage(currentLanguage);
  }

  public static void setLanguage(String language) {
    if (!currentLanguage.equals(language)) {
      currentLanguage = language;
      loadLanguage(language);
    }
  }

  public static String getLanguage() {
    return currentLanguage;
  }

  public static void loadLanguage(String language) {
    try {
      resources.clear();
      String resourceName = "/app/i18n/MessagesBundle_" + language + ".properties";

      InputStream is = midlet.getClass().getResourceAsStream(resourceName);

      if (is == null && !"en".equals(language)) {
        resourceName = "/app/i18n/MessagesBundle_en.properties";
        is = midlet.getClass().getResourceAsStream(resourceName);
        currentLanguage = "en";
      }

      if (is != null) {
        try {

          byte[] data = new byte[is.available()];
          is.read(data);
          String content = new String(data, "UTF-8");

          String[] lines = split(content, '\n');
          for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("#") && line.length() > 0) {
              int equalIndex = line.indexOf('=');
              if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();
                resources.put(key, value);
              }
            }
          }
        } finally {
          is.close();
        }
      }
    } catch (Exception e) {
    }
  }

  public static String tr(String key) {
    String value = (String) resources.get(key);
    return value != null ? value : key;
  }

  public static String tr(String key, String defaultValue) {
    String value = (String) resources.get(key);
    return value != null ? value : defaultValue;
  }

  public static String[] getLanguages() {

    return new String[] {"en", "vi"};
  }

  private static String[] split(String str, char delimiter) {
    if (str == null) {
      return new String[0];
    }

    Vector result = new Vector();
    int start = 0;
    int end;
    while ((end = str.indexOf(delimiter, start)) != -1) {
      result.addElement(str.substring(start, end));
      start = end + 1;
    }
    result.addElement(str.substring(start));

    String[] arr = new String[result.size()];
    result.copyInto(arr);
    return arr;
  }

  public static String getLanguageName(String languageCode) {
    if ("vi".equals(languageCode)) {
      return tr("language_vi");
    }
    return tr("language_en");
  }

  private I18N() {}
}
