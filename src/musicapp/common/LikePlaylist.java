package musicapp.common;

import java.io.IOException;
import org.json.me.JSONObject;
import musicapp.network.URLProvider;

public class LikePlaylist {

    private LikeOberserver observer;

    public void setObserver(LikeOberserver _observer) {
        this.observer = _observer;
    }

    private String getLikeResult(String listkey, String username) {
        RestClient client = new RestClient();

        try {
            return client.get(URLProvider.doLike(2, listkey, username));
        } catch (IOException var5) {
            var5.printStackTrace();
            return "";
        }
    }

    public String parseLikeResult(String liskey, String username) {
        String result = this.getLikeResult(liskey, username);
        if (result != null && !"".equals(result)) {
            try {
                JSONObject json = new JSONObject(result);
                return json.getString("result");
            } catch (Exception var6) {
                var6.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    public void doLike(final String liskey, final String username) {
        (new Thread(new Runnable() {
            public void run() {
                String result = LikePlaylist.this.parseLikeResult(liskey, username);
                LikePlaylist.this.observer.likeComplete(result);
            }
        })).start();
    }
}
