/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.test.runner.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.R;

/**
 * Tests translations for consistency with the default values. You must extend this class and create
 * it with your own values for strings and arrays.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@RunWith(AndroidJUnit4.class)
public class TranslationTests {

  /** Loop through each locale and call runnable */
  private void forEachLocale(Callback<Resources> callback) {
    Locale[] locales = Locale.getAvailableLocales();
    for (Locale locale : locales) {
      callback.apply(getResourcesForLocale(locale));
    }
  }

  private Resources getResourcesForLocale(Locale locale) {
    Resources resources = getTargetContext().getResources();
    Configuration configuration = new Configuration(resources.getConfiguration());
    configuration.locale = locale;
    return new Resources(resources.getAssets(), resources.getDisplayMetrics(), configuration);
  }

  /** Internal test of format string parser */
  @Test
  public void testFormatStringParser() {
    String s = "abc";
    FormatStringData data = new FormatStringData(s);
    assertEquals(s, data.string);
    assertEquals(0, data.characters.length);

    s = "abc %s def";
    data = new FormatStringData(s);
    assertEquals(1, data.characters.length);
    assertEquals('s', data.characters[0]);

    s = "abc %%s def %d";
    data = new FormatStringData(s);
    assertEquals(2, data.characters.length);
    assertEquals('%', data.characters[0]);
    assertEquals('d', data.characters[1]);
    assertTrue(data.toString(), data.toString().contains("[%"));
    assertTrue(data.toString(), data.toString().contains("d]"));
    assertTrue(data.toString(), data.toString().contains(s));
    assertTrue(data.matches(new FormatStringData("espanol %% und %d si")));
    assertFalse(data.matches(new FormatStringData("ingles %d ja %% pon")));

    s = "% abc %";
    data = new FormatStringData(s);
    assertEquals(2, data.characters.length);
    assertEquals(' ', data.characters[0]);
    assertEquals('\0', data.characters[1]);
  }

  /**
   * Test that the format specifiers in translations match exactly the translations in the default
   * text
   */
  @Test
  public void testFormatStringsMatch() {
    final Resources resources = getTargetContext().getResources();
    final int[] strings = getResourceIds(R.string.class);
    final FormatStringData[] formatStrings = new FormatStringData[strings.length];

    final StringBuilder failures = new StringBuilder();

    for (int i = 0; i < strings.length; i++) {
      try {
        String string = resources.getString(strings[i]);
        formatStrings[i] = new FormatStringData(string);
      } catch (Exception e) {
        String name = resources.getResourceName(strings[i]);
        failures.append(String.format("error opening %s: %s\n", name, e.getMessage()));
      }
    }

    forEachLocale(
        r -> {
          Locale locale = r.getConfiguration().locale;
          for (int i = 0; i < strings.length; i++) {
            try {
              switch (strings[i]) {
                case R.string.abc_shareactionprovider_share_with_application:
                  continue;
              }
              String string = r.getString(strings[i]);
              FormatStringData newFS = new FormatStringData(string);
              if (!newFS.matches(formatStrings[i])) {
                String name = r.getResourceName(strings[i]);
                failures.append(
                    String.format(
                        "%s (%s): %s != %s\n", name, locale.toString(), newFS, formatStrings[i]));
              }
            } catch (Exception e) {
              String name = r.getResourceName(strings[i]);
              failures.append(
                  String.format(
                      "%s: error opening %s: %s\n", locale.toString(), name, e.getMessage()));
            }
          }
        });

    assertTrue(failures.toString(), errorCount(failures) == 0);
  }

  /** check if string contains contains substrings */
  private void contains(Resources r, int resource, StringBuilder failures, String expected) {
    String translation = r.getString(resource);
    if (!translation.contains(expected)) {
      Locale locale = r.getConfiguration().locale;
      String name = r.getResourceName(resource);
      failures.append(
          String.format("%s: %s did not contain: %s\n", locale.toString(), name, expected));
    }
  }

  /** Test dollar sign resources */
  @Test
  public void testSpecialStringsMatch() {
    final StringBuilder failures = new StringBuilder();

    forEachLocale(
        r -> {
          contains(r, R.string.CFC_tag_text, failures, "?");
          contains(r, R.string.CFC_title_contains_text, failures, "?");
          contains(r, R.string.CFC_dueBefore_text, failures, "?");
          contains(r, R.string.CFC_tag_contains_text, failures, "?");
          contains(r, R.string.CFC_gtasks_list_text, failures, "?");
        });

    assertEquals(failures.toString(), 0, failures.toString().replaceAll("[^\n]", "").length());
  }

  /** Count newlines */
  private int errorCount(StringBuilder failures) {
    int count = 0;
    int pos = -1;
    while (true) {
      pos = failures.indexOf("\n", pos + 1);
      if (pos == -1) {
        return count;
      }
      count++;
    }
  }

  /** @return an array of all string resource id's */
  private int[] getResourceIds(Class<?> resources) {
    Field[] fields = resources.getDeclaredFields();
    List<Integer> ids = new ArrayList<>(fields.length);
    for (Field field : fields) {
      try {
        ids.add(field.getInt(null));
      } catch (Exception e) {
        // not a field we care about
      }
    }
    int[] idsAsIntArray = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      idsAsIntArray[i] = ids.get(i);
    }
    return idsAsIntArray;
  }

  public interface Callback<T> {

    void apply(T entry);
  }

  private static final class FormatStringData {

    private static final char[] scratch = new char[10];

    /** format characters */
    final char[] characters;

    /** the original string */
    final String string;

    public FormatStringData(String string) {
      this.string = string;

      int pos = -1;
      int count = 0;
      while (true) {
        pos = string.indexOf('%', ++pos);
        if (pos++ == -1) {
          break;
        }
        if (pos >= string.length()) {
          scratch[count++] = '\0';
        } else {
          scratch[count++] = string.charAt(pos);
        }
      }
      characters = new char[count];
      for (int i = 0; i < count; i++) {
        characters[i] = scratch[i];
      }
    }

    /** test that the characters match */
    boolean matches(FormatStringData other) {
      if (characters.length != other.characters.length) {
        return false;
      }
      outer:
      for (int i = 0; i < characters.length; i++) {
        if (Character.isDigit(characters[i])) {
          for (int j = 0; j < other.characters.length; j++) {
            if (characters[i] == other.characters[j]) {
              break outer;
            }
          }
          return false;
        } else if (characters[i] != other.characters[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      StringBuilder value = new StringBuilder("[");
      for (int i = 0; i < characters.length; i++) {
        value.append(characters[i]);
        if (i < characters.length - 1) {
          value.append(',');
        }
      }
      value.append("]: '").append(string).append('\'');
      return value.toString();
    }
  }
}
