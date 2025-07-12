package app.models;

import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONObject;

public class Category implements JSONAble {

  private String id;
  private String name;
  private Vector subItems = new Vector();

  public void setId(String _id) {
    this.id = _id;
  }

  public String getId() {
    return this.id;
  }

  public void setName(String _name) {
    this.name = _name;
  }

  public String getName() {
    return this.name;
  }

  public Vector getSubItems() {
    return this.subItems;
  }

  public void setSubItems(Vector v) {
    this.subItems = v;
  }

  public void fromJSON(String jsonString) {
    try {
      JSONObject json = new JSONObject(jsonString);
      this.setId(json.getString("Key"));
      this.setName(json.getString("Name"));

      if (json.has("SubItems")) {
        Vector subs = new Vector();
        JSONArray arr = json.getJSONArray("SubItems");
        for (int i = 0; i < arr.length(); i++) {
          JSONObject subObj = arr.getJSONObject(i);
          Category subCate = new Category();
          subCate.setId(subObj.getString("Key"));
          subCate.setName(subObj.getString("Name"));
          subs.addElement(subCate);
        }
        this.setSubItems(subs);
      }
    } catch (Exception var3) {
    }
  }
}
