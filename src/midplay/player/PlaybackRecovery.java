package midplay.player;

import midplay.store.Configuration;

import java.util.TimerTask;


// Playback failure recovery for PlayerGUI: handles PlayerListener.ERROR, the
// start watchdog (which promotes a started-but-unreported player or falls back
// to the alternate player method on timeout), and the inputstream/url method
// fallback. Extracted verbatim from PlayerGUI; it still synchronizes on the
// PlayerGUI instance (passed as `gui`) so the session/lock protocol — including
// the "Locked" calling convention (caller already holds the gui lock) — is
// unchanged.
public class PlaybackRecovery {
  private static final long START_WATCHDOG_TIMEOUT_MS = 2500L;

  private final PlayerGUI gui;

  PlaybackRecovery(PlayerGUI gui) {
    this.gui = gui;
  }

  boolean handlePlayerError(Object eventData) {
    String errorText = eventData == null ? "unknown" : String.valueOf(eventData);
    return recoverFromFailure("player error: " + errorText);
  }

  void scheduleStartWatchdog(final int sessionId) {
    synchronized (gui) {
      if (sessionId != gui.playbackSessionId || gui.startWatchdogSessionId == sessionId) {
        return;
      }
      gui.startWatchdogSessionId = sessionId;
    }

    // Scheduled as a one-shot on the shared player timer instead of a dedicated
    // Thread per playback start. The body is guarded so a throw can't kill the
    // shared timer thread (which would also silently stop the per-second repaint).
    TimerTask watchdog =
        new TimerTask() {
          public void run() {
            try {
              boolean shouldPromoteStarted = false;
              boolean recovered = false;
              synchronized (gui) {
                if (sessionId == gui.playbackSessionId) {
                  if (!gui.sessionStarted && gui.isPlayerStarted(gui.player)) {
                    shouldPromoteStarted = true;
                  } else if (!gui.sessionStarted) {
                    recovered = tryRecoverMethodFailureLocked("start watchdog timeout", true);
                  }
                }
                if (gui.startWatchdogSessionId == sessionId) {
                  gui.startWatchdogSessionId = -1;
                }
              }

              if (shouldPromoteStarted && gui.isCurrentSession(sessionId)) {
                gui.onPlaybackStarted();
                return;
              }

              if (recovered && gui.isCurrentSession(sessionId)) {
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
      recovered = tryRecoverPlayerMethodFailureLocked(reason);
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

  private boolean tryRecoverPlayerMethodFailureLocked(String reason) {
    if (tryRecoverMethodFailureLocked(reason, true)) {
      return true;
    }
    return tryRecoverMethodFailureLocked(reason, false);
  }

  // Recover a failed playback by falling back to the *other* player method:
  // when usedInputStream is true (inputstream failed) fall back to pass_url;
  // when false (url failed with an unknown content type) fall back to pass_inputstream.
  // Guard conditions mirror the former tryRecoverInputStreamFailureLocked /
  // tryRecoverUrlContentTypeFailureLocked exactly.
  private boolean tryRecoverMethodFailureLocked(String reason, boolean usedInputStream) {
    int sessionId = gui.playbackSessionId;
    if (gui.destroyed || sessionId <= 0) {
      return false;
    }

    if (gui.sessionMethodSessionId != sessionId || gui.sessionUsedInputStream != usedInputStream || gui.sessionStarted) {
      return false;
    }

    if (usedInputStream) {
      if (gui.lastFallbackSessionId == sessionId) {
        return false;
      }
      gui.lastFallbackSessionId = sessionId;
      gui.resetPlaybackStateLocked();
      gui.pendingPlayerMethodOverride = PlaybackMethod.URL;
      return true;
    }

    if (gui.mediaResolver.getPreferredInputStreamContentType() == null) {
      return false;
    }

    if (!isUnknownContentTypeFailure(reason) || gui.lastFallbackSessionId == sessionId) {
      return false;
    }

    gui.lastFallbackSessionId = sessionId;
    gui.resetPlaybackStateLocked();
    gui.pendingPlayerMethodOverride = PlaybackMethod.INPUT_STREAM;
    return true;
  }

  private boolean isUnknownContentTypeFailure(String reason) {
    if (reason == null) {
      return false;
    }

    String normalized = reason.toLowerCase();
    return normalized.indexOf("unknown content type") != -1
        || normalized.indexOf("unsupported content type") != -1
        || (normalized.indexOf("unknown") != -1
            && (normalized.indexOf("content type") != -1
                || normalized.indexOf("content-type") != -1));
  }
}
