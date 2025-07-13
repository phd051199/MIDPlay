package app.core.platform;

import app.constants.ServicesConstants;
import app.core.settings.SettingsManager;

public class PlayerMethod {
  public static final int PASS_URL = 0;
  public static final int PASS_CONNECTION_STREAM = 1;

  private static boolean checkClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  public static int getPlayerHttpMethod() {
    int playerHttpMethod = PASS_URL;
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
        playerHttpMethod = PASS_URL;
      } catch (Exception e) {
        // s40v2+ breaks http locator parsing
        playerHttpMethod = PASS_CONNECTION_STREAM;
      }
    } catch (Exception e) {
      playerHttpMethod = PASS_URL;
      if (symbian) {
        if (symbianJrt
            && (p.indexOf("java_build_version=2.") != -1
                || p.indexOf("java_build_version=1.4") != -1)) {
          // emc (s60v5+), supports mp3 streaming
        } else if (checkClass("com.symbian.mmapi.PlayerImpl")) {
          // uiq
          playerHttpMethod = PASS_CONNECTION_STREAM;
        } else {
          // mmf (s60v3.2-)
          playerHttpMethod = PASS_CONNECTION_STREAM;
        }
      }
    }

    if (SettingsManager.getInstance().getCurrentService().equals(ServicesConstants.SOUNDCLOUD)
        && playerHttpMethod == PASS_URL) {
      playerHttpMethod = PASS_CONNECTION_STREAM;
    }

    return playerHttpMethod;
  }

  private PlayerMethod() {}
}
