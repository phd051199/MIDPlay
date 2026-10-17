package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.DateField;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import midplay.store.TasksManager;
import midplay.store.TasksManager.Task;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.ui.TaskPrompts;
import midplay.util.DateTime;
import midplay.util.Lang;

/** Create/edit form for a task: title, description, priority, due date and reminder. */
public final class TaskEditScreen extends BaseForm {

  private int taskId;
  private final Runnable onClose;
  private final TextField titleField;
  private final ChoiceGroup priorityGroup;
  private final DateField dueField;
  private final Command descriptionCommand;
  private String description;

  public TaskEditScreen(Task task, Navigator navigator, Runnable onClose) {
    super(task == null ? Lang.tr("task.create") : Lang.tr("task.edit"), navigator);
    this.onClose = onClose;
    taskId = task == null ? -1 : task.id;
    description = task == null || task.description == null ? "" : task.description;

    titleField =
        new TextField(Lang.tr("task.title_field"), task == null ? "" : task.title, 80, TextField.ANY);

    priorityGroup = new ChoiceGroup(Lang.tr("task.priority"), Choice.EXCLUSIVE);
    priorityGroup.append(Lang.tr("task.priority_high"), null);
    priorityGroup.append(Lang.tr("task.priority_normal"), null);
    priorityGroup.append(Lang.tr("task.priority_low"), null);
    priorityGroup.setSelectedIndex(
        TaskPrompts.priorityIndex(task == null ? TasksManager.PRIORITY_NORMAL : task.priority), true);

    dueField = new DateField(Lang.tr("task.due"), javax.microedition.lcdui.DateField.DATE_TIME);

    descriptionCommand = new Command(Lang.tr("task.description"), Command.SCREEN, 3);

    append(titleField);
    append(priorityGroup);
    append(dueField);

    addCommand(Commands.formSave());
    addCommand(descriptionCommand);
    addCommand(Commands.playerNowPlaying());

    if (task != null) {
      applyTask(task);
    }
  }

  protected void handleCommand(Command command, Displayable displayable) {
    if (command == Commands.formSave()) {
      save();
    } else if (command == descriptionCommand) {
      editDescription();
    } else if (command == Commands.playerNowPlaying()) {
      PlayerNavHelper.showNowPlaying(navigator);
    }
  }

  private void applyTask(Task task) {
    if (task.due != DateTime.NONE) {
      dueField.setDate(new java.util.Date(task.due));
    }
  }


  private void editDescription() {
    final TextBox box =
        new TextBox(Lang.tr("task.description"), description == null ? "" : description, 1000, TextField.ANY);
    box.addCommand(Commands.ok());
    box.addCommand(Commands.cancel());
    box.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              description = box.getString();
              navigator.back();
            } else if (command == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(box);
  }

  private void save() {
    String title = titleField.getString();
    title = title == null ? "" : title.trim();
    if (title.length() == 0) {
      navigator.showAlert(Lang.tr("task.title_required"), AlertType.ERROR);
      return;
    }

    java.util.Date dueDate = dueField.getDate();
    long due = dueDate == null ? DateTime.NONE : dueDate.getTime();
    int priority = TaskPrompts.priorityValue(priorityGroup.getSelectedIndex());
    try {
      if (taskId < 0) {
        TasksManager.getInstance().create(title, description, priority, due);
      } else {
        TasksManager.getInstance().update(taskId, title, description, priority, due);
      }
      if (onClose != null) {
        onClose.run();
      }
      if (taskId < 0) {
        navigator.back();
      } else {
        try {
          TasksManager.Task updated = TasksManager.getInstance().getTask(taskId);
          if (updated != null) {
            navigator.forward(new TaskReaderScreen(updated, navigator, new Runnable() {
              public void run() {
                if (onClose != null) onClose.run();
              }
            }));
          } else {
            navigator.back();
          }
        } catch (Exception e) {
          navigator.back();
        }
      }
      navigator.showAlert(Lang.tr("task.saved"), AlertType.CONFIRMATION);
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

}
