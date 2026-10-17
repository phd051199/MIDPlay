package midplay.ui.screen;

import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import midplay.store.TasksManager;
import midplay.store.TasksManager.Task;
import midplay.ui.Commands;
import midplay.ui.Navigator;
import midplay.ui.TaskPrompts;
import midplay.util.DateTime;
import midplay.util.Lang;
import midplay.util.Utils;

/** Task list with filtering, search and quick actions. */
public final class TasksScreen extends List implements CommandListener {
  private static final int FILTER_ALL = 0;
  private static final int FILTER_PENDING = 1;
  private static final int FILTER_DONE = 2;
  private static final int FILTER_OVERDUE = 3;

  private final Navigator navigator;
  private final Command newCommand;
  private final Command doneCommand;
  private final Command pinCommand;
  private final Command priorityCommand;
  private final Command deleteCommand;
  private final Command filterCommand;
  private final Command searchCommand;
  private final Command clearSearchCommand;
  private Task[] tasks;
  private int filter = FILTER_ALL;
  private String searchQuery = "";

  public TasksScreen(Navigator navigator) {
    super(Lang.tr("task.title"), List.IMPLICIT);
    this.navigator = navigator;
    newCommand = new Command(Lang.tr("task.new"), Command.SCREEN, 1);
    doneCommand = new Command(Lang.tr("task.toggle_done"), Command.SCREEN, 2);
    pinCommand = new Command(Lang.tr("task.pin"), Command.SCREEN, 3);
    priorityCommand = new Command(Lang.tr("task.priority"), Command.SCREEN, 4);
    deleteCommand = new Command(Lang.tr("task.delete"), Command.SCREEN, 5);
    filterCommand = new Command(Lang.tr("task.filter"), Command.SCREEN, 6);
    searchCommand = new Command(Lang.tr("task.search"), Command.SCREEN, 7);
    clearSearchCommand = new Command(Lang.tr("task.clear_search"), Command.SCREEN, 8);
    addCommand(newCommand);
    addCommand(doneCommand);
    addCommand(pinCommand);
    addCommand(priorityCommand);
    addCommand(deleteCommand);
    addCommand(filterCommand);
    addCommand(searchCommand);
    addCommand(clearSearchCommand);
    addCommand(Commands.back());
    addCommand(Commands.playerNowPlaying());
    setCommandListener(this);
    populateItems();
  }

  public void showNotify() {
    refresh();
  }

  public void commandAction(Command command, Displayable displayable) {
    try {
      if (command == List.SELECT_COMMAND) {
        Task task = selected();
        if (task != null) {
          navigator.forward(new TaskReaderScreen(task, navigator, refreshOnClose()));
        }
      } else if (command == Commands.back()) {
        navigator.back();
      } else if (command == Commands.playerNowPlaying()) {
        midplay.ui.PlayerNavHelper.showNowPlaying(navigator);
      } else if (command == newCommand) {
        navigator.forward(new TaskEditScreen(null, navigator, refreshOnClose()));
      } else if (command == doneCommand) {
        toggleDone();
      } else if (command == pinCommand) {
        togglePin();
      } else if (command == priorityCommand) {
        Task task = selected();
        TaskPrompts.priority(navigator, task, refreshOnClose());
      } else if (command == deleteCommand) {
        deleteSelected();
      } else if (command == filterCommand) {
        promptFilter();
      } else if (command == searchCommand) {
        searchTasks();
      } else if (command == clearSearchCommand) {
        searchQuery = "";
        refresh();
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void populateItems() {
    deleteAll();
    Task[] all = TasksManager.getInstance().getTasks();
    int pending = 0;
    for (int i = 0; i < all.length; i++) {
      if (!all[i].done) {
        pending++;
      }
    }
    Vector visible = new Vector();
    for (int i = 0; i < all.length; i++) {
      if (matches(all[i])) {
        visible.addElement(all[i]);
      }
    }
    tasks = new Task[visible.size()];
    for (int i = 0; i < tasks.length; i++) {
      tasks[i] = (Task) visible.elementAt(i);
      append(label(tasks[i]), null);
    }
    if (tasks.length == 0) {
      append(Lang.tr(searchQuery.length() == 0 && filter == FILTER_ALL ? "task.empty" : "task.no_results"), null);
    }
    setTitle(Lang.tr("task.title") + "  " + pending + "/" + all.length);
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

  private boolean matches(Task task) {
    if (!matchesFilter(task)) {
      return false;
    }
    if (searchQuery.length() == 0) {
      return true;
    }
    return Utils.indexOfIgnoreCase(task.title, searchQuery) >= 0
        || Utils.indexOfIgnoreCase(task.description, searchQuery) >= 0;
  }

  private boolean matchesFilter(Task task) {
    if (filter == FILTER_PENDING) {
      return !task.done;
    }
    if (filter == FILTER_DONE) {
      return task.done;
    }
    if (filter == FILTER_OVERDUE) {
      return DateTime.isOverdue(task.due, task.done);
    }
    return true;
  }

  private void toggleDone() {
    Task task = selected();
    if (task == null) {
      return;
    }
    try {
      TasksManager.getInstance().toggleDone(task.id);
      refresh();
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void togglePin() {
    Task task = selected();
    if (task == null) {
      return;
    }
    try {
      TasksManager.getInstance().togglePinned(task.id);
      refresh();
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  private void searchTasks() {
    final TextBox box = new TextBox(Lang.tr("task.search"), searchQuery, 100, TextField.ANY);
    box.addCommand(Commands.ok());
    box.addCommand(Commands.cancel());
    box.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              String value = box.getString();
              searchQuery = value == null ? "" : value.trim();
              navigator.back();
              refresh();
            } else if (command == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(box);
  }

  private void promptFilter() {
    final Form form = new Form(Lang.tr("task.filter"));
    final ChoiceGroup group = new ChoiceGroup(Lang.tr("task.filter"), Choice.EXCLUSIVE);
    group.append(Lang.tr("task.filter_all"), null);
    group.append(Lang.tr("task.filter_pending"), null);
    group.append(Lang.tr("task.filter_done"), null);
    group.append(Lang.tr("task.filter_overdue"), null);
    group.setSelectedIndex(filter, true);
    form.append(group);
    form.addCommand(Commands.ok());
    form.addCommand(Commands.cancel());
    form.setCommandListener(
        new CommandListener() {
          public void commandAction(Command command, Displayable displayable) {
            if (command == Commands.ok()) {
              filter = group.getSelectedIndex();
              navigator.back();
              refresh();
            } else if (command == Commands.cancel()) {
              navigator.back();
            }
          }
        });
    navigator.forward(form);
  }

  private Task selected() {
    int index = getSelectedIndex();
    return tasks != null && index >= 0 && index < tasks.length ? tasks[index] : null;
  }

  private Runnable refreshOnClose() {
    return new Runnable() {
      public void run() {
        refresh();
      }
    };
  }

  private void deleteSelected() {
    final Task task = selected();
    if (task == null) {
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("task.delete_confirm", task.title),
        new Runnable() {
          public void run() {
            try {
              TasksManager.getInstance().remove(task.id);
              refresh();
              navigator.showAlert(Lang.tr("task.deleted"), AlertType.CONFIRMATION, TasksScreen.this);
            } catch (Exception e) {
              navigator.showAlert(e.toString(), AlertType.ERROR, TasksScreen.this);
            }
          }
        });
  }

  private void refresh() {
    populateItems();
  }
}
