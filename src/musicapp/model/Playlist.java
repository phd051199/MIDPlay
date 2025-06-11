package musicapp.model;

import org.json.me.JSONObject;
import musicapp.utils.Utils;

public class Playlist implements JSONAble {

    private String id = "";
    private String name = "";
    private String desc = "";
    private String imageUrl = "";

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

    public void setDesc(String _desc) {
        this.desc = _desc;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setImageUrl(String _imageUrl) {
        this.imageUrl = _imageUrl;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String toJSON() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void fromJSON(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.setId(json.getString("ListKey"));
            if (!json.getString("Name").equals("")) {
                String listName = Utils.convertString(json.getString("Name"));
                this.setName(listName);
            } else {
                this.setName("MusicApp");
            }

            this.setImageUrl(json.getString("Image"));
        } catch (Exception var4) {
            System.out.println(var4.getMessage());
        }

    }
}
