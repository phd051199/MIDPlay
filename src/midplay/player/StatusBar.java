package midplay.player;

import midplay.store.Configuration;
import midplay.util.Lang;

final class StatusBar {
  private String currentStatusKey = Configuration.PLAYER_STATUS_STOPPED;
  private String statusCurrent = "";
  private String realPlayerStatus = "";
  private boolean timerOverrideActive = false;
  private String loadingText;
  private String errorText;

  String getStatusCurrent() {
    return statusCurrent;
  }

  String getCurrentStatusKey() {
    return currentStatusKey;
  }

  void setStatus(String s) {
    if (s == null) {
      s = "";
    }
    realPlayerStatus = s;
    if (!timerOverrideActive) {
      statusCurrent = s;
    } else if (isCriticalStatus(s)) {
      statusCurrent = s;
      timerOverrideActive = false;
    }
  }

  void setStatusByKey(String statusKey) {
    currentStatusKey = statusKey;
    setStatus(Lang.tr(statusKey));
  }

  void setTimerOverride(String timerStatus) {
    timerOverrideActive = true;
    statusCurrent = timerStatus;
  }

  void clearTimerOverride() {
    timerOverrideActive = false;
    statusCurrent = realPlayerStatus;
  }

  private boolean isCriticalStatus(String status) {
    if (status == null) {
      return false;
    }
    if (loadingText == null) {
      loadingText = Lang.tr("status.loading");
      errorText = Lang.tr("status.error");
    }
    return status.indexOf(loadingText) != -1 || status.indexOf(errorText) != -1;
  }
}
