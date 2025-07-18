package app.utils;

import java.io.InputStream;
import java.util.Hashtable;
import javax.microedition.midlet.MIDlet;

public class I18N {
  private static final Hashtable resources = new Hashtable();
  private static String currentLanguage;
  private static MIDlet midlet;
  private static final Object resourcesLock = new Object();

  public static void initialize(MIDlet midlet) {
    I18N.midlet = midlet;
    setDefaultLanguage();
    loadLanguage(currentLanguage);
  }

  public static void setDefaultLanguage() {
    String locale = System.getProperty("microedition.locale");
    if (locale.startsWith("vi")) {
      currentLanguage = "vi";
    } else if (locale.startsWith("tr")) {
      currentLanguage = "tr";
    } else {
      currentLanguage = "en";
    }
  }

  public static void setLanguage(String language) {
    synchronized (resourcesLock) {
      if (!currentLanguage.equals(language)) {
        currentLanguage = language;
        loadLanguage(language);
      }
    }
  }

  public static String getLanguage() {
    return currentLanguage;
  }

  public static void loadLanguage(String language) {
    synchronized (resourcesLock) {
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

            String[] lines = TextUtils.split(content, '\n');
            for (int i = 0; i < lines.length; i++) {
              String line = lines[i].trim();
              if (!line.startsWith("#") && line.length() > 0) {
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                  String key = line.substring(0, equalIndex).trim();
                  String value = line.substring(equalIndex + 1).trim();

                  if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                  }

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
  }

  public static String tr(String key) {
    synchronized (resourcesLock) {
      String value = (String) resources.get(key);
      return value != null ? value : key;
    }
  }

  public static String tr(String key, String defaultValue) {
    synchronized (resourcesLock) {
      String value = (String) resources.get(key);
      return value != null ? value : defaultValue;
    }
  }

  public static String[] getLanguages() {
    return new String[] {"en", "vi", "tr"};
  }

  public static String getLanguageName(String languageCode) {
    if ("vi".equals(languageCode)) {
      return tr("language_vi");
    }
    if ("tr".equals(languageCode)) {
      return tr("language_tr");
    }
    return tr("language_en");
  }

  private I18N() {}
}
