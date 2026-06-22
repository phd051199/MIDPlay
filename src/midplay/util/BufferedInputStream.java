package midplay.util;

import java.io.IOException;
import java.io.InputStream;

public class BufferedInputStream extends InputStream {

  private static final int DEFAULT_BUFFER_SIZE = 8192;

  private final InputStream in;
  private final byte[] buf;
  private int count;
  private int pos;

  public BufferedInputStream(InputStream in) {
    this.in = in;
    this.buf = new byte[DEFAULT_BUFFER_SIZE];
  }

  private int fill() throws IOException {
    int n = in.read(buf, 0, buf.length);
    count = (n > 0) ? n : 0;
    pos = 0;
    return n;
  }

  public int read() throws IOException {
    if (pos >= count) {
      if (fill() <= 0) {
        return -1;
      }
    }
    return buf[pos++] & 0xFF;
  }

  public int read(byte[] b, int off, int len) throws IOException {
    if (pos >= count) {
      if (len >= buf.length) {
        return in.read(b, off, len);
      }
      if (fill() <= 0) {
        return -1;
      }
    }
    int avail = count - pos;
    int toCopy = (len < avail) ? len : avail;
    System.arraycopy(buf, pos, b, off, toCopy);
    pos += toCopy;
    return toCopy;
  }

  public int available() throws IOException {
    return count - pos;
  }

  public void close() throws IOException {
    in.close();
  }
}
