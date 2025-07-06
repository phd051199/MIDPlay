package app.model;

import app.interfaces.JSONAble;
import org.json.me.JSONObject;

class Chat implements JSONAble {
  private String message = "";

  public void setMessage(String _message) {
    this.message = _message;
  }

  public String getMessage() {
    return this.message;
  }

  public JSONObject toJSON() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void fromJSON(String jsonString) {
    try {
      JSONObject json = new JSONObject(jsonString);
      this.setMessage(json.getString("message"));
    } catch (Exception e) {
    }
  }
}
