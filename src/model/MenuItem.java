package model;

public class MenuItem {
  public String key;
  public int order;
  public boolean enabled;

  public MenuItem(String key, int order, boolean enabled) {
    this.key = key;
    this.order = order;
    this.enabled = enabled;
  }
}
