package midplay.player;

import java.io.InputStream;
import java.util.TimerTask;
import javax.microedition.io.HttpConnection;
import javax.microedition.media.Player;

final class PendingPlayback {
  Player pendingPlayer;
  HttpConnection pendingConnection;
  InputStream pendingInputStream;
  boolean usedInputStream;
  String pendingResolvedUrl;
  long pendingContentLength = -1L;
  TimerTask connectionWatchdog;
}
