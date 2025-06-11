package musicapp.utils;

import java.io.UnsupportedEncodingException;

public class TextUtil {

    static final String HEX_DIGITS = "0123456789ABCDEF";

    private TextUtil() {
    }

    protected static String urlEncode(byte[] rs) {
        StringBuffer result = new StringBuffer(rs.length * 2);

        for (int i = 0; i < rs.length; ++i) {
            char c = (char) rs[i];
            switch (c) {
                case ' ':
                    result.append('+');
                    continue;
                case '*':
                case '-':
                case '.':
                case '/':
                case '_':
                    result.append(c);
                    continue;
            }

            if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9')) {
                result.append('%');
                result.append("0123456789ABCDEF".charAt((c & 240) >> 4));
                result.append("0123456789ABCDEF".charAt(c & 15));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    protected static String urlDecode(byte[] bytes, String encoding) throws UnsupportedEncodingException, IllegalArgumentException {
        if (bytes == null) {
            return null;
        } else {
            byte[] decodeBytes = new byte[bytes.length];
            int decodedByteCount = 0;

            try {
                for (int count = 0; count < bytes.length; ++count) {
                    switch (bytes[count]) {
                        case 37:
                            int var10001 = decodedByteCount++;
                            ++count;
                            int var10002 = "0123456789ABCDEF".indexOf(bytes[count]) << 4;
                            ++count;
                            decodeBytes[var10001] = (byte) (var10002 + "0123456789ABCDEF".indexOf(bytes[count]));
                            break;
                        case 43:
                            decodeBytes[decodedByteCount++] = 32;
                            break;
                        default:
                            decodeBytes[decodedByteCount++] = bytes[count];
                    }
                }
            } catch (IndexOutOfBoundsException var7) {
                throw new IllegalArgumentException("Malformed UTF-8 string?");
            }

            String processedPageName = null;

            try {
                processedPageName = new String(decodeBytes, 0, decodedByteCount, encoding);
                return processedPageName;
            } catch (UnsupportedEncodingException var6) {
                throw new UnsupportedEncodingException("UTF-8 encoding not supported on this platform");
            }
        }
    }

    public static String urlEncodeUTF8(String text) throws Exception {
        if (text == null) {
            return "";
        } else {
            try {
                byte[] rs = text.getBytes("UTF-8");
                return urlEncode(rs);
            } catch (UnsupportedEncodingException var3) {
                throw new Exception("UTF-8 not supported!?!");
            }
        }
    }

    public static String urlDecodeUTF8(String utf8) throws Exception {
        String rs = null;
        if (utf8 == null) {
            return null;
        } else {
            try {
                rs = urlDecode(utf8.getBytes("ISO-8859-1"), "UTF-8");
                return rs;
            } catch (UnsupportedEncodingException var3) {
                throw new Exception("UTF-8 or ISO-8859-1 not supported!?!");
            }
        }
    }

    public static String urlEncode(String data, String encoding) throws Exception {
        if ("UTF-8".equals(encoding)) {
            return urlEncodeUTF8(data);
        } else {
            try {
                return urlEncode(data.getBytes(encoding));
            } catch (UnsupportedEncodingException var3) {
                throw new Exception("Could not encode String into" + encoding);
            }
        }
    }

    public static String urlDecode(String data, String encoding) throws UnsupportedEncodingException, IllegalArgumentException, Exception {
        if ("UTF-8".equals(encoding)) {
            return urlDecodeUTF8(data);
        } else {
            try {
                return urlDecode(data.getBytes(encoding), encoding);
            } catch (UnsupportedEncodingException var3) {
                throw new Exception("Could not decode String into" + encoding);
            }
        }
    }
}
