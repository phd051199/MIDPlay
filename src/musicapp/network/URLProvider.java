package musicapp.network;

import musicapp.constants.APIConstants;
import musicapp.utils.TextUtil;

public class URLProvider {

    public static String getTopHotPlaylist(int pageIndex, int pageSize) {
        String client = "http://api.m.nhaccuatui.com/v4/api/playlist?";
        if (client != null) {

            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=getHotPlaylist";
                client = client + "&pageindex=" + pageIndex;
                client = client + "&pagesize=" + pageSize;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var6) {
                System.out.println(var6.getMessage());
            }
        }

        return null;
    }

    public static String getTopNewsPlaylist(int pageIndex, int pageSize) {
        String client = "http://api.m.nhaccuatui.com/v4/api/playlist?";
        if (client != null) {

            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=getNewPlaylist";
                client = client + "&pageindex=" + pageIndex;
                client = client + "&pagesize=" + pageSize;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var6) {
                System.out.println(var6.getMessage());
            }
        }

        return null;
    }

    public static String getSongByPlaylist(String listKey, String userName) {
        String client = "http://api-gateway.dph.workers.dev/nct/tracks?";
        if (client != null) {
            try {
                client = client + "&listkey=" + listKey;
                client = client + "&username=" + userName;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var6) {
                System.out.println(var6.getMessage());
            }
        }

        return null;
    }

    public static String getCategory(int type) {
        String client = "http://api.m.nhaccuatui.com/v4/api/Genre?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=getCategory";
                client = client + "&type=" + type;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var5) {
                System.out.println(var5.getMessage());
            }
        }

        return null;
    }

    public static String newVersion() {
        String client = "http://api.m.nhaccuatui.com/v4/api/user?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=reportUserInfo";
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                System.out.println(client);
                return client;
            } catch (Exception var4) {
                System.out.println(var4.getMessage());
            }
        }

        return null;
    }

    public static String login(String user, String pass) {
        String client = "http://api.m.nhaccuatui.com/v4/api/user?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=login";
                client = client + "&username=" + user;
                client = client + "&password=" + TextUtil.urlEncodeUTF8(pass);
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var6) {
                System.out.println(var6.getMessage());
            }
        }

        return null;
    }

    public static String getSearchData(int type, String keyword, String genreKey, int pageIndex, int pageSize) {
        String client = "http://api-gateway.dph.workers.dev/nct/search?";
        if (client != null) {
            try {
                client = client + "q=" + TextUtil.urlEncodeUTF8(keyword);
                client = client + "&page=" + pageIndex;
                client = client + "&type=" + type;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var9) {
                System.out.println(var9.getMessage());
            }
        }

        return null;
    }

    public static String getSongLiked(String username, int pageindex, int pagesize) {
        String client = "http://api.m.nhaccuatui.com/v4/api/song?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=getSongLiked";
                client = client + "&username=" + username;
                client = client + "&pageindex=" + pageindex;
                client = client + "&pagesize=" + pagesize;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var7) {
                System.out.println(var7.getMessage());
            }
        }

        return null;
    }

    public static String getMySong(String username, int pageindex, int pagesize) {
        String client = "http://api.m.nhaccuatui.com/v4/api/song?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=getMySong";
                client = client + "&username=" + username;
                client = client + "&pageindex=" + pageindex;
                client = client + "&pagesize=" + pagesize;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var7) {
                System.out.println(var7.getMessage());
            }
        }

        return null;
    }

    public static String getPlaylistLiked(String username, int pageindex, int pagesize) {
        String client = "http://api.m.nhaccuatui.com/v4/api/playlist?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=playlistLiked";
                client = client + "&username=" + username;
                client = client + "&pageindex=" + pageindex;
                client = client + "&pagesize=" + pagesize;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var7) {
                System.out.println(var7.getMessage());
            }
        }

        return null;
    }

    public static String getMyPlaylist(String username, int pageindex, int pagesize) {
        String client = "http://api.m.nhaccuatui.com/v4/api/playlist?";
        if (client != null) {
            try {
                client = client + "secretkey=nct@mobile_service";
                client = client + "&action=getMyPlaylist";
                client = client + "&username=" + username;
                client = client + "&pageindex=" + pageindex;
                client = client + "&pagesize=" + pagesize;
                client = client + "&deviceinfo=" + TextUtil.urlEncodeUTF8(APIConstants.DEVICE_INFOR);
                return client;
            } catch (Exception var7) {
                System.out.println(var7.getMessage());
            }
        }

        return null;
    }

    public static String doLike(int type, String key, String username) {
        return null;
    }
}
