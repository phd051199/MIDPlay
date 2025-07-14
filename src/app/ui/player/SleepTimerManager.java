package app.ui.player;

import app.MIDPlay;
import app.core.threading.ThreadManagerIntegration;
import app.utils.I18N;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SleepTimerManager {

  private static SleepTimerManager instance;

  private Timer sleepTimer;
  private TimerTask sleepTimerTask;
  private SleepTimerCallback callback;
  private boolean isActive = false;
  private long targetTimeMillis;
  private int timerAction;
  private String timerDescription;

  public interface SleepTimerCallback {
    void onTimerExpired(int action);

    void onTimerUpdate(String remainingTime);

    void onTimerCancelled();
  }

  private SleepTimerManager() {}

  public static synchronized SleepTimerManager getInstance() {
    if (instance == null) {
      instance = new SleepTimerManager();
    }
    return instance;
  }

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
    timerDescription = durationMinutes + " " + I18N.tr("minutes");

    startTimer();
  }

  public void startAbsoluteTimer(int targetHour, int targetMinute, int action) {
    cancelTimer();

    Calendar now = Calendar.getInstance();
    Calendar target = Calendar.getInstance();
    target.set(Calendar.HOUR_OF_DAY, targetHour);
    target.set(Calendar.MINUTE, targetMinute);
    target.set(Calendar.SECOND, 0);
    target.set(Calendar.MILLISECOND, 0);

    if (target.getTime().getTime() <= now.getTime().getTime()) {
      long dayInMillis = 24 * 60 * 60 * 1000L;
      target.setTime(new Date(target.getTime().getTime() + dayInMillis));
    }

    targetTimeMillis = target.getTime().getTime();
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
      final String remaining = getRemainingTime();
      ThreadManagerIntegration.executeUITask(
          new Runnable() {
            public void run() {
              if (callback != null && isActive) {
                callback.onTimerUpdate(remaining);
              }
            }
          },
          "SleepTimerUpdate");
    }
  }

  private void executeTimerAction() {
    final int action = timerAction;

    ThreadManagerIntegration.executeUITask(
        new Runnable() {
          public void run() {
            try {
              if (callback != null) {
                callback.onTimerExpired(action);
              }

              if (action == SleepTimerForm.ACTION_EXIT_APP) {
                ThreadManagerIntegration.scheduleDelayedTask(
                    new Runnable() {
                      public void run() {
                        MIDPlay.getInstance().exit();
                      }
                    },
                    "SleepTimerExit",
                    1000);
              }
            } catch (Exception e) {
            }
          }
        },
        "SleepTimerAction");

    cancelTimer();
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
      ThreadManagerIntegration.executeUITask(
          new Runnable() {
            public void run() {
              callback.onTimerCancelled();
            }
          },
          "SleepTimerCancel");
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
}
