import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class SleepTimerForm extends BaseForm {
  public static final int MODE_COUNTDOWN = 0;
  public static final int MODE_ABSOLUTE = 1;
  public static final int ACTION_STOP_PLAYBACK = 0;
  public static final int ACTION_EXIT_APP = 1;
  private Command switchModeCommand;
  private final ChoiceGroup timerActionChoice;
  private final TextField countdownMinutesField;
  private final TextField absoluteHoursField;
  private final TextField absoluteMinutesField;
  private final StringItem modeHintItem;
  private int currentMode = MODE_COUNTDOWN;
  private final SleepTimerCallback callback;

  public SleepTimerForm(Navigator navigator, SleepTimerCallback callback) {
    super(Lang.tr("timer.sleep_timer"), navigator);
    this.callback = callback;
    String[] timerActions = {
      Lang.tr("timer.actions.stop_playback"), Lang.tr("timer.actions.exit_app")
    };
    this.timerActionChoice =
        new ChoiceGroup(Lang.tr("timer.action"), ChoiceGroup.EXCLUSIVE, timerActions, null);
    this.timerActionChoice.setSelectedIndex(ACTION_STOP_PLAYBACK, true);
    this.countdownMinutesField = new TextField(Lang.tr("time.minutes"), "30", 3, TextField.NUMERIC);
    this.absoluteHoursField = new TextField(Lang.tr("time.hours"), "23", 2, TextField.NUMERIC);
    this.absoluteMinutesField = new TextField(Lang.tr("time.minutes"), "00", 2, TextField.NUMERIC);
    this.modeHintItem = new StringItem("", "");
    setupCommands();
    setupForm();
  }

  private void setupForm() {
    append(timerActionChoice);
    updateModeDisplay();
  }

  private void setupCommands() {
    addCommand(Commands.timerSet());
    updateSwitchModeCommand();
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.timerSet()) {
      handleSetTimer();
    } else if (c == switchModeCommand) {
      handleModeSwitch();
    }
  }

  private void handleModeSwitch() {
    currentMode = (currentMode == MODE_COUNTDOWN) ? MODE_ABSOLUTE : MODE_COUNTDOWN;
    clearInputFields();
    updateModeDisplay();
    updateSwitchModeCommand();
  }

  private void updateSwitchModeCommand() {
    if (switchModeCommand != null) {
      removeCommand(switchModeCommand);
    }
    switchModeCommand =
        (currentMode == MODE_COUNTDOWN)
            ? Commands.timerSwitchToAbsolute()
            : Commands.timerSwitchToCountdown();
    addCommand(switchModeCommand);
  }

  private void clearInputFields() {
    if (currentMode == MODE_COUNTDOWN) {
      countdownMinutesField.setString("30");
    } else {
      absoluteHoursField.setString("23");
      absoluteMinutesField.setString("00");
    }
  }

  private void handleSetTimer() {
    try {
      int action = timerActionChoice.getSelectedIndex();
      if (action == ACTION_EXIT_APP) {
        showExitConfirmation(action);
      } else {
        proceedWithTimerSet(action);
      }
    } catch (Exception e) {
      navigator.showAlert(Lang.tr("time.error.invalid_format"), AlertType.ERROR);
    }
  }

  private void handleCountdownMode(int action) {
    String minutesStr = countdownMinutesField.getString();
    if (!isValidInput(minutesStr)) {
      navigator.showAlert(Lang.tr("time.error.invalid_format"), AlertType.ERROR);
      return;
    }
    try {
      int minutes = parseInteger(minutesStr.trim());
      if (minutes < 1 || minutes > 999) {
        navigator.showAlert(Lang.tr("time.error.invalid_duration"), AlertType.ERROR);
        return;
      }
      if (callback != null) {
        callback.onTimerSet(MODE_COUNTDOWN, minutes, 0, 0, action);
      }
    } catch (NumberFormatException e) {
      navigator.showAlert(Lang.tr("time.error.invalid_format"), AlertType.ERROR);
    }
  }

  private void handleAbsoluteMode(int action) {
    String hoursStr = absoluteHoursField.getString();
    String minutesStr = absoluteMinutesField.getString();
    if (!isValidInput(hoursStr) || !isValidInput(minutesStr)) {
      navigator.showAlert(Lang.tr("time.error.invalid_format"), AlertType.ERROR);
      return;
    }
    try {
      int hours = parseInteger(hoursStr.trim());
      int minutes = parseInteger(minutesStr.trim());
      if (hours < 0 || hours > 23) {
        navigator.showAlert(Lang.tr("time.error.invalid_hour"), AlertType.ERROR);
        return;
      }
      if (minutes < 0 || minutes > 59) {
        navigator.showAlert(Lang.tr("time.error.invalid_minute"), AlertType.ERROR);
        return;
      }
      if (callback != null) {
        callback.onTimerSet(MODE_ABSOLUTE, 0, hours, minutes, action);
      }
    } catch (NumberFormatException e) {
      navigator.showAlert(Lang.tr("time.error.invalid_format"), AlertType.ERROR);
    }
  }

  private void showExitConfirmation(final int action) {
    navigator.showConfirmationAlert(
        Lang.tr("timer.confirm.exit"),
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.ok()) {
              proceedWithTimerSet(action);
            } else if (c == Commands.cancel()) {
              navigator.dismissAlert();
            }
          }
        });
  }

  private void proceedWithTimerSet(int action) {
    if (currentMode == MODE_COUNTDOWN) {
      handleCountdownMode(action);
    } else {
      handleAbsoluteMode(action);
    }
  }

  private boolean isValidInput(String input) {
    if (input == null || input.trim().length() == 0) {
      return false;
    }
    String trimmed = input.trim();
    if (trimmed.length() > 3) {
      return false;
    }
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  private int parseInteger(String input) throws NumberFormatException {
    if (input == null || input.trim().length() == 0) {
      throw new NumberFormatException("Empty input");
    }
    String trimmed = input.trim();
    int result = 0;
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < '0' || c > '9') {
        throw new NumberFormatException("Invalid character: " + c);
      }
      result = result * 10 + (c - '0');
      if (result > 9999) {
        throw new NumberFormatException("Number too large");
      }
    }
    return result;
  }

  public void updateModeDisplay() {
    int itemsToRemove = size() - 1;
    for (int i = 0; i < itemsToRemove; i++) {
      delete(1);
    }
    if (currentMode == MODE_COUNTDOWN) {
      modeHintItem.setText(Lang.tr("time.input.minutes"));
      append(modeHintItem);
      append(countdownMinutesField);
    } else {
      modeHintItem.setText(Lang.tr("time.input.time"));
      append(modeHintItem);
      append(absoluteHoursField);
      append(absoluteMinutesField);
    }
  }

  public interface SleepTimerCallback {
    void onTimerSet(int mode, int durationMinutes, int targetHour, int targetMinute, int action);
  }
}
