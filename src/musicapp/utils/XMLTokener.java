package musicapp.utils;

import java.util.Hashtable;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import org.json.me.JSONTokener;

public class XMLTokener extends JSONTokener {

    public static final Hashtable entity = new Hashtable(8);

    public XMLTokener(String s) {
        super(s);
    }

    public String nextCDATA() throws JSONException {
        StringBuffer sb = new StringBuffer();

        int i;
        do {
            char c = this.next();
            if (c == 0) {
                throw this.syntaxError("Unclosed CDATA.");
            }

            sb.append(c);
            i = sb.length() - 3;
        } while (i < 0 || sb.charAt(i) != ']' || sb.charAt(i + 1) != ']' || sb.charAt(i + 2) != '>');

        sb.setLength(i);
        return sb.toString();
    }

    public Object nextContent() throws JSONException {
        char c;
        do {
            c = this.next();
        } while (isWhitespace(c));

        if (c == 0) {
            return null;
        } else if (c == '<') {
            return XML.LT;
        } else {
            StringBuffer sb;
            for (sb = new StringBuffer(); c != '<' && c != 0; c = this.next()) {
                if (c == '&') {
                    sb.append(this.nextEntity(c));
                } else {
                    sb.append(c);
                }
            }

            this.back();
            return sb.toString().trim();
        }
    }

    public Object nextEntity(char a) throws JSONException {
        StringBuffer sb = new StringBuffer();

        while (true) {
            char c = this.next();
            if (!isLetterOrDigit(c) && c != '#') {
                if (c == ';') {
                    String s = sb.toString();
                    Object e = entity.get(s);
                    return e != null ? e : a + s + ";";
                }

                throw this.syntaxError("Missing ';' in XML entity: &" + sb);
            }

            sb.append(Character.toLowerCase(c));
        }
    }

    public Object nextMeta() throws JSONException {
        char c;
        do {
            c = this.next();
        } while (isWhitespace(c));

        switch (c) {
            case '\u0000':
                throw this.syntaxError("Misshaped meta tag.");
            case '!':
                return XML.BANG;
            case '"':
            case '\'':
                char q = c;

                do {
                    c = this.next();
                    if (c == 0) {
                        throw this.syntaxError("Unterminated string.");
                    }
                } while (c != q);

                return JSONObject.TRUE;
            case '/':
                return XML.SLASH;
            case '<':
                return XML.LT;
            case '=':
                return XML.EQ;
            case '>':
                return XML.GT;
            case '?':
                return XML.QUEST;
            default:
                while (true) {
                    c = this.next();
                    if (isWhitespace(c)) {
                        return JSONObject.TRUE;
                    }

                    switch (c) {
                        case '\u0000':
                        case '!':
                        case '"':
                        case '\'':
                        case '/':
                        case '<':
                        case '=':
                        case '>':
                        case '?':
                            this.back();
                            return JSONObject.TRUE;
                    }
                }
        }
    }

    public Object nextToken() throws JSONException {
        char c;
        do {
            c = this.next();
        } while (isWhitespace(c));

        StringBuffer sb;
        switch (c) {
            case '\u0000':
                throw this.syntaxError("Misshaped element.");
            case '!':
                return XML.BANG;
            case '"':
            case '\'':
                char q = c;
                sb = new StringBuffer();

                while (true) {
                    c = this.next();
                    if (c == 0) {
                        throw this.syntaxError("Unterminated string.");
                    }

                    if (c == q) {
                        return sb.toString();
                    }

                    if (c == '&') {
                        sb.append(this.nextEntity(c));
                    } else {
                        sb.append(c);
                    }
                }
            case '/':
                return XML.SLASH;
            case '<':
                throw this.syntaxError("Misplaced '<'.");
            case '=':
                return XML.EQ;
            case '>':
                return XML.GT;
            case '?':
                return XML.QUEST;
            default:
                sb = new StringBuffer();

                while (true) {
                    sb.append(c);
                    c = this.next();
                    if (isWhitespace(c)) {
                        return sb.toString();
                    }

                    switch (c) {
                        case '\u0000':
                        case '!':
                        case '/':
                        case '=':
                        case '>':
                        case '?':
                        case '[':
                        case ']':
                            this.back();
                            return sb.toString();
                        case '"':
                        case '\'':
                        case '<':
                            throw this.syntaxError("Bad character in a name.");
                    }
                }
        }
    }

    private static boolean isWhitespace(char c) {
        switch (c) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
                return true;
            default:
                return false;
        }
    }

    private static boolean isLetterOrDigit(char c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                return true;
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            default:
                return false;
        }
    }

    static {
        entity.put("amp", XML.AMP);
        entity.put("apos", XML.APOS);
        entity.put("gt", XML.GT);
        entity.put("lt", XML.LT);
        entity.put("quot", XML.QUOT);
    }
}
