package model;

public class Message {
  public String text;
  public boolean isSent;
  public boolean isClickable;
  public String displayText = "";
  public int height = -1;
  public int clickableY;
  public int clickableHeight;

  public Message(String text, boolean isSent) {
    this.text = text;
    this.isSent = isSent;
    this.isClickable = text.startsWith("[") && (text.endsWith("]") || text.endsWith("]."));
  }
}
