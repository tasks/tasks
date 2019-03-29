package org.tasks;

public interface Callback<T> {
  void call(T item);
}
