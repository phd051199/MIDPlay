package app.common;

public class PlayerMethod {
  private static int playerHttpMethod =
      -1; // 0 - pass url, 1 - save to file, 2 - pass connection stream
  // platform
  private static boolean symbianJrt;
  private static boolean symbian;

  private static boolean checkClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // reference https://github.com/shinovon/mpgram-client/blob/master/src/MP.java
  public static void setPlayerHttpMethod() {
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
        playerHttpMethod = 0;
      } catch (Exception e) {
        // s40v2+ breaks http locator parsing
        playerHttpMethod = 1;
      }
    } catch (Exception e) {
      playerHttpMethod = 0;
      if (symbian) {
        if (symbianJrt
            && (p.indexOf("java_build_version=2.") != -1
                || p.indexOf("java_build_version=1.4") != -1)) {
          // emc (s60v5+), supports mp3 streaming
        } else if (checkClass("com.symbian.mmapi.PlayerImpl")) {
          // uiq
          playerHttpMethod = 2;
        } else {
          // mmf (s60v3.2-)
          playerHttpMethod = 2;
        }
      }
    }
  }

  public static int getPlayerHttpMethod() {
    if (playerHttpMethod == -1) {
      setPlayerHttpMethod();
    }
    return playerHttpMethod;
  }
}
