package model;

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

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
