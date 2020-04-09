package org.tasks.db;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.tasks.Callback;

public class DbUtils {

  public static final int MAX_SQLITE_ARGS = 990;

  public static <F, T> List<T> collect(Collection<F> items, Function<List<F>, List<T>> func) {
    if (items.size() < MAX_SQLITE_ARGS) {
      return func.apply(items instanceof List ? (List<F>) items : newArrayList(items));
    }
    List<T> result = new ArrayList<>();
    batch(items, b -> result.addAll(func.apply(b)));
    return result;
  }

  public static <T> void batch(List<T> items, Callback<List<T>> callback) {
    batch(items, MAX_SQLITE_ARGS, callback);
  }

  public static <T> void batch(Iterable<T> items, Callback<List<T>> callback) {
    batch(items, MAX_SQLITE_ARGS, callback);
  }

  private static <T> void batch(List<T> items, int size, Callback<List<T>> callback) {
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
