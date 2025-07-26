import java.util.Timer;
import java.util.TimerTask;

public class SleepTimerManager {
  private Timer sleepTimer;
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

  public void startAbsoluteTimer(int targetHour, int targetMinute, int action) {
    cancelTimer();
    long currentTime = System.currentTimeMillis();
    long currentDay = currentTime / (24 * 60 * 60 * 1000L);
    long targetTime =
        currentDay * (24 * 60 * 60 * 1000L)
            + targetHour * 60 * 60 * 1000L
            + targetMinute * 60 * 1000L;
    if (targetTime <= currentTime) {
      targetTime += 24 * 60 * 60 * 1000L;
    }
    targetTimeMillis = targetTime;
    timerAction = action;
    timerDescription = formatTime(targetHour) + ":" + formatTime(targetMinute);
    startTimer();
  }

  private void startTimer() {
    isActive = true;
    sleepTimer = new Timer();
    sleepTimerTask =
        new TimerTask() {
          public void run() {
            long currentTime = System.currentTimeMillis();
            long remainingMillis = targetTimeMillis - currentTime;
            if (remainingMillis <= 0) {
              executeTimerAction();
            } else {
              updateTimerDisplay();
            }
          }
        };
    sleepTimer.scheduleAtFixedRate(sleepTimerTask, 0, 1000);
  }

  private void updateTimerDisplay() {
    if (callback != null && isActive) {
      callback.onTimerUpdate(getRemainingTime());
    }
  }

  private void executeTimerAction() {
    final int action = timerAction;
    cancelTimer();
    if (callback != null) {
      callback.onTimerExpired(action);
    }
    if (action == SleepTimerForm.ACTION_EXIT_APP) {
      Timer exitTimer = new Timer();
      exitTimer.schedule(
          new TimerTask() {
            public void run() {
              MIDPlay.getInstance().notifyDestroyed();
            }
          },
          1000);
    }
  }

  public void cancelTimer() {
    if (sleepTimerTask != null) {
      sleepTimerTask.cancel();
      sleepTimerTask = null;
    }
    if (sleepTimer != null) {
      sleepTimer.cancel();
      sleepTimer = null;
    }
    if (isActive && callback != null) {
      callback.onTimerCancelled();
    }
    isActive = false;
    targetTimeMillis = 0;
    timerAction = 0;
    timerDescription = "";
  }

  public void shutdown() {
    cancelTimer();
    callback = null;
  }

  public interface SleepTimerCallback {
    void onTimerExpired(int action);

    void onTimerUpdate(String remainingTime);

    void onTimerCancelled();
  }
}
