package midplay.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.rms.RecordStoreException;
import midplay.util.Utils;

public final class NotesManager {
  public static final class Note {
    public final int id;
    public final String title;
    public final String text;
    public final boolean pinned;
    public final String customTitle;

    Note(int id, String title, String text, boolean pinned, String customTitle) {
      this.id = id;
      this.title = title;
      this.text = text;
      this.pinned = pinned;
      this.customTitle = customTitle;
    }
  }

  private static NotesManager instance;
  private static final String EXPORT_FILE_NAME = "MIDPlay.vnt";
  private final JsonRecordStore storage = new JsonRecordStore(Configuration.STORAGE_NOTES, 1, "[]");
  private JSONArray entries;

  public static synchronized NotesManager getInstance() {
    if (instance == null) {
      instance = new NotesManager();
    }
    return instance;
  }

  private NotesManager() {}

  public Note[] getNotes() {
    JSONArray data = notes();
    Note[] result = new Note[data.size()];
    for (int i = 0; i < result.length; i++) {
      JSONObject item = data.getObject(i);
      String text = item.getString("text", "");
      result[i] = noteFromJSON(item, i + 1);
    }
    Note[] ordered = new Note[result.length];
    int index = 0;
    for (int i = 0; i < result.length; i++) {
      if (result[i].pinned) ordered[index++] = result[i];
    }
    for (int i = 0; i < result.length; i++) {
      if (!result[i].pinned) ordered[index++] = result[i];
    }
    return ordered;
  }

  public Note getNote(int id) {
    JSONObject item = find(notes(), id);
    if (item == null) {
      return null;
    }
    return noteFromJSON(item, id);
  }

  public Note create(String text) throws RecordStoreException {
    JSONArray data = notes();
    int id = nextId(data);
    JSONObject item = noteObject(id, "", text == null ? "" : text, false);
    data.add(item);
    save(data);
    return noteFromJSON(item, id);
  }

  public void update(int id, String text) throws RecordStoreException {
    JSONArray data = notes();
    JSONArray updated = new JSONArray();
    for (int i = 0; i < data.size(); i++) {
      JSONObject item = data.getObject(i);
      int itemId = item.getInt("id", i + 1);
      String itemText = itemId == id ? (text == null ? "" : text) : item.getString("text", "");
      String customTitle = customTitle(item);
      updated.add(noteObject(itemId, customTitle, itemText, item.getBoolean("pinned", false)));
    }
    save(updated);
  }

  public void rename(int id, String title) throws RecordStoreException {
    updateCustomTitle(id, title);
  }

  public void updateCustomTitle(int id, String title) throws RecordStoreException {
    rewriteNote(id, title == null ? "" : title.trim(), null, null);
  }

  public void togglePinned(int id) throws RecordStoreException {
    JSONObject item = find(notes(), id);
    if (item != null) rewriteNote(id, null, null, new Boolean(!item.getBoolean("pinned", false)));
  }

  public void remove(int id) throws RecordStoreException {
    JSONArray data = notes();
    JSONArray remaining = new JSONArray();
    for (int i = 0; i < data.size(); i++) {
      JSONObject item = data.getObject(i);
      if (item.getInt("id", -1) != id) {
        remaining.add(item);
      }
    }
    save(remaining);
  }

  public String exportToDevice(String directory, Note[] selected, String fileName) throws IOException {
    String name = fileName == null ? "" : fileName.trim();
    while (name.indexOf('/') != -1 || name.indexOf('\\') != -1) {
      int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
      name = name.substring(slash + 1);
    }
    if (name.length() == 0) name = EXPORT_FILE_NAME;
    if (!endsWithIgnoreCase(name, ".vnt")) name += ".vnt";
    String path = directory.endsWith("/") ? directory + name : directory + "/" + name;
    writeFile(path, exportData(selected));
    return path;
  }

  public int importFromDevice(String path) throws Exception {
    String text = readFile(path);
    JSONArray imported = parseImported(path, text);
    JSONArray data = notes();
    int count = 0;
    for (int i = 0; i < imported.size(); i++) {
      JSONObject source = imported.getObject(i);
      JSONObject item = noteObject(nextId(data), "", source.getString("text", ""), false);
      data.add(item);
      count++;
    }
    if (count > 0) {
      save(data);
    }
    return count;
  }

  private JSONArray notes() {
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
    if (raw != null && raw.length() > 0) {
       entries.add(noteObject(1, "", raw, false));
      try {
        save(entries);
      } catch (Exception e) {
      }
    }
    return entries;
  }

  private JSONArray parseImported(String path, String text) throws Exception {
    JSONArray result = new JSONArray();
    if (endsWithIgnoreCase(path, ".vnt")) {
      parseVNotes(text, result);
      if (result.size() > 0) {
        return result;
      }
      try {
        JSONObject root = JSON.getObject(text);
        JSONArray source = root.getArray("notes");
        for (int i = 0; i < source.size(); i++) {
          result.add(source.getObject(i));
        }
        return result;
      } catch (Exception e) {
      }
    }
    result.add(noteObject(0, "", text, false));
    return result;
  }

  private String exportData(Note[] all) {
    if (all == null) all = new Note[0];
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < all.length; i++) {
      output.append("BEGIN:VNOTE\nVERSION:1.1\nBODY;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:");
      output.append(encodeQuotedPrintable(all[i].text));
      output.append("\nDCREATED:").append(createdTimestamp()).append("\nEND:VNOTE\n");
    }
    return output.toString();
  }

  private static void parseVNotes(String text, JSONArray result) {
    int position = 0;
    while (true) {
      int begin = text.indexOf("BEGIN:VNOTE", position);
      if (begin < 0) {
        return;
      }
      int end = text.indexOf("END:VNOTE", begin);
      if (end < 0) {
        return;
      }
      String block = text.substring(begin, end);
      int body = block.indexOf("BODY");
      if (body >= 0) {
        int colon = block.indexOf(':', body);
        if (colon >= 0) {
          int nextField = block.indexOf("\nDCREATED", colon);
          String value = block.substring(colon + 1, nextField < 0 ? block.length() : nextField).trim();
          String noteText =
              block.indexOf("ENCODING=QUOTED-PRINTABLE", body) >= 0
                  ? decodeQuotedPrintable(value)
                  : unescapeVNote(value);
          result.add(noteObject(0, "", noteText, false));
        }
      }
      position = end + 9;
    }
  }

  private static String encodeQuotedPrintable(String text) {
    StringBuffer output = new StringBuffer();
    byte[] bytes = Utils.utf8ToBytes(text);
    for (int i = 0; i < bytes.length; i++) {
      int value = bytes[i] & 0xFF;
      output.append('=').append(hex(value >> 4)).append(hex(value & 0x0F));
    }
    return output.toString();
  }

  private static String decodeQuotedPrintable(String text) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '=' && i + 2 < text.length()) {
        if (text.charAt(i + 1) == '\n') {
          i++;
          continue;
        }
        int high = Character.digit(text.charAt(i + 1), 16);
        int low = Character.digit(text.charAt(i + 2), 16);
        if (high >= 0 && low >= 0) {
          output.write((high << 4) | low);
          i += 2;
          continue;
        }
      }
      output.write((byte) c);
    }
    return Utils.bytesToUtf8(output.toByteArray());
  }

  private static char hex(int value) {
    return "0123456789ABCDEF".charAt(value & 0x0F);
  }

  private static String createdTimestamp() {
    return "19700101T000000";
  }

  private static String unescapeVNote(String text) {
    StringBuffer output = new StringBuffer();
    boolean escaped = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (escaped) {
        output.append(c == 'n' ? '\n' : c);
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else {
        output.append(c);
      }
    }
    if (escaped) output.append('\\');
    return output.toString();
  }

  private static JSONObject noteObject(int id, String title, String text, boolean pinned) {
    JSONObject item = new JSONObject();
    item.put("id", id);
    item.put("customTitle", title == null ? "" : title);
    item.put("text", text == null ? "" : text);
    item.put("pinned", pinned);
    return item;
  }

  private static Note noteFromJSON(JSONObject item, int fallbackId) {
    String text = item.getString("text", "");
    String customTitle = customTitle(item);
    String title = customTitle.length() > 0 ? customTitle : titleFromText(text);
    return new Note(item.getInt("id", fallbackId), title, text, item.getBoolean("pinned", false), customTitle);
  }

  private static String customTitle(JSONObject item) {
    String title = item.getString("customTitle", "");
    if (title.length() > 0) return title;
    title = item.getString("title", "");
    if ("Note".equals(title) || "Note 1".equals(title)) return "";
    return title;
  }

  private void rewriteNote(int id, String customTitle, String text, Boolean pinned) throws RecordStoreException {
    JSONArray old = notes();
    JSONArray updated = new JSONArray();
    for (int i = 0; i < old.size(); i++) {
      JSONObject item = old.getObject(i);
      int currentId = item.getInt("id", i + 1);
      String currentTitle = customTitle(item);
      String currentText = item.getString("text", "");
      boolean currentPinned = item.getBoolean("pinned", false);
      if (currentId == id) {
        if (customTitle != null) currentTitle = customTitle;
        if (text != null) currentText = text;
        if (pinned != null) currentPinned = pinned.booleanValue();
      }
      updated.add(noteObject(currentId, currentTitle, currentText, currentPinned));
    }
    save(updated);
  }

  private static String titleFromText(String text) {
    if (text == null || text.trim().length() == 0) {
      return "Note";
    }
    StringBuffer title = new StringBuffer();
    boolean previousSpace = false;
    for (int i = 0; i < text.length() && title.length() < 20; i++) {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r' || c == '\t') {
        c = ' ';
      }
      if (c == ' ' && previousSpace) {
        continue;
      }
      title.append(c);
      previousSpace = c == ' ';
    }
    String result = title.toString().trim();
    if (text.trim().length() > result.length()) {
      result += "...";
    }
    return result.length() == 0 ? "Note" : result;
  }

  private static int nextId(JSONArray data) {
    int largest = 0;
    for (int i = 0; i < data.size(); i++) {
      largest = Math.max(largest, data.getObject(i).getInt("id", 0));
    }
    return largest + 1;
  }

  private static JSONObject find(JSONArray data, int id) {
    for (int i = 0; i < data.size(); i++) {
      JSONObject item = data.getObject(i);
      if (item.getInt("id", -1) == id) {
        return item;
      }
    }
    return null;
  }

  private void save(JSONArray data) throws RecordStoreException {
    entries = data;
    storage.save(data.toString());
  }

  private static boolean endsWithIgnoreCase(String value, String suffix) {
    return value != null && value.toLowerCase().endsWith(suffix);
  }

  private static String titleFromPath(String path) {
    int slash = path == null ? -1 : path.lastIndexOf('/');
    String name = slash >= 0 ? path.substring(slash + 1) : path;
    int dot = name == null ? -1 : name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

  private static String readFile(String path) throws IOException {
    FileConnection file = null;
    InputStream input = null;
    try {
      file = (FileConnection) Connector.open(path, Connector.READ);
      input = file.openInputStream();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int count;
      while ((count = input.read(buffer, 0, buffer.length)) != -1) {
        output.write(buffer, 0, count);
      }
      return Utils.bytesToUtf8(output.toByteArray());
    } finally {
      Utils.closeQuietly(input);
      close(file);
    }
  }

  private static String writeFile(String path, String text) throws IOException {
    FileConnection file = null;
    OutputStream output = null;
    try {
      file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
      if (!file.exists()) file.create();
      else file.truncate(0);
      output = file.openOutputStream();
      byte[] bytes = Utils.utf8ToBytes(text);
      output.write(bytes, 0, bytes.length);
      return path;
    } finally {
      Utils.closeQuietly(output);
      close(file);
    }
  }

  private static void close(FileConnection file) {
    if (file != null) {
      try {
        file.close();
      } catch (IOException e) {
      }
    }
  }
}
