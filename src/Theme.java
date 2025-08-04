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

    try {
      Configuration.loadPlayerIcons();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static int getOnPrimaryColor() {
    return onPrimaryColor;
  }

  public static void setOnPrimaryColor(int onPrimaryColor) {
    Theme.onPrimaryColor = onPrimaryColor;
  }

  public static int getOnBackgroundColor() {
    return onBackgroundColor;
  }

  public static void setOnBackgroundColor(int onBackgroundColor) {
    Theme.onBackgroundColor = onBackgroundColor;
  }

  public static int getBackgroundColor() {
    return backgroundColor;
  }

  public static void setBackgroundColor(int backgroundColor) {
    Theme.backgroundColor = backgroundColor;
  }

  public static int getPrimaryColor() {
    return primaryColor;
  }

  public static void setPrimaryColor(int primaryColor) {
    Theme.primaryColor = primaryColor;
  }

  public static int getSurfaceColor() {
    return surfaceColor;
  }

  public static void setSurfaceColor(int surfaceColor) {
    Theme.surfaceColor = surfaceColor;
  }

  public static int getOnSurfaceColor() {
    return onSurfaceColor;
  }

  public static void setOnSurfaceColor(int onSurfaceColor) {
    Theme.onSurfaceColor = onSurfaceColor;
  }

  public static int getSurfaceVariantColor() {
    return surfaceVariantColor;
  }

  public static void setSurfaceVariantColor(int surfaceVariantColor) {
    Theme.surfaceVariantColor = surfaceVariantColor;
  }

  public static int getOnSurfaceVariantColor() {
    return onSurfaceVariantColor;
  }

  public static void setOnSurfaceVariantColor(int onSurfaceVariantColor) {
    Theme.onSurfaceVariantColor = onSurfaceVariantColor;
  }

  public static int getOutlineColor() {
    return outlineColor;
  }

  public static void setOutlineColor(int outlineColor) {
    Theme.outlineColor = outlineColor;
  }

  public static int getOutlineVariantColor() {
    return outlineVariantColor;
  }

  public static void setOutlineVariantColor(int outlineVariantColor) {
    Theme.outlineVariantColor = outlineVariantColor;
  }

  public static int getSecondaryContainerColor() {
    return secondaryContainerColor;
  }

  public static void setSecondaryContainerColor(int secondaryContainerColor) {
    Theme.secondaryContainerColor = secondaryContainerColor;
  }

  public static int getOnSecondaryContainerColor() {
    return onSecondaryContainerColor;
  }

  public static void setOnSecondaryContainerColor(int onSecondaryContainerColor) {
    Theme.onSecondaryContainerColor = onSecondaryContainerColor;
  }
}
