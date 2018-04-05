/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

  public static Map<String, Object> mapFromSerializedString(String string) {
    if (string == null) {
      return new HashMap<>();
    }

    Map<String, Object> result = new HashMap<>();
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

  /** Copy a file from one place to another */
  public static void copyFile(File in, File out) throws Exception {
    FileInputStream fis = new FileInputStream(in);
    FileOutputStream fos = new FileOutputStream(out);
    try {
      copyStream(fis, fos);
    } finally {
      fis.close();
      fos.close();
    }
  }

  /** Copy stream from source to destination */
  private static void copyStream(InputStream source, OutputStream dest) throws IOException {
    int bytes;
    byte[] buffer;
    int BUFFER_SIZE = 1024;
    buffer = new byte[BUFFER_SIZE];
    while ((bytes = source.read(buffer)) != -1) {
      if (bytes == 0) {
        bytes = source.read();
        if (bytes < 0) {
          break;
        }
        dest.write(bytes);
        dest.flush();
        continue;
      }

      dest.write(buffer, 0, bytes);
      dest.flush();
    }
  }

  public static int convertDpToPixels(DisplayMetrics displayMetrics, int dp) {
    // developer.android.com/guide/practices/screens_support.html#dips-pels
    return (int) (dp * displayMetrics.density + 0.5f);
  }

  public static boolean preLollipop() {
    return !atLeastLollipop();
  }

  public static boolean preJellybean() {
    return !atLeastJellybean();
  }

  public static boolean preOreo() {
    return !atLeastOreo();
  }

  public static boolean atLeastJellybeanMR1() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
  }

  public static boolean atLeastJellybean() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
  }

  public static boolean atLeastKitKat() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
  }

  public static boolean atLeastLollipop() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  public static boolean atLeastMarshmallow() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
  }

  public static boolean atLeastNougat() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
  }

  public static boolean atLeastOreo() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
  }

  /**
   * Sleep, ignoring interruption. Before using this method, think carefully about why you are
   * ignoring interruptions.
   */
  public static void sleepDeep(long l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  /** Capitalize the first character */
  public static String capitalize(String string) {
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }

  public static void hideKeyboard(Activity activity) {
    try {
      View currentFocus = activity.getCurrentFocus();
      if (currentFocus != null) {
        InputMethodManager inputMethodManager =
            (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  /**
   * Dismiss the keyboard if it is displayed by any of the listed views
   *
   * @param views - a list of views that might potentially be displaying the keyboard
   */
  public static void hideSoftInputForViews(Context context, View... views) {
    InputMethodManager imm =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    for (View v : views) {
      imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
  }

  /** Returns the final word characters after the last '.' */
  public static String getFileExtension(String file) {
    int index = file.lastIndexOf('.');
    String extension = "";
    if (index > 0) {
      extension = file.substring(index + 1);
      if (!extension.matches("\\w+")) {
        extension = "";
      }
    }
    return extension;
  }

  interface SerializedPut<T> {

    void put(T object, String key, char type, String value) throws NumberFormatException;
  }
}
