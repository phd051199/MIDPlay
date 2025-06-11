package musicapp.utils;

import java.util.Enumeration;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class XML {

    public static final Character AMP = new Character('&');
    public static final Character APOS = new Character('\'');
    public static final Character BANG = new Character('!');
    public static final Character EQ = new Character('=');
    public static final Character GT = new Character('>');
    public static final Character LT = new Character('<');
    public static final Character QUEST = new Character('?');
    public static final Character QUOT = new Character('"');
    public static final Character SLASH = new Character('/');

    public static String escape(String string) {
        StringBuffer sb = new StringBuffer();
        int i = 0;

        for (int len = string.length(); i < len; ++i) {
            char c = string.charAt(i);
            switch (c) {
                case '"':
                    sb.append("&quot;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

    private static boolean parse(XMLTokener x, JSONObject context, String name) throws JSONException {
        JSONObject o = null;
        Object t = x.nextToken();
        String s;
        if (t == BANG) {
            char c = x.next();
            if (c == '-') {
                if (x.next() == '-') {
                    x.skipPast("-->");
                    return false;
                }

                x.back();
            } else if (c == '[') {
                t = x.nextToken();
                if (t.equals("CDATA") && x.next() == '[') {
                    s = x.nextCDATA();
                    if (s.length() > 0) {
                        context.accumulate("content", s);
                    }

                    return false;
                }

                throw x.syntaxError("Expected 'CDATA['");
            }

            int i = 1;

            do {
                t = x.nextMeta();
                if (t == null) {
                    throw x.syntaxError("Missing '>' after '<!'.");
                }

                if (t == LT) {
                    ++i;
                } else if (t == GT) {
                    --i;
                }
            } while (i > 0);

            return false;
        } else if (t == QUEST) {
            x.skipPast("?>");
            return false;
        } else if (t == SLASH) {
            if (name != null && x.nextToken().equals(name)) {
                if (x.nextToken() != GT) {
                    throw x.syntaxError("Misshaped close tag");
                } else {
                    return true;
                }
            } else {
                throw x.syntaxError("Mismatched close tag");
            }
        } else if (t instanceof Character) {
            throw x.syntaxError("Misshaped tag");
        } else {
            String n = (String) t;
            t = null;
            o = new JSONObject();

            while (true) {
                if (t == null) {
                    t = x.nextToken();
                }

                if (!(t instanceof String)) {
                    if (t == SLASH) {
                        if (x.nextToken() != GT) {
                            throw x.syntaxError("Misshaped tag");
                        }

                        context.accumulate(n, o);
                        return false;
                    }

                    if (t != GT) {
                        throw x.syntaxError("Misshaped tag");
                    }

                    while (true) {
                        t = x.nextContent();
                        if (t == null) {
                            if (name != null) {
                                throw x.syntaxError("Unclosed tag " + name);
                            }

                            return false;
                        }

                        if (t instanceof String) {
                            s = (String) t;
                            if (s.length() > 0) {
                                o.accumulate("content", s);
                            }
                        } else if (t == LT && parse(x, o, n)) {
                            if (o.length() == 0) {
                                context.accumulate(n, "");
                            } else if (o.length() == 1 && o.opt("content") != null) {
                                context.accumulate(n, o.opt("content"));
                            } else {
                                context.accumulate(n, o);
                            }

                            return false;
                        }
                    }
                }

                s = (String) t;
                t = x.nextToken();
                if (t == EQ) {
                    t = x.nextToken();
                    if (!(t instanceof String)) {
                        throw x.syntaxError("Missing value");
                    }

                    o.accumulate(s, t);
                    t = null;
                } else {
                    o.accumulate(s, "");
                }
            }
        }
    }

    public static JSONObject toJSONObject(String string) throws JSONException {
        JSONObject o = new JSONObject();
        XMLTokener x = new XMLTokener(string);

        while (x.more()) {
            x.skipPast("<");
            parse(x, o, (String) null);
        }

        return o;
    }

    public static String toString(Object o) throws JSONException {
        return toString(o, (String) null);
    }

    public static String toString(Object o, String tagName) throws JSONException {
        StringBuffer b = new StringBuffer();
        int i;
        JSONArray ja;
        int len;
        String s;
        if (!(o instanceof JSONObject)) {
            if (o instanceof JSONArray) {
                ja = (JSONArray) o;
                len = ja.length();

                for (i = 0; i < len; ++i) {
                    b.append(toString(ja.opt(i), tagName == null ? "array" : tagName));
                }

                return b.toString();
            } else {
                s = o == null ? "null" : escape(o.toString());
                return tagName == null ? "\"" + s + "\"" : (s.length() == 0 ? "<" + tagName + "/>" : "<" + tagName + ">" + s + "</" + tagName + ">");
            }
        } else {
            if (tagName != null) {
                b.append('<');
                b.append(tagName);
                b.append('>');
            }

            JSONObject jo = (JSONObject) o;
            Enumeration keys = jo.keys();

            while (true) {
                while (true) {
                    while (keys.hasMoreElements()) {
                        String k = keys.nextElement().toString();
                        Object v = jo.get(k);
                        if (v instanceof String) {
                            s = (String) v;
                        } else {
                            s = null;
                        }

                        if (k.equals("content")) {
                            if (v instanceof JSONArray) {
                                ja = (JSONArray) v;
                                len = ja.length();

                                for (i = 0; i < len; ++i) {
                                    if (i > 0) {
                                        b.append('\n');
                                    }

                                    b.append(escape(ja.get(i).toString()));
                                }
                            } else {
                                b.append(escape(v.toString()));
                            }
                        } else if (v instanceof JSONArray) {
                            ja = (JSONArray) v;
                            len = ja.length();

                            for (i = 0; i < len; ++i) {
                                b.append(toString(ja.get(i), k));
                            }
                        } else if (v.equals("")) {
                            b.append('<');
                            b.append(k);
                            b.append("/>");
                        } else {
                            b.append(toString(v, k));
                        }
                    }

                    if (tagName != null) {
                        b.append("</");
                        b.append(tagName);
                        b.append('>');
                    }

                    return b.toString();
                }
            }
        }
    }
}
