package org.tasks;

public class Event<T> {

  private final T value;
  private boolean handled;

  public Event(T value) {
    this.value = value;
  }

  public T getIfUnhandled() {
    if (handled) {
      return null;
    }
    handled = true;
    return value;
  }
}
