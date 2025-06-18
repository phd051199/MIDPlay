package app.interfaces;

import org.json.me.JSONObject;

public interface JSONAble {

  JSONObject toJSON();

  void fromJSON(String var1);
}
