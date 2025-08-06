import cc.nnproject.json.JSONObject;

public class Theme {
  private static boolean isDark = false;

  private static int primaryColor = 0x65558F;
  private static int onPrimaryColor = 0xFFFFFF;
  private static int backgroundColor = 0xFDF7FF;
  private static int onBackgroundColor = 0x1D1B20;
  private static int surfaceColor = 0xFDF7FF;
  private static int onSurfaceColor = 0x1D1B20;
  private static int surfaceVariantColor = 0xE7E0EB;
  private static int onSurfaceVariantColor = 0x49454E;
  private static int outlineColor = 0x7A757F;
  private static int outlineVariantColor = 0xCAC4CF;
  private static int secondaryContainerColor = 0xE8DEF8;
  private static int onSecondaryContainerColor = 0x4A4458;

  public static boolean isDark() {
    return isDark;
  }

  public static void setDark(boolean isDark) {
    Theme.isDark = isDark;
  }

  public static void applyDefaults(boolean isDark) {
    if (isDark) {
      setPrimaryColor(0xCFBDFE);
      setOnPrimaryColor(0x36275D);
      setBackgroundColor(0x141218);
      setOnBackgroundColor(0xE6E0E9);
      setSurfaceColor(0x141218);
      setOnSurfaceColor(0xE6E0E9);
      setSurfaceVariantColor(0x49454E);
      setOnSurfaceVariantColor(0xCAC4CF);
      setOutlineColor(0x948F99);
      setOutlineVariantColor(0x49454E);
      setSecondaryContainerColor(0x4A4458);
      setOnSecondaryContainerColor(0xE8DEF8);
    } else {
      setPrimaryColor(0x65558F);
      setOnPrimaryColor(0xFFFFFF);
      setBackgroundColor(0xFDF7FF);
      setOnBackgroundColor(0x1D1B20);
      setSurfaceColor(0xFDF7FF);
      setOnSurfaceColor(0x1D1B20);
      setSurfaceVariantColor(0xE7E0EB);
      setOnSurfaceVariantColor(0x49454E);
      setOutlineColor(0x7A757F);
      setOutlineVariantColor(0xCAC4CF);
      setSecondaryContainerColor(0xE8DEF8);
      setOnSecondaryContainerColor(0x4A4458);
    }
  }

  public static void applyColors(JSONObject colors) {
    if (colors == null) {
      return;
    }

    setPrimaryColor(colors.getInt("primary", primaryColor));
    setOnPrimaryColor(colors.getInt("onPrimary", onPrimaryColor));
    setBackgroundColor(colors.getInt("background", backgroundColor));
    setOnBackgroundColor(colors.getInt("onBackground", onBackgroundColor));
    setSurfaceColor(colors.getInt("surface", surfaceColor));
    setOnSurfaceColor(colors.getInt("onSurface", onSurfaceColor));
    setSurfaceVariantColor(colors.getInt("surfaceVariant", surfaceVariantColor));
    setOnSurfaceVariantColor(colors.getInt("onSurfaceVariant", onSurfaceVariantColor));
    setOutlineColor(colors.getInt("outline", outlineColor));
    setOutlineVariantColor(colors.getInt("outlineVariant", outlineVariantColor));
    setSecondaryContainerColor(colors.getInt("secondaryContainer", secondaryContainerColor));
    setOnSecondaryContainerColor(colors.getInt("onSecondaryContainer", onSecondaryContainerColor));
  }

  public static int getPrimaryColor() {
    return primaryColor;
  }

  public static int getOnPrimaryColor() {
    return onPrimaryColor;
  }

  public static int getBackgroundColor() {
    return backgroundColor;
  }

  public static int getOnBackgroundColor() {
    return onBackgroundColor;
  }

  public static int getSurfaceColor() {
    return surfaceColor;
  }

  public static int getOnSurfaceColor() {
    return onSurfaceColor;
  }

  public static int getSurfaceVariantColor() {
    return surfaceVariantColor;
  }

  public static int getOnSurfaceVariantColor() {
    return onSurfaceVariantColor;
  }

  public static int getOutlineColor() {
    return outlineColor;
  }

  public static int getOutlineVariantColor() {
    return outlineVariantColor;
  }

  public static int getSecondaryContainerColor() {
    return secondaryContainerColor;
  }

  public static int getOnSecondaryContainerColor() {
    return onSecondaryContainerColor;
  }

  public static void setPrimaryColor(int color) {
    primaryColor = color;
  }

  public static void setOnPrimaryColor(int color) {
    onPrimaryColor = color;
  }

  public static void setBackgroundColor(int color) {
    backgroundColor = color;
  }

  public static void setOnBackgroundColor(int color) {
    onBackgroundColor = color;
  }

  public static void setSurfaceColor(int color) {
    surfaceColor = color;
  }

  public static void setOnSurfaceColor(int color) {
    onSurfaceColor = color;
  }

  public static void setSurfaceVariantColor(int color) {
    surfaceVariantColor = color;
  }

  public static void setOnSurfaceVariantColor(int color) {
    onSurfaceVariantColor = color;
  }

  public static void setOutlineColor(int color) {
    outlineColor = color;
  }

  public static void setOutlineVariantColor(int color) {
    outlineVariantColor = color;
  }

  public static void setSecondaryContainerColor(int color) {
    secondaryContainerColor = color;
  }

  public static void setOnSecondaryContainerColor(int color) {
    onSecondaryContainerColor = color;
  }

  private Theme() {}
}
