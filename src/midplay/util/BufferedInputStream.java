package midplay.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Minimal buffered InputStream — CLDC 1.1 ships no {@code java.io.BufferedInputStream}.
 *
 * <p>Wrapping the media stream before {@code Manager.createPlayer(InputStream, ...)} avoids
 * byte-at-a-time reads on MMAPI implementations that read inefficiently from a raw socket
 * stream — a real CPU hit on slow feature phones.
 */
public class BufferedInputStream extends InputStream {

  private static final int DEFAULT_BUFFER_SIZE = 8192;

  private final InputStream in;
  private final byte[] buf;
  private int count;
  private int pos;

  public BufferedInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE);
  }

  public BufferedInputStream(InputStream in, int size) {
    this.in = in;
    this.buf = new byte[size];
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
      // Bypass the buffer for large reads to avoid a double copy.
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
    // Report only what is safely buffered. in.available() is unreliable on
    // J2ME socket/MMAPI streams (commonly returns 0 or a wrong value), and some
    // MMAPI players size their read buffers from available() — a bogus value
    // can defeat this buffering or trip a MediaException. Underreporting is
    // always safe; overreporting is not.
    return count - pos;
  }

  public void close() throws IOException {
    in.close();
  }
}
