package midplay.model;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;

public class Base {
  private String key;
  private String name;
  private long id;

  public Base() {}

  public Base(String key, String name) {
    this.key = key;
    this.name = name;
    this.id = System.currentTimeMillis();
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  protected String normalizeDisplayText(String value) {
    return value != null ? value.trim() : "";
  }

  public String buildDisplayTitle(String subtitle) {
    String title = normalizeDisplayText(getName());
    String normalizedSubtitle = normalizeDisplayText(subtitle);
    if (title.length() == 0) {
      return normalizedSubtitle;
    }
    if (normalizedSubtitle.length() == 0) {
      return title;
    }
    return title + "\n" + normalizedSubtitle;
  }

  protected static boolean equalsNullable(String left, String right) {
    if (left == null) {
      return right == null;
    }
    return left.equals(right);
  }

  protected static JSONObject parseObject(String jsonString) {
    if (jsonString == null || jsonString.trim().length() == 0) {
      return null;
    }
    return JSON.getObject(jsonString);
  }
}
