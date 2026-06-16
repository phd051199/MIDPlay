package midplay.player;

import midplay.store.Configuration;
import midplay.util.Lang;

// The player status bar's state, extracted from PlayerScreen: the current
// status key, the displayed text (which may be a sleep-timer override), the
// real underlying status, and the timer-override flag. A sleep-timer override
// shows until a critical status (loading/error) arrives, which preempts it.
final class StatusBar {
  private String currentStatusKey = Configuration.PLAYER_STATUS_STOPPED;
  private String statusCurrent = "";
  private String realPlayerStatus = "";
  private boolean timerOverrideActive = false;

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
    return status != null
        && (status.indexOf(Lang.tr("status.loading")) != -1
            || status.indexOf(Lang.tr("status.error")) != -1);
  }
}
