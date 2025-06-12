package app.model;

import org.json.me.JSONObject;
import app.common.Common;

public class Song implements JSONAble {

    private String songId = "";
    private String songName = "";
    private String artistName = "";
    private String streamUrl = "";
    private String kbit = "";
    private String nickUser;
    private String genreID = "";
    private String genreName = "";
    private String listened = "0";
    private String liked = "0";
    private int downloadStatus = -1;
    private String filePath = "";
    private int duration;

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getSongId() {
        return this.songId;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongName() {
        return this.songName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getArtistName() {
        return this.artistName;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = Common.replace(streamUrl, " ", "%20");
        this.streamUrl = Common.replace(this.streamUrl, "[", "%5B");
        this.streamUrl = Common.replace(this.streamUrl, "]", "%5D");
    }

    public void setStreamUrlNotReplaceSpecialChar(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getStreamUrl() {
        return this.streamUrl;
    }

    public void setKbit(String kbit) {
        this.kbit = kbit;
    }

    public String getKbit() {
        return this.kbit;
    }

    public void setDownloadStatus(int status) {
        this.downloadStatus = status;
    }

    public int getDownloadStatus() {
        return this.downloadStatus;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setGenreID(String genreID) {
        this.genreID = genreID;
    }

    public String getGenreID() {
        return this.genreID;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public String getGenreName() {
        return this.genreName;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String createFileName() {
        String result = "New Song";
        if (this.songName != null && this.songName.length() > 0) {
            result = this.songName;
        }

        if (this.artistName != null && this.artistName.length() > 0) {
            result = result + " - " + this.artistName;
        }

        return result;
    }

    public String toJSON() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void fromJSON(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.setSongName(json.getString("Name"));
            this.setStreamUrl(json.getString("Url"));
            this.setArtistName(json.getString("Singer"));
            this.setDuration(json.getInt("Duration"));
        } catch (Exception var3) {
        }

    }
}
