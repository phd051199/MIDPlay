package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import midplay.store.TasksManager;
import midplay.store.TasksManager.Task;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.ui.TaskPrompts;
import midplay.util.DateTime;
import midplay.util.Lang;

/** Task detail view with quick actions (done, pin, rename, priority, edit, delete). */
public final class TaskReaderScreen extends BaseForm {
  private final int taskId;
  private final Runnable onClose;
  private final StringItem statusItem;
  private final StringItem priorityItem;
  private final StringItem dueItem;
  private final StringItem descriptionItem;
  private final StringItem createdItem;
  private final Command editCommand;
  private final Command doneCommand;
  private final Command pinCommand;
  private final Command renameCommand;
  private final Command priorityCommand;
  private final Command deleteCommand;

  public TaskReaderScreen(Task task, Navigator navigator, Runnable onClose) {
    super(task.title, navigator);
    taskId = task.id;
    this.onClose = onClose;
    statusItem = new StringItem(null, "");
    priorityItem = new StringItem(Lang.tr("task.priority") + ":", "");
    dueItem = new StringItem(Lang.tr("task.due") + ":", "");
    descriptionItem = new StringItem(Lang.tr("task.description") + ":", "");
    String _createdLbl = Lang.tr("task.created_at");
if (_createdLbl.indexOf(':') >= 0) _createdLbl = _createdLbl.substring(0, _createdLbl.indexOf(':'));
createdItem = new StringItem(_createdLbl + ":", "");
    editCommand = new Command(Lang.tr("task.edit"), Command.SCREEN, 1);
    doneCommand = new Command(Lang.tr("task.toggle_done"), Command.SCREEN, 2);
    pinCommand = new Command(Lang.tr("task.pin"), Command.SCREEN, 3);
    renameCommand = new Command(Lang.tr("task.rename"), Command.SCREEN, 4);
    priorityCommand = new Command(Lang.tr("task.priority"), Command.SCREEN, 5);
    deleteCommand = new Command(Lang.tr("task.delete"), Command.SCREEN, 6);
    append(statusItem);
    append(priorityItem);
    append(dueItem);
    append(descriptionItem);
    append(createdItem);
    addCommand(editCommand);
    addCommand(doneCommand);
    addCommand(pinCommand);
    addCommand(renameCommand);
    addCommand(priorityCommand);
    addCommand(deleteCommand);
    addCommand(Commands.playerNowPlaying());
    render();
  }

  protected void handleCommand(Command command, Displayable displayable) {
    Task task = TasksManager.getInstance().getTask(taskId);
    if (task == null) {
      navigator.back();
      return;
    }
    if (command == editCommand) {
      navigator.forward(new TaskEditScreen(task, navigator, refreshReader()));
    } else if (command == doneCommand) {
      toggleDone(task);
    } else if (command == pinCommand) {
      togglePin(task);
    } else if (command == renameCommand) {
      TaskPrompts.rename(navigator, task, refreshReader());
    } else if (command == priorityCommand) {
      TaskPrompts.priority(navigator, task, refreshReader());
    } else if (command == deleteCommand) {
      confirmDelete();
    } else if (command == Commands.playerNowPlaying()) {
      PlayerNavHelper.showNowPlaying(navigator);
    }
  }

  private void toggleDone(Task task) {
    try {
      TasksManager.getInstance().toggleDone(task.id);
      render();
      if (onClose != null) {
        onClose.run();
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void togglePin(Task task) {
    try {
      TasksManager.getInstance().togglePinned(task.id);
      render();
      if (onClose != null) {
        onClose.run();
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void confirmDelete() {
    final Task task = TasksManager.getInstance().getTask(taskId);
    if (task == null) {
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("task.delete_confirm", task.title),
        new Runnable() {
          public void run() {
            try {
              TasksManager.getInstance().remove(taskId);
              if (onClose != null) {
                onClose.run();
              }
              navigator.back();
              navigator.showAlert(Lang.tr("task.deleted"), AlertType.CONFIRMATION);
            } catch (Exception e) {
              navigator.showAlert(e.toString(), AlertType.ERROR);
            }
          }
        });
  }

  private void render() {
    Task task = TasksManager.getInstance().getTask(taskId);
    if (task == null) {
      return;
    }
    setTitle((task.pinned ? "[PIN] " : "") + task.title);
    if (task.done) {
      String completed =
          task.completedAt != DateTime.NONE
              ? Lang.tr("task.completed_at", DateTime.format(task.completedAt))
              : Lang.tr("task.status_done");
      statusItem.setText(Lang.tr("task.status_done") + "  " + completed);
    } else {
      statusItem.setText(Lang.tr("task.status_pending"));
    }
    priorityItem.setText(TaskPrompts.priorityName(task.priority));
    if (task.due != DateTime.NONE) {
      StringBuffer due = new StringBuffer();
      due.append(Lang.tr("task.due")).append(": ").append(DateTime.format(task.due));
      due.append(" (").append(DateTime.formatRelative(task.due)).append(')');
      if (DateTime.isOverdue(task.due, task.done)) {
        due.append(" - ").append(Lang.tr("task.overdue"));
      }
      dueItem.setText(due.toString());
    } else {
      dueItem.setText(Lang.tr("task.due_none"));
    }

    String description = task.description;
    if (description == null || description.trim().length() == 0) {
      descriptionItem.setText(Lang.tr("task.description_empty"));
    } else {
      descriptionItem.setText(description);
    }
    createdItem.setText(
        task.createdAt != DateTime.NONE
            ? DateTime.format(task.createdAt)
            : "");
  }

  private Runnable refreshReader() {
    return new Runnable() {
      public void run() {
        render();
        if (onClose != null) {
          onClose.run();
        }
      }
    };
  }
}