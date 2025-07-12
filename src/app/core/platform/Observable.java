package app.core.platform;

import app.utils.text.StringUtils;
import java.util.Vector;

public class Observable {

  private boolean changed = false;
  private final Vector observers = new Vector();

  public synchronized void addObserver(Observer observer) {
    if (observer == null) {
      throw new NullPointerException();
    } else {
      if (!this.observers.contains(observer)) {
        this.observers.addElement(observer);
      }
    }
  }

  public synchronized void deleteObserver(Observer observer) {
    this.observers.removeElement(observer);
  }

  public void notifyObservers() {
    this.notifyObservers((Object) null);
  }

  public void notifyObservers(Object arg) {
    Object[] observerArray;
    synchronized (this) {
      if (!this.changed) {
        return;
      }

      observerArray = StringUtils.vectorToArray(this.observers);
      this.clearChanged();
    }

    for (int i = observerArray.length - 1; i >= 0; --i) {
      Observer observer = (Observer) observerArray[i];
      observer.update(this, arg);
    }
  }

  public synchronized void deleteObservers() {
    this.observers.removeAllElements();
  }

  protected synchronized void setChanged() {
    this.changed = true;
  }

  protected synchronized void clearChanged() {
    this.changed = false;
  }

  public synchronized boolean hasChanged() {
    return this.changed;
  }

  public synchronized int countObservers() {
    return this.observers.size();
  }
}
