package app.utils;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Image;

public class ImageCache {

  private static final int DEFAULT_MAX_CACHE_SIZE = 15;
  private static final int DEFAULT_MAX_MEMORY_KB = 512;

  private static ImageCache instance = null;

  private final Hashtable cache;
  private final Vector accessOrder;
  private final int maxCacheSize;
  private final int maxMemoryBytes;
  private int currentMemoryUsage;

  private static class CacheEntry {
    final Image image;
    final int sizeBytes;
    final long timestamp;
    int accessCount;

    CacheEntry(Image image, int sizeBytes) {
      this.image = image;
      this.sizeBytes = sizeBytes;
      this.timestamp = System.currentTimeMillis();
      this.accessCount = 1;
    }

    void updateAccess() {
      this.accessCount++;
    }
  }

  private ImageCache(int maxCacheSize, int maxMemoryKB) {
    this.maxCacheSize = maxCacheSize;
    this.maxMemoryBytes = maxMemoryKB * 1024;
    this.cache = new Hashtable(maxCacheSize);
    this.accessOrder = new Vector(maxCacheSize);
    this.currentMemoryUsage = 0;
  }

  public static synchronized ImageCache getInstance() {
    if (instance == null) {
      instance = new ImageCache(DEFAULT_MAX_CACHE_SIZE, DEFAULT_MAX_MEMORY_KB);
    }
    return instance;
  }

  public synchronized Image getImage(String url, int size) {
    if (url == null || url.length() == 0) {
      return null;
    }

    String cacheKey = createCacheKey(url, size);

    CacheEntry entry = (CacheEntry) cache.get(cacheKey);
    if (entry != null) {
      entry.updateAccess();
      updateAccessOrder(cacheKey);
      return entry.image;
    }

    try {
      Image image = loadImageFromNetwork(url, size);
      if (image != null) {
        putImageInCache(cacheKey, image);
      }
      return image;
    } catch (IOException e) {
      return null;
    }
  }

  private Image loadImageFromNetwork(String url, int size) throws IOException {
    return ImageUtils.getImageFromNetwork(url, size);
  }

  private void putImageInCache(String key, Image image) {
    if (image == null) {
      return;
    }

    int imageSize = estimateImageSize(image);

    while ((cache.size() >= maxCacheSize || currentMemoryUsage + imageSize > maxMemoryBytes)
        && !cache.isEmpty()) {
      evictLeastRecentlyUsed();
    }

    if (cache.size() < maxCacheSize && currentMemoryUsage + imageSize <= maxMemoryBytes) {

      CacheEntry entry = new CacheEntry(image, imageSize);
      cache.put(key, entry);
      accessOrder.addElement(key);
      currentMemoryUsage += imageSize;
    }
  }

  private void evictLeastRecentlyUsed() {
    if (accessOrder.isEmpty()) {
      return;
    }

    String lruKey = null;
    long oldestAccess = Long.MAX_VALUE;

    for (int i = 0; i < accessOrder.size(); i++) {
      String key = (String) accessOrder.elementAt(i);
      CacheEntry entry = (CacheEntry) cache.get(key);
      if (entry != null && entry.timestamp < oldestAccess) {
        oldestAccess = entry.timestamp;
        lruKey = key;
      }
    }

    if (lruKey != null) {
      removeFromCache(lruKey);
    }
  }

  private void removeFromCache(String key) {
    CacheEntry entry = (CacheEntry) cache.remove(key);
    if (entry != null) {
      currentMemoryUsage -= entry.sizeBytes;
      accessOrder.removeElement(key);
    }
  }

  private void updateAccessOrder(String key) {
    accessOrder.removeElement(key);
    accessOrder.addElement(key);
  }

  private String createCacheKey(String url, int size) {
    StringBuffer sb = new StringBuffer(url.length() + 10);
    sb.append(url).append("_").append(size);
    return sb.toString();
  }

  private int estimateImageSize(Image image) {
    if (image == null) {
      return 0;
    }

    int width = image.getWidth();
    int height = image.getHeight();

    return (width * height * 2) + 100;
  }

  public synchronized void clearCache() {
    cache.clear();
    accessOrder.removeAllElements();
    currentMemoryUsage = 0;
  }

  public synchronized CacheStats getStats() {
    return new CacheStats(cache.size(), maxCacheSize, currentMemoryUsage, maxMemoryBytes);
  }

  public static class CacheStats {
    public final int currentSize;
    public final int maxSize;
    public final int memoryUsageBytes;
    public final int maxMemoryBytes;

    CacheStats(int currentSize, int maxSize, int memoryUsageBytes, int maxMemoryBytes) {
      this.currentSize = currentSize;
      this.maxSize = maxSize;
      this.memoryUsageBytes = memoryUsageBytes;
      this.maxMemoryBytes = maxMemoryBytes;
    }

    public int getMemoryUsageKB() {
      return memoryUsageBytes / 1024;
    }

    public int getMaxMemoryKB() {
      return maxMemoryBytes / 1024;
    }

    public int getMemoryUsagePercent() {
      if (maxMemoryBytes == 0) return 0;
      return (memoryUsageBytes * 100) / maxMemoryBytes;
    }
  }

  public void preloadImageAsync(final String url, final int size) {
    Thread preloadThread =
        ThreadManager.createThread(
            new Runnable() {
              public void run() {
                try {
                  getImage(url, size);
                } catch (Exception e) {
                }
              }
            },
            "ImagePreloader");

    ThreadManager.safeStartThread(preloadThread);
  }
}
