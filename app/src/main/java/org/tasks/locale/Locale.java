package org.tasks.locale;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.text.TextUtilsCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewParent;
import com.google.common.base.Strings;
import java.text.NumberFormat;
import java.text.ParseException;
import org.tasks.R;

public class Locale {

  private static final Locale DEFAULT = new Locale(java.util.Locale.getDefault(), null, -1);
  private static final int[] sDialogButtons =
      new int[] {android.R.id.button1, android.R.id.button2, android.R.id.button3};
  private static final char LEFT_TO_RIGHT_MARK = '\u200e';
  private static final char RIGHT_TO_LEFT_MARK = '\u200f';
  private static Locale INSTANCE;
  private final java.util.Locale deviceLocale;
  private final java.util.Locale appLocale;
  private final int appDirectionality;
  private final char appDirectionalityMark;
  private final String languageOverride;
  private final int directionOverride;
  private final boolean hasUserOverrides;

  public Locale(java.util.Locale deviceLocale, String languageOverride, int directionOverride) {
    this.deviceLocale = deviceLocale;
    this.languageOverride = languageOverride;
    this.directionOverride = directionOverride;

    java.util.Locale override = localeFromString(languageOverride);
    if (override != null) {
      appLocale = override;
    } else {
      appLocale = deviceLocale;
    }

    if (directionOverride == View.LAYOUT_DIRECTION_LTR
        || directionOverride == View.LAYOUT_DIRECTION_RTL) {
      appDirectionality = directionOverride;
    } else {
      appDirectionality = TextUtilsCompat.getLayoutDirectionFromLocale(appLocale);
    }
    appDirectionalityMark =
        appDirectionality == View.LAYOUT_DIRECTION_RTL ? RIGHT_TO_LEFT_MARK : LEFT_TO_RIGHT_MARK;
    int deviceDirectionality = TextUtilsCompat.getLayoutDirectionFromLocale(deviceLocale);
    hasUserOverrides =
        !(deviceLocale.equals(appLocale) && appDirectionality == deviceDirectionality)
            && atLeastJellybeanMR1();
  }

  public static Locale getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (DEFAULT) {
        if (INSTANCE == null) {
          Context applicationContext = context.getApplicationContext();
          SharedPreferences prefs =
              PreferenceManager.getDefaultSharedPreferences(applicationContext);
          String language =
              prefs.getString(applicationContext.getString(R.string.p_language), null);
          int directionOverride =
              Integer.parseInt(
                  prefs.getString(applicationContext.getString(R.string.p_layout_direction), "-1"));
          setDefault(DEFAULT.getLocale(), language, directionOverride);
        }
      }
    }

    return getInstance();
  }

  public static void setDefault(java.util.Locale locale) {
    setDefault(locale, null, -1);
  }

  private static void setDefault(
      java.util.Locale locale, String languageOverride, int directionOverride) {
    INSTANCE = new Locale(locale, languageOverride, directionOverride);
    java.util.Locale.setDefault(locale);
  }

  public static Locale getInstance() {
    return INSTANCE == null ? DEFAULT : INSTANCE;
  }

  private static java.util.Locale localeFromString(String locale) {
    if (Strings.isNullOrEmpty(locale)) {
      return null;
    }

    String[] split = locale.split("-");
    if (split.length == 1) {
      return new java.util.Locale(split[0]);
    } else if (split.length == 2) {
      return new java.util.Locale(split[0], split[1]);
    }
    throw new RuntimeException();
  }

  public java.util.Locale getLocale() {
    return appLocale;
  }

  public java.util.Locale getDeviceLocale() {
    return deviceLocale;
  }

  public char getDirectionalityMark() {
    return appDirectionalityMark;
  }

  public int getDirectionality() {
    return appDirectionality;
  }

  public String getLanguageOverride() {
    return languageOverride;
  }

  @SuppressLint("NewApi")
  public Context createConfigurationContext(Context context) {
    return hasUserOverrides
        ? context.createConfigurationContext(getLocaleConfiguration())
        : context;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private Configuration getLocaleConfiguration() {
    Configuration configuration = new Configuration();
    configuration.locale = getLocale();
    final int layoutDirection = 1 + appDirectionality;
    configuration.screenLayout =
        (configuration.screenLayout & ~Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
            | (layoutDirection << Configuration.SCREENLAYOUT_LAYOUTDIR_SHIFT);
    return configuration;
  }

  @SuppressLint("NewApi")
  public void applyOverrideConfiguration(ContextThemeWrapper wrapper) {
    if (hasUserOverrides) {
      wrapper.applyOverrideConfiguration(getLocaleConfiguration());
    }
  }

  public Locale withLanguage(String language) {
    return new Locale(deviceLocale, language, directionOverride);
  }

  public Locale withDirectionality(int directionality) {
    return new Locale(deviceLocale, languageOverride, directionality);
  }

  public String getDisplayName() {
    java.util.Locale locale = getLocale();
    return locale.getDisplayName(locale);
  }

  public String formatNumber(int number) {
    return NumberFormat.getNumberInstance(appLocale).format(number);
  }

  public Integer parseInteger(String number) {
    try {
      return NumberFormat.getNumberInstance(appLocale).parse(number).intValue();
    } catch (ParseException e) {
      return null;
    }
  }

  public String formatPercentage(int percentage) {
    return NumberFormat.getPercentInstance(appLocale).format(percentage / 100.0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Locale locale = (Locale) o;

    return languageOverride != null
        ? languageOverride.equals(locale.languageOverride)
        : locale.languageOverride == null;
  }

  @Override
  public int hashCode() {
    return languageOverride != null ? languageOverride.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Locale{"
        + "deviceLocale="
        + deviceLocale
        + ", appLocale="
        + appLocale
        + ", appDirectionality="
        + appDirectionality
        + ", languageOverride='"
        + languageOverride
        + '\''
        + ", directionOverride="
        + directionOverride
        + ", hasUserOverrides="
        + hasUserOverrides
        + '}';
  }

  @SuppressLint("NewApi")
  public void applyDirectionality(Dialog dialog) {
    if (hasUserOverrides) {
      dialog.findViewById(android.R.id.content).setLayoutDirection(appDirectionality);
      for (int id : sDialogButtons) {
        ViewParent parent = dialog.findViewById(id).getParent();
        ((View) parent).setLayoutDirection(appDirectionality);
      }
    }
  }

  public String getLanguage() {
    return appLocale.getLanguage();
  }

  public String getCountry() {
    return appLocale.getCountry();
  }
}
