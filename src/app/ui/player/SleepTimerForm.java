package app.ui.player;

import app.MIDPlay;
import app.ui.MainObserver;
import app.utils.I18N;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class SleepTimerForm extends Form implements CommandListener {

  public static final int MODE_COUNTDOWN = 0;
  public static final int MODE_ABSOLUTE = 1;

  public static final int ACTION_STOP_PLAYBACK = 0;
  public static final int ACTION_EXIT_APP = 1;

  private Command setTimerCommand;
  private Command cancelCommand;
  private Command backCommand;
  private Command switchModeCommand;

  private final ChoiceGroup timerActionChoice;
  private final TextField countdownMinutesField;
  private final TextField absoluteHoursField;
  private final TextField absoluteMinutesField;
  private final StringItem modeHintItem;

  private int currentMode = MODE_COUNTDOWN;

  private final MainObserver observer;
  private final SleepTimerCallback callback;

  public SleepTimerForm(MainObserver observer, SleepTimerCallback callback) {
    super(I18N.tr("sleep_timer_title"));
    this.observer = observer;
    this.callback = callback;

    String[] timerActions = {I18N.tr("action_stop_playback"), I18N.tr("action_exit_app")};
    this.timerActionChoice =
        new ChoiceGroup(I18N.tr("timer_action"), ChoiceGroup.EXCLUSIVE, timerActions, null);
    this.timerActionChoice.setSelectedIndex(ACTION_STOP_PLAYBACK, true);

    this.countdownMinutesField = new TextField(I18N.tr("minutes"), "30", 3, TextField.NUMERIC);
    this.absoluteHoursField = new TextField(I18N.tr("hours"), "23", 2, TextField.NUMERIC);
    this.absoluteMinutesField = new TextField(I18N.tr("minutes"), "00", 2, TextField.NUMERIC);

    this.modeHintItem = new StringItem("", "");

    initializeCommands();
    setupForm();
  }

  private void setupForm() {
    append(timerActionChoice);
    updateModeDisplay();
  }

  private void initializeCommands() {
    this.setTimerCommand = new Command(I18N.tr("set_timer"), Command.OK, 1);
    this.cancelCommand = new Command(I18N.tr("cancel"), Command.CANCEL, 2);
    this.backCommand = new Command(I18N.tr("back"), Command.BACK, 3);

    addCommand(setTimerCommand);
    addCommand(cancelCommand);
    addCommand(backCommand);
    updateSwitchModeCommand();
    setCommandListener(this);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == setTimerCommand) {
      handleSetTimer();
    } else if (c == cancelCommand || c == backCommand) {
      handleCancel();
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
    String nextModeText =
        (currentMode == MODE_COUNTDOWN) ? I18N.tr("absolute_mode") : I18N.tr("countdown_mode");
    switchModeCommand = new Command(nextModeText, Command.SCREEN, 4);
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
      showAlert(I18N.tr("invalid_time"), AlertType.ERROR);
    }
  }

  private void handleCountdownMode(int action) {
    String minutesStr = countdownMinutesField.getString();

    if (!isValidInput(minutesStr)) {
      showAlert(I18N.tr("invalid_time"), AlertType.ERROR);
      return;
    }

    try {
      int minutes = parseInteger(minutesStr.trim());
      if (minutes < 1 || minutes > 999) {
        showAlert(I18N.tr("duration_format_hint"), AlertType.ERROR);
        return;
      }

      if (callback != null) {
        callback.onTimerSet(MODE_COUNTDOWN, minutes, 0, 0, action);
      }
    } catch (NumberFormatException e) {
      showAlert(I18N.tr("invalid_time"), AlertType.ERROR);
    } catch (Exception e) {
      showAlert(I18N.tr("error"), AlertType.ERROR);
    }
  }

  private void handleAbsoluteMode(int action) {
    String hoursStr = absoluteHoursField.getString();
    String minutesStr = absoluteMinutesField.getString();

    if (!isValidInput(hoursStr) || !isValidInput(minutesStr)) {
      showAlert(I18N.tr("invalid_time"), AlertType.ERROR);
      return;
    }

    try {
      int hours = parseInteger(hoursStr.trim());
      int minutes = parseInteger(minutesStr.trim());

      if (hours < 0 || hours > 23) {
        showAlert(I18N.tr("time_format_hint") + " (Hours: 0-23)", AlertType.ERROR);
        return;
      }

      if (minutes < 0 || minutes > 59) {
        showAlert(I18N.tr("time_format_hint") + " (Minutes: 0-59)", AlertType.ERROR);
        return;
      }

      if (callback != null) {
        callback.onTimerSet(MODE_ABSOLUTE, 0, hours, minutes, action);
      }
    } catch (NumberFormatException e) {
      showAlert(I18N.tr("invalid_time"), AlertType.ERROR);
    } catch (Exception e) {
      showAlert(I18N.tr("error"), AlertType.ERROR);
    }
  }

  private void showExitConfirmation(final int action) {
    Alert confirmAlert = new Alert(null, I18N.tr("confirm_exit_timer"), null, AlertType.INFO);
    confirmAlert.setTimeout(Alert.FOREVER);

    Command yesCommand = new Command(I18N.tr("yes"), Command.OK, 1);
    Command noCommand = new Command(I18N.tr("no"), Command.CANCEL, 2);

    confirmAlert.addCommand(yesCommand);
    confirmAlert.addCommand(noCommand);

    confirmAlert.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            MIDPlay.getInstance().getDisplay().setCurrent(SleepTimerForm.this);
            if (c.getCommandType() == Command.OK) {
              proceedWithTimerSet(action);
            }
          }
        });

    MIDPlay.getInstance().getDisplay().setCurrent(confirmAlert, this);
  }

  private void proceedWithTimerSet(int action) {
    int mode = currentMode;

    if (mode == MODE_COUNTDOWN) {
      handleCountdownMode(action);
    } else {
      handleAbsoluteMode(action);
    }
  }

  private void handleCancel() {
    observer.goBack();
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

  private void showAlert(String message, AlertType type) {
    Alert alert = new Alert(null, message, null, type);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  public void updateModeDisplay() {

    int itemsToRemove = size() - 1;
    for (int i = 0; i < itemsToRemove; i++) {
      delete(1);
    }

    if (currentMode == MODE_COUNTDOWN) {
      modeHintItem.setText(I18N.tr("duration_format_hint"));
      append(modeHintItem);
      append(countdownMinutesField);
    } else {
      modeHintItem.setText(I18N.tr("time_format_hint"));
      append(modeHintItem);
      append(absoluteHoursField);
      append(absoluteMinutesField);
    }
  }

  public interface SleepTimerCallback {
    void onTimerSet(int mode, int durationMinutes, int targetHour, int targetMinute, int action);
  }
}
