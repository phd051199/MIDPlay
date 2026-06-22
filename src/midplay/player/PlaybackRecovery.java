package midplay.player;

import java.util.TimerTask;
import midplay.store.Configuration;
import midplay.util.Utils;

public class PlaybackRecovery {
  private static final long START_WATCHDOG_TIMEOUT_MS = 2500L;

  private final PlayerGUI gui;

  PlaybackRecovery(PlayerGUI gui) {
    this.gui = gui;
  }

  boolean handlePlayerError(Object eventData) {
    return recoverFromFailure(eventData == null ? "unknown" : String.valueOf(eventData));
  }

  void scheduleStartWatchdog(final int sessionId) {
    if (!gui.claimStartWatchdog(sessionId)) {
      return;
    }

    TimerTask watchdog =
        new TimerTask() {
          public void run() {
            try {
              boolean shouldPromoteStarted = false;
              boolean recovered = false;
              synchronized (gui) {
                if (sessionId == gui.currentSessionId()) {
                  if (!gui.isSessionStarted() && gui.isPlayerStarted(gui.currentPlayerLocked())) {
                    shouldPromoteStarted = true;
                  } else if (!gui.isSessionStarted()) {
                    recovered = tryRecoverMethodFailureLocked("start watchdog timeout", true);
                  }
                }
                gui.releaseStartWatchdog(sessionId);
              }

              if (shouldPromoteStarted && gui.isSessionActive(sessionId)) {
                gui.onPlaybackStarted();
                return;
              }

              if (recovered && gui.isSessionActive(sessionId)) {
                restartAfterRecovery();
              }
            } catch (Throwable t) {
            }
          }
        };
    gui.scheduleStartWatchdogTask(watchdog, START_WATCHDOG_TIMEOUT_MS);
  }

  boolean recoverFromFailure(String reason) {
    boolean recovered;
    synchronized (gui) {
      recovered =
          tryRecoverMethodFailureLocked(reason, true)
              || tryRecoverMethodFailureLocked(reason, false);
    }

    if (recovered) {
      restartAfterRecovery();
      return true;
    }

    return false;
  }

  private void restartAfterRecovery() {
    gui.stopTimer();
    gui.setStatusByKey(Configuration.PLAYER_STATUS_LOADING);
    gui.play();
  }

  private boolean tryRecoverMethodFailureLocked(String reason, boolean usedInputStream) {
    int sessionId = gui.currentSessionId();
    if (gui.isDestroyed() || sessionId <= 0) {
      return false;
    }

    if (gui.sessionMethodSessionId() != sessionId
        || gui.sessionUsedInputStream() != usedInputStream
        || gui.isSessionStarted()) {
      return false;
    }

    if (usedInputStream) {
      if (gui.isLastFallbackFor(sessionId)) {
        return false;
      }
      gui.markLastFallback(sessionId);
      gui.resetPlaybackStateLocked();
      gui.setPendingPlayerMethodOverride(PlaybackMethod.URL);
      return true;
    }

    if (!gui.hasPreferredInputStreamContentType()) {
      return false;
    }

    if (!isUnknownContentTypeFailure(reason) || gui.isLastFallbackFor(sessionId)) {
      return false;
    }

    gui.markLastFallback(sessionId);
    gui.resetPlaybackStateLocked();
    gui.setPendingPlayerMethodOverride(PlaybackMethod.INPUT_STREAM);
    return true;
  }

  private boolean isUnknownContentTypeFailure(String reason) {
    if (reason == null) {
      return false;
    }
    return Utils.indexOfIgnoreCase(reason, "unknown content type") != -1
        || Utils.indexOfIgnoreCase(reason, "unsupported content type") != -1
        || (Utils.indexOfIgnoreCase(reason, "unknown") != -1
            && (Utils.indexOfIgnoreCase(reason, "content type") != -1
                || Utils.indexOfIgnoreCase(reason, "content-type") != -1));
  }
}
