/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.utility;

import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.widget.TextView;

import org.tasks.BuildConfig;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import timber.log.Timber;

/**
 * Android Utility Classes
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class AndroidUtilities {

  public static final String SEPARATOR_ESCAPE = "!PIPE!"; // $NON-NLS-1$
  public static final String SERIALIZATION_SEPARATOR = "|"; // $NON-NLS-1$

  // --- utility methods

  /** Suppress virtual keyboard until user's first tap */
  public static void suppressVirtualKeyboard(final TextView editor) {
    final int inputType = editor.getInputType();
    editor.setInputType(InputType.TYPE_NULL);
    editor.setOnTouchListener(
        (v, event) -> {
          editor.setInputType(inputType);
          editor.setOnTouchListener(null);
          return false;
        });
  }

  // --- serialization

  /** Serializes a content value into a string */
  public static String mapToSerializedString(Map<String, Object> source) {
    StringBuilder result = new StringBuilder();
    for (Entry<String, Object> entry : source.entrySet()) {
      addSerialized(result, entry.getKey(), entry.getValue());
    }
    return result.toString();
  }

  /** add serialized helper */
  private static void addSerialized(StringBuilder result, String key, Object value) {
    result
        .append(key.replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE))
        .append(SERIALIZATION_SEPARATOR);
    if (value instanceof Integer) {
      result.append('i').append(value);
    } else if (value instanceof Double) {
      result.append('d').append(value);
    } else if (value instanceof Long) {
      result.append('l').append(value);
    } else if (value instanceof String) {
      result
          .append('s')
          .append(value.toString().replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE));
    } else if (value instanceof Boolean) {
      result.append('b').append(value);
    } else {
      throw new UnsupportedOperationException(value.getClass().toString());
    }
    result.append(SERIALIZATION_SEPARATOR);
  }

  public static Map<String, Serializable> mapFromSerializedString(String string) {
    if (string == null) {
      return new HashMap<>();
    }

    Map<String, Serializable> result = new HashMap<>();
    fromSerialized(
        string,
        result,
        (object, key, type, value) -> {
          switch (type) {
            case 'i':
              object.put(key, Integer.parseInt(value));
              break;
            case 'd':
              object.put(key, Double.parseDouble(value));
              break;
            case 'l':
              object.put(key, Long.parseLong(value));
              break;
            case 's':
              object.put(key, value.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR));
              break;
            case 'b':
              object.put(key, Boolean.parseBoolean(value));
              break;
          }
        });
    return result;
  }

  private static <T> void fromSerialized(String string, T object, SerializedPut<T> putter) {
    String[] pairs = string.split("\\" + SERIALIZATION_SEPARATOR); // $NON-NLS-1$
    for (int i = 0; i < pairs.length; i += 2) {
      try {
        String key = pairs[i].replaceAll(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR);
        String value = pairs[i + 1].substring(1);
        try {
          putter.put(object, key, pairs[i + 1].charAt(0), value);
        } catch (NumberFormatException e) {
          // failed parse to number
          putter.put(object, key, 's', value);
          Timber.e(e);
        }
      } catch (IndexOutOfBoundsException e) {
        Timber.e(e);
      }
    }
  }

  public static int convertDpToPixels(DisplayMetrics displayMetrics, int dp) {
    // developer.android.com/guide/practices/screens_support.html#dips-pels
    return (int) (dp * displayMetrics.density + 0.5f);
  }

  public static boolean preOreo() {
    return !atLeastOreo();
  }

  public static boolean preS() {
    return !atLeastS();
  }

  public static boolean preTiramisu() {
    return !atLeastTiramisu();
  }

  public static boolean preUpsideDownCake() {
    return VERSION.SDK_INT <= VERSION_CODES.TIRAMISU;
  }

  public static boolean atLeastNougatMR1() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
  }

  public static boolean atLeastOreo() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
  }

  public static boolean atLeastOreoMR1() {
    return Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1;
  }

  public static boolean atLeastP() {
    return VERSION.SDK_INT >= Build.VERSION_CODES.P;
  }

  public static boolean atLeastQ() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
  }

  public static boolean atLeastR() {
    return VERSION.SDK_INT >= VERSION_CODES.R;
  }

  public static boolean atLeastS() {
    return VERSION.SDK_INT >= VERSION_CODES.S;
  }

  public static boolean atLeastTiramisu() {
    return VERSION.SDK_INT >= VERSION_CODES.TIRAMISU;
  }

  public static void assertMainThread() {
    if (BuildConfig.DEBUG && !isMainThread()) {
      throw new IllegalStateException("Should be called from main thread");
    }
  }

  public static void assertNotMainThread() {
    if (BuildConfig.DEBUG && isMainThread()) {
      throw new IllegalStateException("Should not be called from main thread");
    }
  }

  private static boolean isMainThread() {
    return Thread.currentThread() == Looper.getMainLooper().getThread();
  }

  interface SerializedPut<T> {

    void put(T object, String key, char type, String value) throws NumberFormatException;
  }
}
