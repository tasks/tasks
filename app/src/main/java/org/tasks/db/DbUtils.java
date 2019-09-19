package org.tasks.db;

import static com.google.common.collect.Lists.partition;

import com.google.common.collect.Iterables;
import java.util.List;
import org.tasks.Callback;

public class DbUtils {

  private static final int MAX_SQLITE_ARGS = 999;

  public static <T> void batch(List<T> items, Callback<List<T>> callback) {
    batch(items, MAX_SQLITE_ARGS, callback);
  }

  public static <T> void batch(Iterable<T> items, Callback<List<T>> callback) {
    batch(items, MAX_SQLITE_ARGS, callback);
  }

  public static <T> void batch(List<T> items, int size, Callback<List<T>> callback) {
    if (items.isEmpty()) {
      return;
    }
    if (items.size() <= size) {
      callback.call(items);
    } else {
      for (List<T> sublist : partition(items, size)) {
        callback.call(sublist);
      }
    }
  }

  public static <T> void batch(Iterable<T> items, int size, Callback<List<T>> callback) {
    if (!items.iterator().hasNext()) {
      return;
    }
    for (List<T> sublist : Iterables.partition(items, size)) {
      callback.call(sublist);
    }
  }
}
