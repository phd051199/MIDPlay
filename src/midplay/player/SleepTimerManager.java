package midplay.player;

import midplay.MIDPlay;
import midplay.ui.screen.SleepTimerForm;
import midplay.util.Lang;

import java.util.Timer;
import java.util.TimerTask;

public class SleepTimerManager {
  // Lazy daemon timer reused across countdowns and the exit one-shot, so we
  // don't spawn (and tear down) a fresh timer thread each time the sleep timer
  // is set. It idles at ~zero cost and dies with the VM.
  private Timer timer;
  private TimerTask sleepTimerTask;
  private SleepTimerCallback callback;
  private boolean isActive = false;
  private long targetTimeMillis;
  private int timerAction;
  private String timerDescription;

  public SleepTimerManager() {}

  public void setCallback(SleepTimerCallback callback) {
    this.callback = callback;
  }

  public boolean isActive() {
    return isActive;
  }

  public String getTimerDescription() {
    return timerDescription;
  }

  public String getRemainingTime() {
    if (!isActive) {
      return "";
    }
    long currentTime = System.currentTimeMillis();
    long remainingMillis = targetTimeMillis - currentTime;
    if (remainingMillis <= 0) {
      return "00:00";
    }
    long remainingSeconds = remainingMillis / 1000;
    long hours = remainingSeconds / 3600;
    long minutes = (remainingSeconds % 3600) / 60;
    long seconds = remainingSeconds % 60;
    if (hours > 0) {
      return formatTime(hours) + ":" + formatTime(minutes) + ":" + formatTime(seconds);
    } else {
      return formatTime(minutes) + ":" + formatTime(seconds);
    }
  }

  private String formatTime(long value) {
    return value < 10 ? "0" + value : String.valueOf(value);
  }

  public void startCountdownTimer(int durationMinutes, int action) {
    cancelTimer();
    long durationMillis = durationMinutes * 60 * 1000L;
    targetTimeMillis = System.currentTimeMillis() + durationMillis;
    timerAction = action;
    timerDescription = durationMinutes + " " + Lang.tr("time.minutes");
    startTimer();
  }

  private Timer timer() {
    if (timer == null) {
      timer = new Timer();
    }
    return timer;
  }

  private void startTimer() {
    isActive = true;
    sleepTimerTask =
        new TimerTask() {
          public void run() {
            // Guard the shared timer thread: an uncaught throw would kill it and
            // silently stop the countdown.
            try {
              long currentTime = System.currentTimeMillis();
              long remainingMillis = targetTimeMillis - currentTime;
              if (remainingMillis <= 0) {
                executeTimerAction();
              } else {
                updateTimerDisplay();
              }
            } catch (Throwable t) {
            }
          }
        };
    timer().scheduleAtFixedRate(sleepTimerTask, 0, 1000);
  }

  private void updateTimerDisplay() {
    if (callback != null && isActive) {
      callback.onTimerUpdate(getRemainingTime());
    }
  }

  private void executeTimerAction() {
    final int action = timerAction;
    clearTimer(false);
    if (callback != null) {
      callback.onTimerExpired(action);
    }
    if (action == SleepTimerForm.ACTION_EXIT_APP) {
      timer().schedule(
          new TimerTask() {
            public void run() {
              try {
                MIDPlay.getInstance().notifyDestroyed();
              } catch (Throwable t) {
              }
            }
          },
          1000);
    }
  }

  public void cancelTimer() {
    clearTimer(true);
  }

  private void clearTimer(boolean notifyCancelled) {
    boolean wasActive = isActive;
    isActive = false;
    if (sleepTimerTask != null) {
      sleepTimerTask.cancel();
      sleepTimerTask = null;
    }
    // Keep the daemon timer alive for reuse (next countdown / exit one-shot);
    // it idles at ~zero cost and dies with the VM.
    if (notifyCancelled && wasActive && callback != null) {
      callback.onTimerCancelled();
    }
    targetTimeMillis = 0;
    timerAction = 0;
    timerDescription = "";
  }

  public void shutdown() {
    clearTimer(false);
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    callback = null;
  }

  public interface SleepTimerCallback {
    void onTimerExpired(int action);

    void onTimerUpdate(String remainingTime);

    void onTimerCancelled();
  }
}
