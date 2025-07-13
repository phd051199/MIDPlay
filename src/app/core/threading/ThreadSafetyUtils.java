package app.core.threading;

import java.util.Hashtable;
import java.util.Vector;

public class ThreadSafetyUtils {

  private static final Hashtable namedLocks = new Hashtable();
  private static final Object namedLocksLock = new Object();
  private static final int MAX_NAMED_LOCKS = 100;

  public static Object getNamedLock(String lockName) {
    if (lockName == null) {
      throw new IllegalArgumentException("Lock name cannot be null");
    }

    synchronized (namedLocksLock) {
      Object lock = namedLocks.get(lockName);
      if (lock == null) {
        if (namedLocks.size() >= MAX_NAMED_LOCKS) {
          namedLocks.clear();
        }

        lock = new Object();
        namedLocks.put(lockName, lock);
      }
      return lock;
    }
  }

  public static void removeNamedLock(String lockName) {
    if (lockName != null) {
      synchronized (namedLocksLock) {
        namedLocks.remove(lockName);
      }
    }
  }

  public static void synchronizedExecute(String lockName, Runnable task) {
    if (task == null) {
      return;
    }

    Object lock = getNamedLock(lockName);
    synchronized (lock) {
      task.run();
    }
  }

  public static Object synchronizedCall(String lockName, Callable callable) {
    if (callable == null) {
      return null;
    }

    Object lock = getNamedLock(lockName);
    synchronized (lock) {
      try {
        return callable.call();
      } catch (Exception e) {
        throw new RuntimeException(e.toString());
      }
    }
  }

  public static Vector createSynchronizedVector() {
    return new SynchronizedVector();
  }

  public static Hashtable createSynchronizedHashtable() {
    return new SynchronizedHashtable();
  }

  public static void safeWait(Object lock, long timeoutMs) {
    if (lock == null) {
      return;
    }

    synchronized (lock) {
      try {
        lock.wait(timeoutMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static void safeNotify(Object lock) {
    if (lock == null) {
      return;
    }

    synchronized (lock) {
      lock.notify();
    }
  }

  public static void safeNotifyAll(Object lock) {
    if (lock == null) {
      return;
    }

    synchronized (lock) {
      lock.notifyAll();
    }
  }

  public static boolean safeInterrupt(Thread thread) {
    if (thread == null) {
      return false;
    }

    try {
      thread.interrupt();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static void safeSleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void clearAllNamedLocks() {
    synchronized (namedLocksLock) {
      namedLocks.clear();
    }
  }

  public static int getNamedLockCount() {
    synchronized (namedLocksLock) {
      return namedLocks.size();
    }
  }

  private ThreadSafetyUtils() {}

  private static class SynchronizedVector extends Vector {
    private final Object lock = new Object();

    public synchronized void addElement(Object obj) {
      synchronized (lock) {
        super.addElement(obj);
      }
    }

    public synchronized boolean removeElement(Object obj) {
      synchronized (lock) {
        return super.removeElement(obj);
      }
    }

    public synchronized Object elementAt(int index) {
      synchronized (lock) {
        return super.elementAt(index);
      }
    }

    public synchronized void removeElementAt(int index) {
      synchronized (lock) {
        super.removeElementAt(index);
      }
    }

    public synchronized int size() {
      synchronized (lock) {
        return super.size();
      }
    }

    public synchronized boolean isEmpty() {
      synchronized (lock) {
        return super.isEmpty();
      }
    }

    public synchronized void removeAllElements() {
      synchronized (lock) {
        super.removeAllElements();
      }
    }

    public synchronized boolean contains(Object elem) {
      synchronized (lock) {
        return super.contains(elem);
      }
    }
  }

  private static class SynchronizedHashtable extends Hashtable {
    private final Object lock = new Object();

    public synchronized Object put(Object key, Object value) {
      synchronized (lock) {
        return super.put(key, value);
      }
    }

    public synchronized Object get(Object key) {
      synchronized (lock) {
        return super.get(key);
      }
    }

    public synchronized Object remove(Object key) {
      synchronized (lock) {
        return super.remove(key);
      }
    }

    public synchronized boolean containsKey(Object key) {
      synchronized (lock) {
        return super.containsKey(key);
      }
    }

    public synchronized int size() {
      synchronized (lock) {
        return super.size();
      }
    }

    public synchronized boolean isEmpty() {
      synchronized (lock) {
        return super.isEmpty();
      }
    }

    public synchronized void clear() {
      synchronized (lock) {
        super.clear();
      }
    }

    public synchronized java.util.Enumeration keys() {
      synchronized (lock) {
        return super.keys();
      }
    }

    public synchronized java.util.Enumeration elements() {
      synchronized (lock) {
        return super.elements();
      }
    }
  }

  public interface Callable {
    Object call() throws Exception;
  }
}
