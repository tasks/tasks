package org.tasks;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.todoroo.astrid.data.Task;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.tasks.caldav.CaldavConverter;

public class TestUtilities {

  private static boolean mockitoInitialized;

  public static void initializeMockito(Context context) {
    if (!mockitoInitialized) {
      // for mockito: https://code.google.com/p/dexmaker/issues/detail?id=2
      System.setProperty("dexmaker.dexcache", context.getCacheDir().toString());
      mockitoInitialized = true;
    }
  }

  public static Task vtodo(String path) {
    Task task = new Task();

    CaldavConverter.apply(task, fromResource(path));

    return task;
  }

  private static at.bitfire.ical4android.Task fromResource(String path) {
    Context context = InstrumentationRegistry.getInstrumentation().getContext();
    InputStream is = null;
    InputStreamReader reader = null;
    try {
      is = context.getAssets().open(path);
      reader = new InputStreamReader(is, Charsets.UTF_8);
      return fromString(CharStreams.toString(reader));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ignored) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private static at.bitfire.ical4android.Task fromString(String task) {
    try {
      return at.bitfire.ical4android.Task.Companion.fromReader(new StringReader(task)).get(0);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
}
