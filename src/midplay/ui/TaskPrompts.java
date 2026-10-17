package midplay.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import midplay.store.TasksManager;
import midplay.store.TasksManager.Task;
import midplay.util.Lang;

/** Shared quick-edit prompts (rename, priority) used by the task list and detail screens. */
public final class TaskPrompts {
  private TaskPrompts() {}

  public static void rename(final Navigator navigator, final Task task, final Runnable onSaved) {
    if (navigator == null || task == null) {
      return;
    }
    final Form form = new Form(Lang.tr("task.rename_title"));
    final TextField field = new TextField(Lang.tr("task.title_field"), task.title, 80, TextField.ANY);
    form.append(field);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              String value = field.getString();
              value = value == null ? "" : value.trim();
              if (value.length() == 0) {
                navigator.showAlert(Lang.tr("task.title_required"), AlertType.ERROR);
                return;
              }
              try {
                TasksManager.getInstance().rename(task.id, value);
                navigator.back();
                if (onSaved != null) {
                  onSaved.run();
                }
              } catch (Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            } else if (command == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(form);
  }

  public static void priority(final Navigator navigator, final Task task, final Runnable onSaved) {
    if (navigator == null || task == null) {
      return;
    }
    final Form form = new Form(Lang.tr("task.priority"));
    final ChoiceGroup group = new ChoiceGroup(Lang.tr("task.priority"), Choice.EXCLUSIVE);
    group.append(Lang.tr("task.priority_high"), null);
    group.append(Lang.tr("task.priority_normal"), null);
    group.append(Lang.tr("task.priority_low"), null);
    group.setSelectedIndex(priorityIndex(task.priority), true);
    form.append(group);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              try {
                TasksManager.getInstance().setPriority(task.id, priorityValue(group.getSelectedIndex()));
                navigator.back();
                if (onSaved != null) {
                  onSaved.run();
                }
              } catch (Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            } else if (command == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(form);
  }

  /** Maps a priority (high/normal/low) to its index in the priority choice group. */
  public static int priorityIndex(int priority) {
    if (priority == TasksManager.PRIORITY_HIGH) {
      return 0;
    }
    if (priority == TasksManager.PRIORITY_LOW) {
      return 2;
    }
    return 1;
  }

  public static int priorityValue(int index) {
    if (index == 0) {
      return TasksManager.PRIORITY_HIGH;
    }
    if (index == 2) {
      return TasksManager.PRIORITY_LOW;
    }
    return TasksManager.PRIORITY_NORMAL;
  }

  public static String priorityName(int priority) {
    if (priority == TasksManager.PRIORITY_HIGH) {
      return Lang.tr("task.priority_high");
    }
    if (priority == TasksManager.PRIORITY_LOW) {
      return Lang.tr("task.priority_low");
    }
    return Lang.tr("task.priority_normal");
  }
}
