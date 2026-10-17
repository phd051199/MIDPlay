package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;
import midplay.util.DateTime;

/** Persistent store for the Tasks feature, backed by a JSON array in a record store. */
public final class TasksManager {
  public static final int PRIORITY_HIGH = 1;
  public static final int PRIORITY_NORMAL = 2;
  public static final int PRIORITY_LOW = 3;

  /** Immutable task model. */
  public static final class Task {
    public final int id;
    public final String title;
    public final String description;
    public final boolean done;
    public final int priority;
    public final boolean pinned;
    public final long due;
    public final long createdAt;
    public final long completedAt;

    Task(
        int id,
        String title,
        String description,
        boolean done,
        int priority,
        boolean pinned,
        long due,
        long createdAt,
        long completedAt) {
      this.id = id;
      this.title = title;
      this.description = description;
      this.done = done;
      this.priority = priority;
      this.pinned = pinned;
      this.due = due;
      this.createdAt = createdAt;
      this.completedAt = completedAt;
    }
  }

  private static TasksManager instance;
  private final JsonRecordStore storage = new JsonRecordStore(Configuration.STORAGE_TASKS, 1, "[]");
  private JSONArray entries;

  public static synchronized TasksManager getInstance() {
    if (instance == null) {
      instance = new TasksManager();
    }
    return instance;
  }

  private TasksManager() {}

  /** All tasks, ordered: pending before done, pinned first, then due date, priority and id. */
  public Task[] getTasks() {
    Task[] tasks = load();
    sort(tasks);
    return tasks;
  }

  public Task getTask(int id) {
    Task[] tasks = load();
    for (int i = 0; i < tasks.length; i++) {
      if (tasks[i].id == id) {
        return tasks[i];
      }
    }
    return null;
  }

  public Task create(String title, String description, int priority, long due)
      throws RecordStoreException {
    Task[] tasks = load();
    int id = 1;
    for (int i = 0; i < tasks.length; i++) {
      id = Math.max(id, tasks[i].id + 1);
    }
    Task created =
        new Task(
            id,
            title == null ? "" : title,
            description == null ? "" : description,
            false,
            priority,
            false,
            due,
            DateTime.now(),
            DateTime.NONE);
    Task[] next = new Task[tasks.length + 1];
    System.arraycopy(tasks, 0, next, 0, tasks.length);
    next[tasks.length] = created;
    store(next);
    return created;
  }

  public void update(int id, String title, String description, int priority, long due)
      throws RecordStoreException {
    Task[] tasks = load();
    Task[] next = new Task[tasks.length];
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      if (task.id == id) {
        next[i] =
            new Task(
                task.id,
                title == null ? "" : title,
                description == null ? "" : description,
                task.done,
                priority,
                task.pinned,
                due,
                task.createdAt,
                task.completedAt);
      } else {
        next[i] = task;
      }
    }
    store(next);
  }

  public void rename(int id, String title) throws RecordStoreException {
    Task[] tasks = load();
    Task[] next = new Task[tasks.length];
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      if (task.id == id) {
        next[i] =
            new Task(
                task.id,
                title == null ? "" : title,
                task.description,
                task.done,
                task.priority,
                task.pinned,
                task.due,
                task.createdAt,
                task.completedAt);
      } else {
        next[i] = task;
      }
    }
    store(next);
  }

  public void setPriority(int id, int priority) throws RecordStoreException {
    Task[] tasks = load();
    Task[] next = new Task[tasks.length];
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      if (task.id == id) {
        next[i] =
            new Task(
                task.id,
                task.title,
                task.description,
                task.done,
                priority,
                task.pinned,
                task.due,
                task.createdAt,
                task.completedAt);
      } else {
        next[i] = task;
      }
    }
    store(next);
  }

  public void togglePinned(int id) throws RecordStoreException {
    Task[] tasks = load();
    Task[] next = new Task[tasks.length];
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      if (task.id == id) {
        next[i] =
            new Task(
                task.id,
                task.title,
                task.description,
                task.done,
                task.priority,
                !task.pinned,
                task.due,
                task.createdAt,
                task.completedAt);
      } else {
        next[i] = task;
      }
    }
    store(next);
  }

  public void toggleDone(int id) throws RecordStoreException {
    Task[] tasks = load();
    Task[] next = new Task[tasks.length];
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      if (task.id == id) {
        boolean done = !task.done;
        long completedAt = done ? DateTime.now() : DateTime.NONE;
        next[i] =
            new Task(
                task.id,
                task.title,
                task.description,
                done,
                task.priority,
                task.pinned,
                task.due,
                task.createdAt,
                completedAt);
      } else {
        next[i] = task;
      }
    }
    store(next);
  }

  public void remove(int id) throws RecordStoreException {
    Task[] tasks = load();
    int count = 0;
    for (int i = 0; i < tasks.length; i++) {
      if (tasks[i].id != id) {
        count++;
      }
    }
    Task[] next = new Task[count];
    int index = 0;
    for (int i = 0; i < tasks.length; i++) {
      if (tasks[i].id != id) {
        next[index++] = tasks[i];
      }
    }
    store(next);
  }


  private void store(Task[] tasks) throws RecordStoreException {
    JSONArray data = new JSONArray();
    for (int i = 0; i < tasks.length; i++) {
      data.add(toJSON(tasks[i]));
    }
    save(data);
  }

  private Task[] load() {
    JSONArray data = tasks();
    Task[] result = new Task[data.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = fromJSON(data.getObject(i), i + 1);
    }
    return result;
  }

  private JSONArray tasks() {
    if (entries != null) {
      return entries;
    }
    String raw = storage.load();
    try {
      entries = JSON.getArray(raw);
      return entries;
    } catch (Exception e) {
    }
    entries = new JSONArray();
    return entries;
  }

  private void save(JSONArray data) throws RecordStoreException {
    entries = data;
    storage.save(data.toString());
  }

  private static JSONObject toJSON(Task task) {
    JSONObject item = new JSONObject();
    item.put("id", task.id);
    item.put("title", task.title == null ? "" : task.title);
    item.put("description", task.description == null ? "" : task.description);
    item.put("done", task.done);
    item.put("priority", task.priority);
    item.put("pinned", task.pinned);
    item.put("due", task.due);
    item.put("due", task.due);
    item.put("createdAt", task.createdAt);
    item.put("completedAt", task.completedAt);
    return item;
  }

  private static Task fromJSON(JSONObject item, int fallbackId) {
    int priority = item.getInt("priority", PRIORITY_NORMAL);
    if (priority < PRIORITY_HIGH || priority > PRIORITY_LOW) {
      priority = PRIORITY_NORMAL;
    }
    return new Task(
        item.getInt("id", fallbackId),
        item.getString("title", ""),
        item.getString("description", ""),
        item.getBoolean("done", false),
        priority,
        item.getBoolean("pinned", false),
        item.getLong("due", DateTime.NONE),
        item.getLong("createdAt", DateTime.NONE),
        item.getLong("completedAt", DateTime.NONE));
  }

  private static void sort(Task[] items) {
    for (int i = 0; i < items.length - 1; i++) {
      int best = i;
      for (int j = i + 1; j < items.length; j++) {
        if (compare(items[j], items[best]) < 0) {
          best = j;
        }
      }
      if (best != i) {
        Task swap = items[i];
        items[i] = items[best];
        items[best] = swap;
      }
    }
  }

  private static int compare(Task a, Task b) {
    if (a.done != b.done) {
      return a.done ? 1 : -1;
    }
    if (a.pinned != b.pinned) {
      return a.pinned ? -1 : 1;
    }
    boolean aHasDue = a.due != DateTime.NONE;
    boolean bHasDue = b.due != DateTime.NONE;
    if (aHasDue != bHasDue) {
      return aHasDue ? -1 : 1;
    }
    if (aHasDue && a.due != b.due) {
      return a.due < b.due ? -1 : 1;
    }
    if (a.priority != b.priority) {
      return a.priority < b.priority ? -1 : 1;
    }
    if (a.id != b.id) {
      return a.id < b.id ? -1 : 1;
    }
    return 0;
  }
}
