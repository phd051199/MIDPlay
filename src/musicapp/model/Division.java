package musicapp.model;

public class Division {

    private String id = "";
    private String chargeType = "";
    private String chargeKey = "";
    private String price = "";
    private String duration = "";

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getChargeType() {
        return this.chargeType;
    }

    public void setChargeKey(String chargeKey) {
        this.chargeKey = chargeKey;
    }

    public String getChargeKey() {
        return this.chargeKey;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPrice() {
        return this.price;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDuration() {
        return this.duration;
    }
}
