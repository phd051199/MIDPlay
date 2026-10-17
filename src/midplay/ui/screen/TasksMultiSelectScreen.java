package midplay.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import midplay.store.TasksManager;
import midplay.store.TasksManager.Task;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.TaskPrompts;
import midplay.util.DateTime;
import midplay.util.Lang;

public final class TasksMultiSelectScreen extends List implements CommandListener {
  private final Navigator navigator;
  private final Runnable onClose;
  private final Command doneCommand;
  private final Command reopenCommand;
  private final Command priorityCommand;
  private final Command deleteCommand;
  private Task[] tasks;

  public TasksMultiSelectScreen(Navigator navigator, Runnable onClose) {
    super(Lang.tr("task.select_multiple"), List.MULTIPLE);
    this.navigator = navigator;
    this.onClose = onClose;
    doneCommand = new Command(Lang.tr("task.complete"), Command.SCREEN, 1);
    reopenCommand = new Command(Lang.tr("task.reopen"), Command.SCREEN, 2);
    priorityCommand = new Command(Lang.tr("task.priority"), Command.SCREEN, 3);
    deleteCommand = new Command(Lang.tr("task.delete"), Command.SCREEN, 4);
    addCommand(doneCommand);
    addCommand(reopenCommand);
    addCommand(priorityCommand);
    addCommand(deleteCommand);
    addCommand(Commands.cancel());
    setCommandListener(this);
    populate();
  }

  private void populate() {
    deleteAll();
    tasks = TasksManager.getInstance().getTasks();
    for (int i = 0; i < tasks.length; i++) {
      append(label(tasks[i]), null);
    }
  }

  private String label(Task task) {
    StringBuffer text = new StringBuffer();
    text.append(task.done ? "[X] " : "[ ] ");
    if (task.pinned) {
      text.append("[PIN] ");
    }
    if (task.priority == TasksManager.PRIORITY_HIGH) {
      text.append("[!] ");
    } else if (task.priority == TasksManager.PRIORITY_LOW) {
      text.append("[-] ");
    }
    text.append(task.title);
    if (task.due != DateTime.NONE) {
      text.append(' ');
      if (DateTime.isOverdue(task.due, task.done)) {
        text.append("!! ");
      }
      text.append(DateTime.formatShort(task.due));
    }
    return text.toString();
  }

  public void commandAction(Command command, Displayable displayable) {
    try {
      if (command == Commands.cancel()) {
        navigator.back();
        return;
      }
      boolean[] selected = new boolean[size()];
      getSelectedFlags(selected);
      if (command == deleteCommand) {
        deleteSelected(selected);
        return;
      }
      if (command == doneCommand) {
        bulkDone(selected, true);
        return;
      }
      if (command == reopenCommand) {
        bulkDone(selected, false);
        return;
      }
      if (command == priorityCommand) {
        promptBulkPriority(selected);
        return;
      }
      navigator.back();
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void bulkDone(boolean[] selected, boolean done) throws Exception {
    boolean changed = false;
    for (int i = 0; i < tasks.length && i < selected.length; i++) {
      if (selected[i] && tasks[i].done != done) {
        TasksManager.getInstance().toggleDone(tasks[i].id);
        changed = true;
      }
    }
    if (onClose != null) {
      onClose.run();
    }
    navigator.back();
    if (changed) {
      navigator.showAlert(Lang.tr("task.saved"), AlertType.CONFIRMATION);
    }
  }

  private void deleteSelected(boolean[] selected) throws Exception {
    int count = 0;
    for (int i = 0; i < tasks.length && i < selected.length; i++) {
      if (selected[i]) {
        count++;
      }
    }
    if (count == 0) {
      navigator.back();
      return;
    }
    String msg = count == 1 ? tasks[firstSelected(selected)].title : String.valueOf(count);
    navigator.showConfirmationAlert(
        Lang.tr("task.delete_confirm", msg),
        new Runnable() {
          public void run() {
            try {
              boolean[] sel = new boolean[size()];
              getSelectedFlags(sel);
              for (int i = 0; i < tasks.length && i < sel.length; i++) {
                if (sel[i]) {
                  TasksManager.getInstance().remove(tasks[i].id);
                }
              }
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

  private int firstSelected(boolean[] selected) {
    for (int i = 0; i < selected.length; i++) {
      if (selected[i]) {
        return i;
      }
    }
    return 0;
  }

  private void promptBulkPriority(boolean[] selected) {
    final int[] selCopy = new int[countSelected(selected)];
    int idx = 0;
    for (int i = 0; i < tasks.length && i < selected.length; i++) {
      if (selected[i]) {
        selCopy[idx++] = tasks[i].id;
      }
    }
    final Form form = new Form(Lang.tr("task.priority"));
    final ChoiceGroup group = new ChoiceGroup(Lang.tr("task.priority"), Choice.EXCLUSIVE);
    group.append(Lang.tr("task.priority_high"), null);
    group.append(Lang.tr("task.priority_normal"), null);
    group.append(Lang.tr("task.priority_low"), null);
    group.setSelectedIndex(1, true);
    form.append(group);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              try {
                int priority = TaskPrompts.priorityValue(group.getSelectedIndex());
                for (int i = 0; i < selCopy.length; i++) {
                  TasksManager.getInstance().setPriority(selCopy[i], priority);
                }
                if (onClose != null) {
                  onClose.run();
                }
                navigator.back();
                navigator.showAlert(Lang.tr("task.saved"), AlertType.CONFIRMATION);
              } catch (Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            } else {
              navigator.back();
            }
          }
        });
    navigator.forward(form);
  }

  private int countSelected(boolean[] selected) {
    int c = 0;
    for (int i = 0; i < selected.length; i++) {
      if (selected[i]) {
        c++;
      }
    }
    return c;
  }
}
