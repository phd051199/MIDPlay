package midplay.ui;

import cc.nnproject.json.JSONObject;

// Theme colors. Public API is a set of static accessors (call sites read colors
// via Theme.getXxxColor()) so the 12 color ints are grouped into a single
// Palette value object internally; the light/dark palettes are each defined
// once as a constant instead of being duplicated across the field initializers
// and applyDefaults.
public class Theme {
  // Field order: primary, onPrimary, background, onBackground, surface,
  // onSurface, surfaceVariant, onSurfaceVariant, outline, outlineVariant,
  // secondaryContainer, onSecondaryContainer.
  private static final class Palette {
    final int primary;
    final int onPrimary;
    final int background;
    final int onBackground;
    final int surface;
    final int onSurface;
    final int surfaceVariant;
    final int onSurfaceVariant;
    final int outline;
    final int outlineVariant;
    final int secondaryContainer;
    final int onSecondaryContainer;

    Palette(
        int primary,
        int onPrimary,
        int background,
        int onBackground,
        int surface,
        int onSurface,
        int surfaceVariant,
        int onSurfaceVariant,
        int outline,
        int outlineVariant,
        int secondaryContainer,
        int onSecondaryContainer) {
      this.primary = primary;
      this.onPrimary = onPrimary;
      this.background = background;
      this.onBackground = onBackground;
      this.surface = surface;
      this.onSurface = onSurface;
      this.surfaceVariant = surfaceVariant;
      this.onSurfaceVariant = onSurfaceVariant;
      this.outline = outline;
      this.outlineVariant = outlineVariant;
      this.secondaryContainer = secondaryContainer;
      this.onSecondaryContainer = onSecondaryContainer;
    }

    Palette override(JSONObject colors) {
      return new Palette(
          colors.getInt("primary", primary),
          colors.getInt("onPrimary", onPrimary),
          colors.getInt("background", background),
          colors.getInt("onBackground", onBackground),
          colors.getInt("surface", surface),
          colors.getInt("onSurface", onSurface),
          colors.getInt("surfaceVariant", surfaceVariant),
          colors.getInt("onSurfaceVariant", onSurfaceVariant),
          colors.getInt("outline", outline),
          colors.getInt("outlineVariant", outlineVariant),
          colors.getInt("secondaryContainer", secondaryContainer),
          colors.getInt("onSecondaryContainer", onSecondaryContainer));
    }
  }

  private static final Palette LIGHT =
      new Palette(
          0x65558F, 0xFFFFFF, 0xFDF7FF, 0x1D1B20, 0xFDF7FF, 0x1D1B20,
          0xE7E0EB, 0x49454E, 0x7A757F, 0xCAC4CF, 0xE8DEF8, 0x4A4458);
  private static final Palette DARK =
      new Palette(
          0xCFBDFE, 0x36275D, 0x141218, 0xE6E0E9, 0x141218, 0xE6E0E9,
          0x49454E, 0xCAC4CF, 0x948F99, 0x49454E, 0x4A4458, 0xE8DEF8);

  private static Palette current = LIGHT;
  // Note: applyDefaults(boolean) sets colors from its parameter but, by design,
  // does NOT update this flag — only setDark() does. Preserved as-is.
  private static boolean isDark = false;

  public static boolean isDark() {
    return isDark;
  }

  public static void setDark(boolean isDark) {
    Theme.isDark = isDark;
  }

  public static void applyDefaults(boolean dark) {
    current = dark ? DARK : LIGHT;
  }

  public static void applyColors(JSONObject colors) {
    if (colors == null) {
      return;
    }
    current = current.override(colors);
  }

  public static int getPrimaryColor() {
    return current.primary;
  }

  public static int getOnPrimaryColor() {
    return current.onPrimary;
  }

  public static int getBackgroundColor() {
    return current.background;
  }

  public static int getOnBackgroundColor() {
    return current.onBackground;
  }

  public static int getSurfaceColor() {
    return current.surface;
  }

  public static int getOnSurfaceColor() {
    return current.onSurface;
  }

  public static int getSurfaceVariantColor() {
    return current.surfaceVariant;
  }

  public static int getOnSurfaceVariantColor() {
    return current.onSurfaceVariant;
  }

  public static int getOutlineColor() {
    return current.outline;
  }

  public static int getOutlineVariantColor() {
    return current.outlineVariant;
  }

  public static int getSecondaryContainerColor() {
    return current.secondaryContainer;
  }

  public static int getOnSecondaryContainerColor() {
    return current.onSecondaryContainer;
  }

  private Theme() {}
}
