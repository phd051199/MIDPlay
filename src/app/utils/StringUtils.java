package app.utils;

public class StringUtils {

  private static final int BUFFER_POOL_SIZE = 5;
  private static final StringBuffer[] bufferPool = new StringBuffer[BUFFER_POOL_SIZE];
  private static final boolean[] bufferInUse = new boolean[BUFFER_POOL_SIZE];

  public static final String EMPTY = "";
  public static final String SPACE = " ";
  public static final String COMMA = ",";
  public static final String DOT = ".";
  public static final String SLASH = "/";
  public static final String COLON = ":";
  public static final String SEMICOLON = ";";
  public static final String EQUALS = "=";
  public static final String AMPERSAND = "&";
  public static final String QUESTION_MARK = "?";

  static {
    for (int i = 0; i < BUFFER_POOL_SIZE; i++) {
      bufferPool[i] = new StringBuffer(256);
      bufferInUse[i] = false;
    }
  }

  public static synchronized StringBuffer getStringBuffer(int initialCapacity) {
    for (int i = 0; i < BUFFER_POOL_SIZE; i++) {
      if (!bufferInUse[i]) {
        bufferInUse[i] = true;
        StringBuffer sb = bufferPool[i];
        sb.setLength(0);
        if (sb.capacity() < initialCapacity) {
          sb.ensureCapacity(initialCapacity);
        }
        return sb;
      }
    }

    return new StringBuffer(initialCapacity);
  }

  public static StringBuffer getStringBuffer() {
    return getStringBuffer(64);
  }

  public static synchronized void returnStringBuffer(StringBuffer sb) {
    if (sb == null) {
      return;
    }

    for (int i = 0; i < BUFFER_POOL_SIZE; i++) {
      if (bufferPool[i] == sb && bufferInUse[i]) {
        bufferInUse[i] = false;
        sb.setLength(0);
        return;
      }
    }
  }

  public static String concat(String[] strings) {
    if (strings == null || strings.length == 0) {
      return EMPTY;
    }

    if (strings.length == 1) {
      return strings[0] != null ? strings[0] : EMPTY;
    }

    int totalLength = 0;
    for (int i = 0; i < strings.length; i++) {
      if (strings[i] != null) {
        totalLength += strings[i].length();
      }
    }

    StringBuffer sb = getStringBuffer(totalLength + 10);
    try {
      for (int i = 0; i < strings.length; i++) {
        if (strings[i] != null) {
          sb.append(strings[i]);
        }
      }
      return sb.toString();
    } finally {
      returnStringBuffer(sb);
    }
  }

  public static String concat(String s1, String s2) {
    if (s1 == null && s2 == null) return EMPTY;
    if (s1 == null) return s2;
    if (s2 == null) return s1;

    StringBuffer sb = getStringBuffer(s1.length() + s2.length() + 5);
    try {
      return sb.append(s1).append(s2).toString();
    } finally {
      returnStringBuffer(sb);
    }
  }

  public static String concat(String s1, String s2, String s3) {
    int totalLength = 0;
    if (s1 != null) totalLength += s1.length();
    if (s2 != null) totalLength += s2.length();
    if (s3 != null) totalLength += s3.length();

    if (totalLength == 0) return EMPTY;

    StringBuffer sb = getStringBuffer(totalLength + 5);
    try {
      if (s1 != null) sb.append(s1);
      if (s2 != null) sb.append(s2);
      if (s3 != null) sb.append(s3);
      return sb.toString();
    } finally {
      returnStringBuffer(sb);
    }
  }

  public static String buildUrl(String baseUrl, String[] params) {
    if (baseUrl == null) {
      return EMPTY;
    }

    if (params == null || params.length == 0) {
      return baseUrl;
    }

    int capacity = baseUrl.length() + 50;
    for (int i = 0; i < params.length; i++) {
      if (params[i] != null) {
        capacity += params[i].length() + 5;
      }
    }

    StringBuffer sb = getStringBuffer(capacity);
    try {
      sb.append(baseUrl);

      boolean firstParam = true;
      for (int i = 0; i < params.length - 1; i += 2) {
        String key = params[i];
        String value = (i + 1 < params.length) ? params[i + 1] : null;

        if (key != null && value != null) {
          sb.append(firstParam ? QUESTION_MARK : AMPERSAND);
          sb.append(key).append(EQUALS).append(value);
          firstParam = false;
        }
      }

      return sb.toString();
    } finally {
      returnStringBuffer(sb);
    }
  }

  public static String join(String[] strings, String delimiter) {
    if (strings == null || strings.length == 0) {
      return EMPTY;
    }

    if (strings.length == 1) {
      return strings[0] != null ? strings[0] : EMPTY;
    }

    int capacity = 0;
    int delimiterLength = delimiter != null ? delimiter.length() : 0;

    for (int i = 0; i < strings.length; i++) {
      if (strings[i] != null) {
        capacity += strings[i].length();
      }
      if (i > 0) {
        capacity += delimiterLength;
      }
    }

    StringBuffer sb = getStringBuffer(capacity + 10);
    try {
      boolean first = true;
      for (int i = 0; i < strings.length; i++) {
        if (strings[i] != null) {
          if (!first && delimiter != null) {
            sb.append(delimiter);
          }
          sb.append(strings[i]);
          first = false;
        }
      }
      return sb.toString();
    } finally {
      returnStringBuffer(sb);
    }
  }

  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  public static boolean isNotEmpty(String str) {
    return str != null && str.length() > 0;
  }

  public static boolean equals(String s1, String s2) {
    if (s1 == s2) return true;
    if (s1 == null || s2 == null) return false;
    return s1.equals(s2);
  }

  private StringUtils() {}
}
