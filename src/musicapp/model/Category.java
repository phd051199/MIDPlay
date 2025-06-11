package musicapp.model;

import org.json.me.JSONObject;

public class Category implements JSONAble {

    private String Id;
    private String Name;

    public void setId(String id) {
        this.Id = id;
    }

    public String getId() {
        return this.Id;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public String getName() {
        return this.Name;
    }

    public String toJSON() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void fromJSON(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.setId(json.getString("Key"));
            this.setName(json.getString("Name"));
        } catch (Exception var3) {
            System.out.println(var3.getMessage());
        }

    }
}
