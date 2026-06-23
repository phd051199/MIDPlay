package midplay.player;

import javax.microedition.amms.GlobalManager;
import javax.microedition.amms.control.audioeffect.EqualizerControl;
import midplay.store.SettingsManager;
import midplay.util.Utils;

public final class EqualizerEngine {

  private static final String GLOBAL_MANAGER = "javax.microedition.amms.GlobalManager";
  private static final String EQ_CONTROL_TYPE =
      "javax.microedition.amms.control.audioeffect.EqualizerControl";

  private static boolean resolved;
  private static Object controlRef;

  private EqualizerEngine() {}

  private static Object control() {
    if (!Utils.hasClass(GLOBAL_MANAGER)) {
      return null;
    }
    if (!resolved) {
      resolved = true;
      try {
        controlRef = GlobalManager.getControl(EQ_CONTROL_TYPE);
      } catch (Throwable t) {
        controlRef = null;
      }
    }
    return controlRef;
  }

  private static EqualizerControl eq() {
    Object ref = control();
    if (ref == null) {
      return null;
    }
    try {
      return (EqualizerControl) ref;
    } catch (Throwable t) {
      return null;
    }
  }

  public static boolean isSupported() {
    return eq() != null;
  }

  public static int getBandCount() {
    EqualizerControl e = eq();
    try {
      return e == null ? 0 : e.getNumberOfBands();
    } catch (Throwable t) {
      return 0;
    }
  }

  public static int getMinLevel() {
    EqualizerControl e = eq();
    try {
      return e == null ? -1500 : e.getMinBandLevel();
    } catch (Throwable t) {
      return -1500;
    }
  }

  public static int getMaxLevel() {
    EqualizerControl e = eq();
    try {
      return e == null ? 1500 : e.getMaxBandLevel();
    } catch (Throwable t) {
      return 1500;
    }
  }

  public static int getCenterFreq(int band) {
    EqualizerControl e = eq();
    try {
      return e == null ? 0 : e.getCenterFreq(band);
    } catch (Throwable t) {
      return 0;
    }
  }

  public static int getBandLevel(int band) {
    EqualizerControl e = eq();
    try {
      return e == null ? 0 : e.getBandLevel(band);
    } catch (Throwable t) {
      return 0;
    }
  }

  public static String[] getPresetNames() {
    EqualizerControl e = eq();
    try {
      return e == null ? null : e.getPresetNames();
    } catch (Throwable t) {
      return null;
    }
  }

  public static void applyFromSettings() {
    EqualizerControl e = eq();
    if (e == null) {
      return;
    }
    SettingsManager s = SettingsManager.getInstance();
    boolean enabled = s.isEqEnabled();
    try {
      e.setEnabled(enabled);
    } catch (Throwable t) {
    }
    if (!enabled) {
      return;
    }
    int preset = s.getEqPreset();
    if (preset >= 0) {
      try {
        String[] names = e.getPresetNames();
        if (names != null && preset < names.length) {
          e.setPreset(names[preset]);
        }
      } catch (Throwable t) {
      }
      return;
    }
    int[] levels = s.getEqLevels();
    if (levels == null) {
      return;
    }
    int bands;
    try {
      bands = e.getNumberOfBands();
    } catch (Throwable t) {
      return;
    }
    for (int i = 0; i < bands && i < levels.length; i++) {
      try {
        e.setBandLevel(levels[i], i);
      } catch (Throwable t) {
      }
    }
  }
}
