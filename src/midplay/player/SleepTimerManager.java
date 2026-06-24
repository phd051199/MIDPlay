package midplay.player;

import java.util.Timer;
import java.util.TimerTask;
import midplay.MIDPlay;
import midplay.ui.screen.SleepTimerScreen;
import midplay.util.Utils;

public class SleepTimerManager {
  private Timer timer;
  private TimerTask sleepTimerTask;
  private TimerTask pendingExitTask;
  private SleepTimerCallback callback;
  private boolean isActive = false;
  private long targetTimeMillis;
  private int timerAction;

  public SleepTimerManager() {}

  public void setCallback(SleepTimerCallback callback) {
    this.callback = callback;
  }

  public boolean isActive() {
    return isActive;
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
    return Utils.formatClock(remainingSeconds, remainingSeconds >= 3600);
  }

  public void startCountdownTimer(int durationMinutes, int action) {
    cancelTimer();
    long durationMillis = durationMinutes * 60L * 1000L;
    targetTimeMillis = System.currentTimeMillis() + durationMillis;
    timerAction = action;
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
    if (action == SleepTimerScreen.ACTION_EXIT_APP) {
      pendingExitTask =
          new TimerTask() {
            public void run() {
              try {
                MIDPlay.getInstance().notifyDestroyed();
              } catch (Throwable t) {
              }
            }
          };
      timer().schedule(pendingExitTask, 1000);
    }
  }

  public void cancelTimer() {
    clearTimer(true);
  }

  public void shutdown() {
    clearTimer(false);
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  private void clearTimer(boolean notifyCancelled) {
    boolean wasActive = isActive;
    isActive = false;
    if (sleepTimerTask != null) {
      sleepTimerTask.cancel();
      sleepTimerTask = null;
    }
    if (pendingExitTask != null) {
      pendingExitTask.cancel();
      pendingExitTask = null;
    }
    if (notifyCancelled && wasActive && callback != null) {
      callback.onTimerCancelled();
    }
    targetTimeMillis = 0;
    timerAction = 0;
  }

  public interface SleepTimerCallback {
    void onTimerExpired(int action);

    void onTimerUpdate(String remainingTime);

    void onTimerCancelled();
  }
}
