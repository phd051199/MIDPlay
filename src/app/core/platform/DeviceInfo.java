package app.core.platform;

import app.constants.ServicesConstants;
import app.core.settings.SettingsManager;

public class DeviceInfo {

  public static final int PLAYER_PASS_URL = 0;
  public static final int PLAYER_PASS_CONNECTION_STREAM = 1;
  public static String deviceId = null;

  private static boolean checkClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static String getPropertyValue(String[] properties) {
    for (int i = 0; i < properties.length; i++) {
      String value = System.getProperty(properties[i]);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static void setDeviceId() {
    try {
      if (checkClass("emulator.custom.CustomMethod")) {
        deviceId = "emulator";
        return;
      }

      String id =
          getPropertyValue(
              new String[] {
                "phone.imei",
                "com.nokia.imei",
                "com.nokia.mid.imei",
                "IMEI",
                "com.motorola.IMEI",
                "com.lge.imei",
                "com.siemens.IMEI",
                "com.samsung.imei",
                "com.sonyericsson.imei"
              });
      if (id != null) {
        deviceId = id;
      } else {
        deviceId = "unknown";
      }
    } catch (Exception e) {
      deviceId = "unknown";
    }
  }

  public static String getDeviceId() {
    if (deviceId == null) {
      setDeviceId();
    }
    return deviceId;
  }

  // reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  public static int getPlayerHttpMethod() {
    SettingsManager settingsManager = SettingsManager.getInstance();

    if (settingsManager.isForcePassConnectionEnabled()) {
      return PLAYER_PASS_CONNECTION_STREAM;
    }

    if (settingsManager.getCurrentService().equals(ServicesConstants.SOUNDCLOUD)) {
      return PLAYER_PASS_CONNECTION_STREAM;
    }

    int playerHttpMethod = PLAYER_PASS_URL;
    // platform
    boolean symbianJrt = false;
    boolean symbian = false;

    String p, v;
    if ((p = System.getProperty("microedition.platform")) != null) {
      if ((symbianJrt = p.indexOf("platform=S60") != -1)) {
        int i;
        v = p.substring(i = p.indexOf("platform_version=") + 17, i = p.indexOf(';', i));
      }

      try {
        Class.forName("emulator.custom.CustomMethod");
        p = "KEmulator";
        if ((v = System.getProperty("kemulator.mod.version")) != null) {
          p = p.concat(" ".concat(v));
        }
      } catch (Exception e) {
        int i;

        if ((i = p.indexOf('/')) != -1 || (i = p.indexOf(' ')) != -1) {
          p = p.substring(0, i);
        }
      }
    }
    symbian =
        symbianJrt
            || System.getProperty("com.symbian.midp.serversocket.support") != null
            || System.getProperty("com.symbian.default.to.suite.icon") != null
            || checkClass("com.symbian.midp.io.protocol.http.Protocol")
            || checkClass("com.symbian.lcdjava.io.File");

    // check media capabilities
    try {
      // s40 check
      Class.forName("com.nokia.mid.impl.isa.jam.Jam");
      try {
        Class.forName("com.sun.mmedia.protocol.CommonDS");
        // s40v1 uses sun impl for media and i/o so it should work fine
        playerHttpMethod = PLAYER_PASS_URL;
      } catch (Exception e) {
        // s40v2+ breaks http locator parsing
        playerHttpMethod = PLAYER_PASS_CONNECTION_STREAM;
      }
    } catch (Exception e) {
      playerHttpMethod = PLAYER_PASS_URL;
      if (symbian) {
        if (symbianJrt
            && (p.indexOf("java_build_version=2.") != -1
                || p.indexOf("java_build_version=1.4") != -1)) {
          // emc (s60v5+), supports mp3 streaming
        } else if (checkClass("com.symbian.mmapi.PlayerImpl")) {
          // uiq
          playerHttpMethod = PLAYER_PASS_CONNECTION_STREAM;
        } else {
          // mmf (s60v3.2-)
          playerHttpMethod = PLAYER_PASS_CONNECTION_STREAM;
        }
      }
    }

    return playerHttpMethod;
  }

  private DeviceInfo() {}
}
