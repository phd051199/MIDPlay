package midplay.player;

// Cached media-time samples for the progress display, scoped to a playback
// session. PlayerGUI reads and mutates these under its playback lock; the
// trust-vs-resample decision logic (and the device-bug guards around it) stays
// inline in PlayerGUI, since it interleaves native MMAPI reads.
final class MediaTimeCache {
  long cachedMediaTimeMicros = 0L;
  long cachedDurationMicros = 0L;
  long cachedSampleTimeMs = 0L;
  int cachedSessionId = -1;

  void reset(int sessionId) {
    cachedMediaTimeMicros = 0L;
    cachedDurationMicros = 0L;
    cachedSampleTimeMs = 0L;
    cachedSessionId = sessionId;
  }
}
