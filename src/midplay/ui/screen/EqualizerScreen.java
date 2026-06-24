package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.rms.RecordStoreException;
import midplay.player.EqualizerEngine;
import midplay.store.SettingsManager;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class EqualizerScreen extends BaseForm implements ItemStateListener {
  private final SettingsManager settings = SettingsManager.getInstance();

  private ChoiceGroup enableGroup;
  private ChoiceGroup presetGroup;
  private Gauge[] bandGauges;
  private int minLevel;
  private int range;
  private int bandCount;
  private boolean updating;

  private boolean originalEnabled;
  private int originalPreset;
  private int[] originalLevels;

  public EqualizerScreen(Navigator navigator) {
    super(Lang.tr("menu.equalizer"), navigator);
    bandCount = EqualizerEngine.isSupported() ? EqualizerEngine.getBandCount() : 0;
    if (bandCount <= 0) {
      append(new StringItem(null, Lang.tr("eq.not_supported")));
      return;
    }
    buildControls();
    addCommand(Commands.formSave());
    setItemStateListener(this);
  }

  private void buildControls() {
    minLevel = EqualizerEngine.getMinLevel();
    range = Math.max(1, EqualizerEngine.getMaxLevel() - minLevel);

    originalEnabled = settings.isEqEnabled();
    originalPreset = settings.getEqPreset();

    enableGroup = new ChoiceGroup(Lang.tr("eq.enable"), ChoiceGroup.MULTIPLE);
    enableGroup.append(Lang.tr("eq.enable"), null);
    enableGroup.setSelectedIndex(0, settings.isEqEnabled());
    append(enableGroup);

    String[] presets = EqualizerEngine.getPresetNames();
    presetGroup = new ChoiceGroup(Lang.tr("eq.presets"), ChoiceGroup.POPUP);
    int manualIndex = presetGroup.append(Lang.tr("eq.manual"), null);
    if (presets != null) {
      for (int i = 0; i < presets.length; i++) {
        presetGroup.append(presets[i], null);
      }
    }
    int preset = settings.getEqPreset();
    presetGroup.setSelectedIndex(preset < 0 ? manualIndex : preset + 1, true);
    append(presetGroup);

    int[] levels = settings.getEqLevels();
    if (levels == null || levels.length != bandCount) {
      levels = new int[bandCount];
    }
    originalLevels = levels;
    bandGauges = new Gauge[bandCount];
    for (int i = 0; i < bandCount; i++) {
      String label = formatFreq(EqualizerEngine.getCenterFreq(i));
      if (label.length() == 0) {
        label = Lang.tr("eq.band") + " " + (i + 1);
      }
      bandGauges[i] = new Gauge(label, true, notches(), levelToGauge(levels[i]));
      append(bandGauges[i]);
    }
  }

  private int notches() {
    return range < 1 ? 1 : range;
  }

  private int levelToGauge(int level) {
    int g = level - minLevel;
    if (g < 0) {
      g = 0;
    }
    if (g > range) {
      g = range;
    }
    return g;
  }

  private int gaugeToLevel(int gaugeValue) {
    return gaugeValue + minLevel;
  }

  private int[] readLevels() {
    if (bandGauges == null) {
      return null;
    }
    int[] levels = new int[bandGauges.length];
    for (int i = 0; i < bandGauges.length; i++) {
      levels[i] = gaugeToLevel(bandGauges[i].getValue());
    }
    return levels;
  }

  private int selectedPreset() {
    if (presetGroup == null) {
      return -1;
    }
    int sel = presetGroup.getSelectedIndex();
    return sel <= 0 ? -1 : sel - 1;
  }

  private void syncGaugesFromDevice() {
    if (bandGauges == null) {
      return;
    }
    updating = true;
    try {
      int[] levels = new int[bandGauges.length];
      boolean flat = true;
      for (int i = 0; i < bandGauges.length; i++) {
        levels[i] = EqualizerEngine.getBandLevel(i);
        if (levels[i] != levels[0]) {
          flat = false;
        }
      }
      if (!flat) {
        for (int i = 0; i < bandGauges.length; i++) {
          bandGauges[i].setValue(levelToGauge(levels[i]));
        }
      }
    } catch (Throwable t) {
    }
    updating = false;
  }

  private void setEnabledQuiet(boolean enabled) {
    updating = true;
    try {
      enableGroup.setSelectedIndex(0, enabled);
    } catch (Throwable t) {
    }
    updating = false;
  }

  private void setPresetQuiet(int presetGroupIndex) {
    updating = true;
    try {
      presetGroup.setSelectedIndex(presetGroupIndex, true);
    } catch (Throwable t) {
    }
    updating = false;
  }

  public void itemStateChanged(Item item) {
    if (updating) {
      return;
    }
    if (item == enableGroup) {
      applyState();
    } else if (item == presetGroup) {
      onPresetChanged();
    } else {
      onGaugeMoved();
    }
  }

  private void onPresetChanged() {
    setEnabledQuiet(true);
    settings.setEqEnabledLive(true);
    settings.setEqPresetLive(selectedPreset());
    settings.setEqLevelsLive(null);
    EqualizerEngine.applyFromSettings();
    syncGaugesFromDevice();
  }

  private void onGaugeMoved() {
    if (selectedPreset() >= 0) {
      setPresetQuiet(0);
    }
    settings.setEqEnabledLive(true);
    setEnabledQuiet(true);
    settings.setEqPresetLive(-1);
    settings.setEqLevelsLive(readLevels());
    EqualizerEngine.applyFromSettings();
  }

  private void applyState() {
    settings.setEqEnabledLive(enableGroup.isSelected(0));
    settings.setEqPresetLive(selectedPreset());
    settings.setEqLevelsLive(readLevels());
    EqualizerEngine.applyFromSettings();
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.formSave()) {
      if (!isEqDirty()) {
        navigator.back();
        return;
      }
      try {
        settings.saveEqualizer(
            enableGroup != null && enableGroup.isSelected(0), selectedPreset(), readLevels());
        navigator.showAlert(
            Lang.tr("settings.status.saved"), AlertType.CONFIRMATION, navigator.getPrevious());
      } catch (RecordStoreException e) {
        navigator.showAlert(Lang.tr("settings.error.save_failed"), AlertType.ERROR);
      }
    }
  }

  protected boolean onBackPressed() {
    if (!isEqDirty()) {
      return false;
    }
    navigator.showConfirmationAlert(
        Lang.tr("confirm.discard_changes"),
        new Runnable() {
          public void run() {
            revertEq();
            navigator.back();
          }
        },
        AlertType.WARNING);
    return true;
  }

  private boolean isEqDirty() {
    if (enableGroup == null) {
      return false;
    }
    if (enableGroup.isSelected(0) != originalEnabled) {
      return true;
    }
    if (selectedPreset() != originalPreset) {
      return true;
    }
    return !levelsEqual(readLevels(), originalLevels);
  }

  private void revertEq() {
    if (bandGauges == null) {
      return;
    }
    settings.setEqEnabledLive(originalEnabled);
    settings.setEqPresetLive(originalPreset);
    settings.setEqLevelsLive(originalLevels);
    EqualizerEngine.applyFromSettings();
  }

  private static boolean levelsEqual(int[] a, int[] b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null || a.length != b.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }
    return true;
  }

  private static String formatFreq(int milliHertz) {
    if (milliHertz <= 0) {
      return "";
    }
    int hz = milliHertz / 1000;
    if (hz >= 1000) {
      return (hz / 1000) + " kHz";
    }
    return hz + " Hz";
  }
}
