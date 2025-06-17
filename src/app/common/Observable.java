package app.common;

import app.interfaces.Observer;
import java.util.Vector;

public class Observable {

  private boolean changed = false;
  private Vector obs = new Vector();

  public synchronized void addObserver(Observer o) {
    if (o == null) {
      throw new NullPointerException();
    } else {
      if (!this.obs.contains(o)) {
        this.obs.addElement(o);
      }
    }
  }

  public synchronized void deleteObserver(Observer o) {
    this.obs.removeElement(o);
  }

  public void notifyObservers() {
    this.notifyObservers((Object) null);
  }

  public void notifyObservers(Object arg) {
    Object[] arrLocal;
    synchronized (this) {
      if (!this.changed) {
        return;
      }

      arrLocal = Common.vectorToArray(this.obs);
      this.clearChanged();
    }

    for (int i = arrLocal.length - 1; i >= 0; --i) {
      Observer observer = (Observer) arrLocal[i];
      observer.update(this, arg);
    }
  }

  public synchronized void deleteObservers() {
    this.obs.removeAllElements();
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
    return this.obs.size();
  }
}
