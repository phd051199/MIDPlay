package app.utils;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.media.MediaException;
import app.model.Song;

public class Utils {

    public static boolean DEBUG = false;

    private Utils() {
    }

    public static String convertString(String source) {
        while (true) {
            try {
                if (source.indexOf("&#") > 0) {
                    int indexBegin = source.indexOf("&#");
                    int indexEnd = source.indexOf(";");
                    String sChar = source.substring(indexBegin + 2, indexEnd - 1);
                    int ichar = Integer.parseInt(sChar);
                    String replace = String.valueOf((char) ichar);
                    source = source.substring(0, indexBegin - 1) + replace + source.substring(indexEnd + 1);
                    continue;
                }
            } catch (Throwable var6) {
                var6.printStackTrace();
            }

            return source;
        }
    }

    public static void debugOut(String s) {
        if (DEBUG) {
            System.out.println(s);
        }

    }

    public static void debugOut(Throwable t) {
        if (DEBUG) {
            System.out.println(t.toString());
        }

        if (DEBUG) {
            t.printStackTrace();
        }

    }

    public static void error(Throwable t, Utils.BreadCrumbTrail bct) {
        if (DEBUG) {
            t.printStackTrace();
        }

        error(friendlyException(t), bct);
    }

    public static void error(String s, Utils.BreadCrumbTrail bct) {
        Alert alert = new Alert("Error", s, (Image) null, AlertType.ERROR);
        alert.setTimeout(-2);
        bct.replaceCurrent(alert);
    }

    public static void FYI(String s, Utils.BreadCrumbTrail bct) {
        Alert alert = new Alert("FYI", s, (Image) null, AlertType.INFO);
        alert.setTimeout(-2);
        bct.replaceCurrent(alert);
    }

    public static String friendlyException(Throwable t) {
        if (t instanceof MediaException && t.getMessage().indexOf(" ") > 5) {
            return t.getMessage();
        } else {
            String s = t.toString();

            while (true) {
                int dot = s.indexOf(".");
                int space = s.indexOf(" ");
                if (space < 0) {
                    space = s.length();
                }

                int colon = s.indexOf(":");
                if (colon < 0) {
                    colon = s.length();
                }

                if (dot < 0 || dot >= space || dot >= colon) {
                    return s;
                }

                s = s.substring(dot + 1);
            }
        }
    }

    public static void query(String title, String def, int maxSize, Utils.QueryListener listener, Utils.BreadCrumbTrail bct) {
        query(title, def, maxSize, 0, listener, bct);
    }

    public static void query(String title, String def, int maxSize, int constraints, Utils.QueryListener listener, Utils.BreadCrumbTrail bct) {
        TextBox tb = new TextBox(title, def, maxSize, constraints);
        tb.addCommand(Utils.QueryTask.cancelCommand);
        tb.addCommand(Utils.QueryTask.OKCommand);
        Utils.QueryTask qt = new Utils.QueryTask(listener, bct);
        tb.setCommandListener(qt);
        bct.go(tb);
    }

    public static String[] splitURL(String url) throws Exception {
        StringBuffer u = new StringBuffer(url);
        String[] result = new String[6];

        for (int i = 0; i <= 5; ++i) {
            result[i] = "";
        }

        boolean protFound = false;
        int index = url.indexOf(":");
        if (index > 0) {
            result[0] = url.substring(0, index);
            u.delete(0, index + 1);
            protFound = true;
        } else if (index == 0) {
            throw new Exception("url format error - protocol");
        }

        int slash;
        int anchorIndex;
        if (u.length() > 2 && u.charAt(0) == '/' && u.charAt(1) == '/') {
            u.delete(0, 2);
            slash = u.toString().indexOf(47);
            if (slash < 0) {
                slash = u.length();
            }

            int colon = u.toString().indexOf(58);
            anchorIndex = slash;
            if (colon >= 0) {
                if (colon > slash) {
                    throw new Exception("url format error - port");
                }

                anchorIndex = colon;
                result[2] = u.toString().substring(colon + 1, slash);
            }

            result[1] = u.toString().substring(0, anchorIndex);
            u.delete(0, slash);
        }

        if (u.length() > 0) {
            url = u.toString();
            slash = url.lastIndexOf(47);
            if (slash > 0) {
                result[3] = url.substring(0, slash);
            }

            if (slash < url.length() - 1) {
                String fn = url.substring(slash + 1, url.length());
                anchorIndex = fn.indexOf("#");
                if (anchorIndex >= 0) {
                    result[4] = fn.substring(0, anchorIndex);
                    result[5] = fn.substring(anchorIndex + 1);
                } else {
                    result[4] = fn;
                }
            }
        }

        return result;
    }

    public static String mergeURL(String[] url) {
        return (url[0] == "" ? "" : url[0] + ":/") + (url[1] == "" ? "" : "/" + url[1]) + (url[2] == "" ? "" : ":" + url[2]) + url[3] + "/" + url[4] + (url[5] == "" ? "" : "#" + url[5]);
    }

    public static String guessContentType(String ext) {
        String ct = "";
        if (ext.equals("mpg")) {
            ct = "video/mpeg";
        } else if (!ext.equals("mid") && !ext.equals("kar")) {
            if (ext.equals("wav")) {
                ct = "audio/x-wav";
            } else if (ext.equals("jts")) {
                ct = "audio/x-tone-seq";
            } else if (ext.equals("txt")) {
                ct = "audio/x-txt";
            } else if (ext.equals("amr")) {
                ct = "audio/amr";
            } else if (ext.equals("awb")) {
                ct = "audio/amr-wb";
            } else if (ext.equals("gif")) {
                ct = "image/gif";
            } else if (ext.equals("mp3")) {
                ct = "audio/mpeg";
            }
        } else {
            ct = "audio/midi";
        }

        return ct;
    }

    public static String guessContentTypeWithURL(String url) throws Exception {
        String[] sURL = splitURL(url);
        String ext = "";
        String ct = "";
        int lastDot = sURL[4].lastIndexOf(46);
        if (lastDot >= 0) {
            ext = sURL[4].substring(lastDot + 1).toLowerCase();
        }

        System.out.println(ext);
        if (url.equals("avi")) {
            ct = "video/mpeg";
        } else {
            ct = guessContentType(ext);
        }

        return ct;
    }

    public static String guessContentTypeWithPath(String path) {
        int lastDot = path.lastIndexOf(46);
        String ext = "";
        String ct = "";
        if (lastDot >= 0) {
            ext = path.substring(lastDot + 1).toLowerCase();
        }

        ct = guessContentType(ext);
        return ct;
    }

    private static void quickSort(String[] s, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        if (hi0 > lo0) {
            String mid = s[(lo0 + hi0) / 2].toUpperCase();

            while (lo <= hi) {
                while (lo < hi0 && s[lo].toUpperCase().compareTo(mid) < 0) {
                    ++lo;
                }

                while (hi > lo0 && s[hi].toUpperCase().compareTo(mid) > 0) {
                    --hi;
                }

                if (lo <= hi) {
                    String temp = s[lo];
                    s[lo] = s[hi];
                    s[hi] = temp;
                    ++lo;
                    --hi;
                }
            }

            if (lo0 < hi) {
                quickSort(s, lo0, hi);
            }

            if (lo < hi0) {
                quickSort(s, lo, hi0);
            }
        }

    }

    public static void sort(String[] elements) {
        quickSort(elements, 0, elements.length - 1);
    }

    public interface Interruptable {

        void pauseApp();

        void resumeApp();
    }

    interface QueryListener {

        void queryOK(String var1);

        void queryCancelled();
    }

    public interface ContentHandler {

        void close();

        boolean canHandle(String var1);

        void handle(Song var1);
    }

    public interface BreadCrumbTrail {

        Displayable go(Displayable var1);

        Displayable goBack();

        void handle(Song var1);

        Displayable replaceCurrent(Displayable var1);

        Displayable getCurrentDisplayable();
    }

    public static class QueryTask implements CommandListener, Runnable {

        private static Command cancelCommand = new Command("Cancel", 3, 1);
        private static Command OKCommand = new Command("OK", 4, 1);
        private Utils.QueryListener queryListener;
        private Utils.BreadCrumbTrail queryBCT;
        private static String queryText = "";

        private QueryTask(Utils.QueryListener listener, Utils.BreadCrumbTrail bct) {
            this.queryListener = listener;
            this.queryBCT = bct;
        }

        public void commandAction(Command c, Displayable s) {
            if (this.queryBCT != null) {
                Utils.debugOut("Utils.commandAction: goBack()");
            }

            if (c == cancelCommand) {
                Utils.debugOut("Command: cancel");
                if (this.queryListener != null) {
                    this.queryListener.queryCancelled();
                }
            } else if (c == OKCommand) {
                Utils.debugOut("Command: OK");
                if (this.queryListener != null) {
                    queryText = "";
                    if (s instanceof TextBox) {
                        queryText = ((TextBox) s).getString();
                    }

                    (new Thread(this)).start();
                }
            }

        }

        public void run() {
            this.sendListenerEvent();
        }

        private void sendListenerEvent() {
            if (this.queryListener != null) {
                this.queryListener.queryOK(queryText);
            }

        }

        // $FF: synthetic method
        QueryTask(Utils.QueryListener x0, Utils.BreadCrumbTrail x1, Object x2) {
            this(x0, x1);
        }
    }
}
