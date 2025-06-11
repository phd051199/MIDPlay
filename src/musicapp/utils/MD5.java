package musicapp.utils;

import java.io.UnsupportedEncodingException;

public class MD5 {

    MD5State state;
    MD5State finals;
    static byte[] padding = new byte[]{-128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public synchronized void Init() {
        this.state = new MD5State();
        this.finals = null;
    }

    public MD5() {
        this.Init();
    }

    public MD5(Object ob) {
        this();
        this.Update(ob.toString());
    }

    private void Decode(byte[] buffer, int shift, int[] out) {
        out[0] = buffer[shift] & 255 | (buffer[shift + 1] & 255) << 8 | (buffer[shift + 2] & 255) << 16 | buffer[shift + 3] << 24;
        out[1] = buffer[shift + 4] & 255 | (buffer[shift + 5] & 255) << 8 | (buffer[shift + 6] & 255) << 16 | buffer[shift + 7] << 24;
        out[2] = buffer[shift + 8] & 255 | (buffer[shift + 9] & 255) << 8 | (buffer[shift + 10] & 255) << 16 | buffer[shift + 11] << 24;
        out[3] = buffer[shift + 12] & 255 | (buffer[shift + 13] & 255) << 8 | (buffer[shift + 14] & 255) << 16 | buffer[shift + 15] << 24;
        out[4] = buffer[shift + 16] & 255 | (buffer[shift + 17] & 255) << 8 | (buffer[shift + 18] & 255) << 16 | buffer[shift + 19] << 24;
        out[5] = buffer[shift + 20] & 255 | (buffer[shift + 21] & 255) << 8 | (buffer[shift + 22] & 255) << 16 | buffer[shift + 23] << 24;
        out[6] = buffer[shift + 24] & 255 | (buffer[shift + 25] & 255) << 8 | (buffer[shift + 26] & 255) << 16 | buffer[shift + 27] << 24;
        out[7] = buffer[shift + 28] & 255 | (buffer[shift + 29] & 255) << 8 | (buffer[shift + 30] & 255) << 16 | buffer[shift + 31] << 24;
        out[8] = buffer[shift + 32] & 255 | (buffer[shift + 33] & 255) << 8 | (buffer[shift + 34] & 255) << 16 | buffer[shift + 35] << 24;
        out[9] = buffer[shift + 36] & 255 | (buffer[shift + 37] & 255) << 8 | (buffer[shift + 38] & 255) << 16 | buffer[shift + 39] << 24;
        out[10] = buffer[shift + 40] & 255 | (buffer[shift + 41] & 255) << 8 | (buffer[shift + 42] & 255) << 16 | buffer[shift + 43] << 24;
        out[11] = buffer[shift + 44] & 255 | (buffer[shift + 45] & 255) << 8 | (buffer[shift + 46] & 255) << 16 | buffer[shift + 47] << 24;
        out[12] = buffer[shift + 48] & 255 | (buffer[shift + 49] & 255) << 8 | (buffer[shift + 50] & 255) << 16 | buffer[shift + 51] << 24;
        out[13] = buffer[shift + 52] & 255 | (buffer[shift + 53] & 255) << 8 | (buffer[shift + 54] & 255) << 16 | buffer[shift + 55] << 24;
        out[14] = buffer[shift + 56] & 255 | (buffer[shift + 57] & 255) << 8 | (buffer[shift + 58] & 255) << 16 | buffer[shift + 59] << 24;
        out[15] = buffer[shift + 60] & 255 | (buffer[shift + 61] & 255) << 8 | (buffer[shift + 62] & 255) << 16 | buffer[shift + 63] << 24;
    }

    private void Transform(MD5State state, byte[] buffer, int shift, int[] decode_buf) {
        int a = state.state[0];
        int b = state.state[1];
        int c = state.state[2];
        int d = state.state[3];
        this.Decode(buffer, shift, decode_buf);
        a += (b & c | ~b & d) + decode_buf[0] + -680876936;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + decode_buf[1] + -389564586;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + decode_buf[2] + 606105819;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + decode_buf[3] + -1044525330;
        b = (b << 22 | b >>> 10) + c;
        a += (b & c | ~b & d) + decode_buf[4] + -176418897;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + decode_buf[5] + 1200080426;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + decode_buf[6] + -1473231341;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + decode_buf[7] + -45705983;
        b = (b << 22 | b >>> 10) + c;
        a += (b & c | ~b & d) + decode_buf[8] + 1770035416;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + decode_buf[9] + -1958414417;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + decode_buf[10] + -42063;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + decode_buf[11] + -1990404162;
        b = (b << 22 | b >>> 10) + c;
        a += (b & c | ~b & d) + decode_buf[12] + 1804603682;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + decode_buf[13] + -40341101;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + decode_buf[14] + -1502002290;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + decode_buf[15] + 1236535329;
        b = (b << 22 | b >>> 10) + c;
        a += (b & d | c & ~d) + decode_buf[1] + -165796510;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + decode_buf[6] + -1069501632;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + decode_buf[11] + 643717713;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + decode_buf[0] + -373897302;
        b = (b << 20 | b >>> 12) + c;
        a += (b & d | c & ~d) + decode_buf[5] + -701558691;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + decode_buf[10] + 38016083;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + decode_buf[15] + -660478335;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + decode_buf[4] + -405537848;
        b = (b << 20 | b >>> 12) + c;
        a += (b & d | c & ~d) + decode_buf[9] + 568446438;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + decode_buf[14] + -1019803690;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + decode_buf[3] + -187363961;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + decode_buf[8] + 1163531501;
        b = (b << 20 | b >>> 12) + c;
        a += (b & d | c & ~d) + decode_buf[13] + -1444681467;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + decode_buf[2] + -51403784;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + decode_buf[7] + 1735328473;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + decode_buf[12] + -1926607734;
        b = (b << 20 | b >>> 12) + c;
        a += (b ^ c ^ d) + decode_buf[5] + -378558;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + decode_buf[8] + -2022574463;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + decode_buf[11] + 1839030562;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + decode_buf[14] + -35309556;
        b = (b << 23 | b >>> 9) + c;
        a += (b ^ c ^ d) + decode_buf[1] + -1530992060;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + decode_buf[4] + 1272893353;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + decode_buf[7] + -155497632;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + decode_buf[10] + -1094730640;
        b = (b << 23 | b >>> 9) + c;
        a += (b ^ c ^ d) + decode_buf[13] + 681279174;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + decode_buf[0] + -358537222;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + decode_buf[3] + -722521979;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + decode_buf[6] + 76029189;
        b = (b << 23 | b >>> 9) + c;
        a += (b ^ c ^ d) + decode_buf[9] + -640364487;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + decode_buf[12] + -421815835;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + decode_buf[15] + 530742520;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + decode_buf[2] + -995338651;
        b = (b << 23 | b >>> 9) + c;
        a += (c ^ (b | ~d)) + decode_buf[0] + -198630844;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + decode_buf[7] + 1126891415;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + decode_buf[14] + -1416354905;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + decode_buf[5] + -57434055;
        b = (b << 21 | b >>> 11) + c;
        a += (c ^ (b | ~d)) + decode_buf[12] + 1700485571;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + decode_buf[3] + -1894986606;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + decode_buf[10] + -1051523;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + decode_buf[1] + -2054922799;
        b = (b << 21 | b >>> 11) + c;
        a += (c ^ (b | ~d)) + decode_buf[8] + 1873313359;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + decode_buf[15] + -30611744;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + decode_buf[6] + -1560198380;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + decode_buf[13] + 1309151649;
        b = (b << 21 | b >>> 11) + c;
        a += (c ^ (b | ~d)) + decode_buf[4] + -145523070;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + decode_buf[11] + -1120210379;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + decode_buf[2] + 718787259;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + decode_buf[9] + -343485551;
        b = (b << 21 | b >>> 11) + c;
        int[] var10000 = state.state;
        var10000[0] += a;
        var10000 = state.state;
        var10000[1] += b;
        var10000 = state.state;
        var10000[2] += c;
        var10000 = state.state;
        var10000[3] += d;
    }

    public void Update(MD5State stat, byte[] buffer, int offset, int length) {
        this.finals = null;
        if (length - offset > buffer.length) {
            length = buffer.length - offset;
        }

        int index = (int) (stat.count & 63L);
        stat.count += (long) length;
        int partlen = 64 - index;
        int i;
        if (length >= partlen) {
            int[] decode_buf = new int[16];
            if (partlen == 64) {
                partlen = 0;
            } else {
                for (i = 0; i < partlen; ++i) {
                    stat.buffer[i + index] = buffer[i + offset];
                }

                this.Transform(stat, stat.buffer, 0, decode_buf);
            }

            for (i = partlen; i + 63 < length; i += 64) {
                this.Transform(stat, buffer, i + offset, decode_buf);
            }

            index = 0;
        } else {
            i = 0;
        }

        if (i < length) {
            for (int start = i; i < length; ++i) {
                stat.buffer[index + i - start] = buffer[i + offset];
            }
        }

    }

    public void Update(byte[] buffer, int offset, int length) {
        this.Update(this.state, buffer, offset, length);
    }

    public void Update(byte[] buffer, int length) {
        this.Update(this.state, buffer, 0, length);
    }

    public void Update(byte[] buffer) {
        this.Update(buffer, 0, buffer.length);
    }

    public void Update(byte b) {
        byte[] buffer = new byte[]{b};
        this.Update(buffer, 1);
    }

    public void Update(String s) {
        byte[] chars = s.getBytes();
        this.Update(chars, chars.length);
    }

    public void Update(String s, String charset_name) throws UnsupportedEncodingException {
        if (charset_name == null) {
            charset_name = "ISO8859_1";
        }

        byte[] chars = s.getBytes(charset_name);
        this.Update(chars, chars.length);
    }

    public void Update(int i) {
        this.Update((byte) (i & 255));
    }

    private byte[] Encode(int[] input, int len) {
        byte[] out = new byte[len];
        int j = 0;

        for (int i = 0; j < len; j += 4) {
            out[j] = (byte) (input[i] & 255);
            out[j + 1] = (byte) (input[i] >>> 8 & 255);
            out[j + 2] = (byte) (input[i] >>> 16 & 255);
            out[j + 3] = (byte) (input[i] >>> 24 & 255);
            ++i;
        }

        return out;
    }

    public synchronized byte[] Final() {
        if (this.finals == null) {
            MD5State fin = new MD5State(this.state);
            int[] count_ints = new int[]{(int) (fin.count << 3), (int) (fin.count >> 29)};
            byte[] bits = this.Encode(count_ints, 8);
            int index = (int) (fin.count & 63L);
            int padlen = index < 56 ? 56 - index : 120 - index;
            this.Update(fin, padding, 0, padlen);
            this.Update(fin, bits, 0, 8);
            this.finals = fin;
        }

        return this.Encode(this.finals.state, 16);
    }

    public static String asHex(byte[] hash) {
        char[] buf = new char[hash.length * 2];
        int i = 0;

        for (int var3 = 0; i < hash.length; ++i) {
            buf[var3++] = HEX_CHARS[hash[i] >>> 4 & 15];
            buf[var3++] = HEX_CHARS[hash[i] & 15];
        }

        return new String(buf);
    }

    public String asHex() {
        return asHex(this.Final());
    }

    public static boolean hashesEqual(byte[] hash1, byte[] hash2) {
        if (hash1 == null) {
            return hash2 == null;
        } else if (hash2 == null) {
            return false;
        } else {
            int targ = 16;
            if (hash1.length < 16) {
                if (hash2.length != hash1.length) {
                    return false;
                }

                targ = hash1.length;
            } else if (hash2.length < 16) {
                return false;
            }

            for (int i = 0; i < targ; ++i) {
                if (hash1[i] != hash2[i]) {
                    return false;
                }
            }

            return true;
        }
    }
}
