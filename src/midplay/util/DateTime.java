package midplay.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Small date/time helpers for the Tasks feature.
 *
 * <p>Only the CLDC 1.1 {@link Calendar} subset is used (get/set of single fields plus
 * getTime/setTime); {@code Calendar.add}, {@code getTimeInMillis} and {@code clear} are not
 * available on the target profile.
 */
public final class DateTime {
  /** Value used to mean "no date/time set". */
  public static final long NONE = -1L;

  /** Default wall-clock time used by the quick due-date presets. */
  public static final int PRESET_HOUR = 23;
  public static final int PRESET_MINUTE = 59;

  private DateTime() {}

  public static long now() {
    return System.currentTimeMillis();
  }

  /** Today (shifted by {@code daysFromToday}) at the given wall-clock time. */
  public static long preset(int daysFromToday, int hour, int minute) {
    Calendar c = Calendar.getInstance();
    int day = c.get(Calendar.DAY_OF_MONTH);
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, minute);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    if (daysFromToday != 0) {
      c.set(Calendar.DAY_OF_MONTH, day + daysFromToday);
    }
    return c.getTime().getTime();
  }

  /** "dd/MM/yyyy HH:mm". */
  public static String format(long ms) {
    return formatDate(ms) + " " + formatTime(ms);
  }

  /** "dd/MM/yyyy". */
  public static String formatDate(long ms) {
    Calendar c = calendar(ms);
    return pad(c.get(Calendar.DAY_OF_MONTH), 2)
        + "/"
        + pad(c.get(Calendar.MONTH) + 1, 2)
        + "/"
        + c.get(Calendar.YEAR);
  }

  /** "HH:mm". */
  public static String formatTime(long ms) {
    Calendar c = calendar(ms);
    return pad(c.get(Calendar.HOUR_OF_DAY), 2) + ":" + pad(c.get(Calendar.MINUTE), 2);
  }

  /** Compact form for list rows: "dd/MM HH:mm" (adds a two digit year when it differs). */
  public static String formatShort(long ms) {
    Calendar now = Calendar.getInstance();
    Calendar c = calendar(ms);
    StringBuffer b = new StringBuffer();
    b.append(pad(c.get(Calendar.DAY_OF_MONTH), 2)).append('/').append(pad(c.get(Calendar.MONTH) + 1, 2));
    if (c.get(Calendar.YEAR) != now.get(Calendar.YEAR)) {
      b.append('/').append(pad(c.get(Calendar.YEAR) % 100, 2));
    }
    b.append(' ').append(formatTime(ms));
    return b.toString();
  }

  /** "Hoje 18:30", "Amanhã 08:00" or a compact date. */
  public static String formatRelative(long ms) {
    if (ms == NONE) {
      return "";
    }
    Calendar c = calendar(ms);
    if (sameDay(Calendar.getInstance(), c)) {
      return Lang.tr("task.today") + " " + formatTime(ms);
    }
    if (sameDay(calendar(preset(1, 0, 0)), c)) {
      return Lang.tr("task.tomorrow") + " " + formatTime(ms);
    }
    return formatShort(ms);
  }

  /**
   * Parses "dd/MM/yyyy HH:mm", "dd/MM/yyyy", "dd/MM HH:mm", "dd/MM" or "HH:mm".
   *
   * @return the timestamp in milliseconds, or {@link #NONE} when the text is empty or invalid.
   */
  public static long parse(String input) {
    if (input == null) {
      return NONE;
    }
    String text = input.trim();
    if (text.length() == 0) {
      return NONE;
    }
    String datePart = null;
    String timePart = null;
    int space = text.indexOf(' ');
    if (space >= 0) {
      datePart = text.substring(0, space).trim();
      timePart = text.substring(space + 1).trim();
    } else if (text.indexOf(':') >= 0) {
      timePart = text;
    } else {
      datePart = text;
    }

    Calendar now = Calendar.getInstance();
    int year = now.get(Calendar.YEAR);
    int month = now.get(Calendar.MONTH) + 1;
    int day = now.get(Calendar.DAY_OF_MONTH);
    int hour = 0;
    int minute = 0;

    if (datePart != null && datePart.length() > 0) {
      int[] date = parseDate(datePart);
      if (date == null) {
        return NONE;
      }
      year = date[0];
      month = date[1];
      day = date[2];
    }
    if (timePart != null && timePart.length() > 0) {
      int[] time = parseTime(timePart);
      if (time == null) {
        return NONE;
      }
      hour = time[0];
      minute = time[1];
    }
    if (!inRange(year, 1970, 2100)
        || !inRange(month, 1, 12)
        || !inRange(day, 1, 31)
        || !inRange(hour, 0, 23)
        || !inRange(minute, 0, 59)) {
      return NONE;
    }

    Calendar result = Calendar.getInstance();
    result.set(Calendar.YEAR, year);
    result.set(Calendar.MONTH, month - 1);
    result.set(Calendar.DAY_OF_MONTH, day);
    result.set(Calendar.HOUR_OF_DAY, hour);
    result.set(Calendar.MINUTE, minute);
    result.set(Calendar.SECOND, 0);
    result.set(Calendar.MILLISECOND, 0);
    return result.getTime().getTime();
  }

  public static boolean isOverdue(long due, boolean done) {
    return !done && due != NONE && due < now();
  }

  private static int[] parseDate(String text) {
    int first = text.indexOf('/');
    if (first <= 0) {
      return null;
    }
    int second = text.indexOf('/', first + 1);
    String dayText = text.substring(0, first);
    String monthText = second < 0 ? text.substring(first + 1) : text.substring(first + 1, second);
    String yearText = second < 0 ? null : text.substring(second + 1);
    if (!isDigits(dayText) || !isDigits(monthText)) {
      return null;
    }
    int day = Integer.parseInt(dayText);
    int month = Integer.parseInt(monthText);
    int year = Calendar.getInstance().get(Calendar.YEAR);
    if (yearText != null && yearText.length() > 0) {
      if (!isDigits(yearText)) {
        return null;
      }
      year = Integer.parseInt(yearText);
      if (year < 100) {
        year += 2000;
      }
    }
    return new int[] {year, month, day};
  }

  private static int[] parseTime(String text) {
    int colon = text.indexOf(':');
    if (colon <= 0) {
      return null;
    }
    String hourText = text.substring(0, colon);
    String minuteText = text.substring(colon + 1);
    if (!isDigits(hourText) || !isDigits(minuteText)) {
      return null;
    }
    return new int[] {Integer.parseInt(hourText), Integer.parseInt(minuteText)};
  }

  private static Calendar calendar(long ms) {
    Calendar c = Calendar.getInstance();
    c.setTime(new Date(ms));
    return c;
  }

  private static boolean sameDay(Calendar a, Calendar b) {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
        && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
        && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
  }

  private static boolean isDigits(String value) {
    if (value == null || value.length() == 0) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      if (value.charAt(i) < '0' || value.charAt(i) > '9') {
        return false;
      }
    }
    return true;
  }

  private static boolean inRange(int value, int min, int max) {
    return value >= min && value <= max;
  }

  private static String pad(int value, int width) {
    String text = String.valueOf(value);
    StringBuffer b = new StringBuffer();
    for (int i = text.length(); i < width; i++) {
      b.append('0');
    }
    return b.append(text).toString();
  }
}
