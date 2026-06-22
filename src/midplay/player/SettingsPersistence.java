package midplay.player;

import javax.microedition.rms.RecordStoreException;
import midplay.store.Configuration;
import midplay.store.SettingsManager;

final class SettingsPersistence {
  private final SettingsManager settingsManager;

  SettingsPersistence(SettingsManager settingsManager) {
    this.settingsManager = settingsManager;
  }

  void loadInto(PlayerGUI gui) {
    try {
      gui.volumeLevel = settingsManager.getCurrentVolumeLevel();
      gui.repeatMode = settingsManager.getCurrentRepeatMode();
      gui.isShuffleEnabled =
          Configuration.PLAYER_SHUFFLE_ON == settingsManager.getCurrentShuffleMode();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void saveVolume(int volumeLevel) {
    try {
      settingsManager.saveVolumeLevel(volumeLevel);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  void saveRepeat(int repeatMode) {
    try {
      settingsManager.saveRepeatMode(repeatMode);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  void saveShuffle(boolean enabled) {
    try {
      settingsManager.saveShuffleMode(
          enabled ? Configuration.PLAYER_SHUFFLE_ON : Configuration.PLAYER_SHUFFLE_OFF);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }
}
