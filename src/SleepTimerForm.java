import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;

public class SleepTimerForm extends BaseForm {
  public static final int ACTION_STOP_PLAYBACK = 0;
  public static final int ACTION_EXIT_APP = 1;
  private final ChoiceGroup timerActionChoice;
  private final TextField countdownMinutesField;
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
    setupCommands();
    setupForm();
  }

  private void setupForm() {
    append(timerActionChoice);
    append(countdownMinutesField);
  }

  private void setupCommands() {
    addCommand(Commands.timerSet());
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.timerSet()) {
      handleSetTimer();
    }
  }

  private void handleSetTimer() {
    try {
      int action = timerActionChoice.getSelectedIndex();
      if (action == ACTION_EXIT_APP) {
        showExitConfirmation(action);
      } else {
        handleTimerSet(action);
      }
    } catch (Exception e) {
      navigator.showAlert(Lang.tr("time.error.invalid_format"), AlertType.ERROR);
    }
  }

  private void handleTimerSet(int action) {
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
        callback.onTimerSet(minutes, action);
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
              handleTimerSet(action);
            } else if (c == Commands.cancel()) {
              navigator.dismissAlert();
            }
          }
        });
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

  public interface SleepTimerCallback {
    void onTimerSet(int durationMinutes, int action);
  }
}
