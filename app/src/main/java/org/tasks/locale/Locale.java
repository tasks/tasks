package org.tasks.locale;

import static org.tasks.Strings.isNullOrEmpty;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewParent;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;
import org.tasks.R;

public class Locale implements Serializable {

  private static final Locale DEFAULT = new Locale(java.util.Locale.getDefault(), null);
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
  private final boolean hasUserOverrides;

  public Locale(java.util.Locale deviceLocale, String languageOverride) {
    this.deviceLocale = deviceLocale;
    this.languageOverride = languageOverride;

    java.util.Locale override = localeFromString(languageOverride);
    if (override != null) {
      appLocale = override;
    } else {
      appLocale = deviceLocale;
    }

    appDirectionality = TextUtils.getLayoutDirectionFromLocale(appLocale);
    appDirectionalityMark =
        appDirectionality == View.LAYOUT_DIRECTION_RTL ? RIGHT_TO_LEFT_MARK : LEFT_TO_RIGHT_MARK;
    int deviceDirectionality = TextUtils.getLayoutDirectionFromLocale(deviceLocale);
    hasUserOverrides =
        !(deviceLocale.equals(appLocale) && appDirectionality == deviceDirectionality);
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
          INSTANCE = new Locale(DEFAULT.getLocale(), language);
          java.util.Locale.setDefault(INSTANCE.getLocale());
        }
      }
    }

    return getInstance();
  }

  public static Locale getInstance() {
    return INSTANCE == null ? DEFAULT : INSTANCE;
  }

  private static java.util.Locale localeFromString(String locale) {
    if (isNullOrEmpty(locale)) {
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

  public @Nullable String getLanguageOverride() {
    return languageOverride;
  }

  public Context createConfigurationContext(Context context) {
    return hasUserOverrides
        ? context.createConfigurationContext(getLocaleConfiguration())
        : context;
  }

  private Configuration getLocaleConfiguration() {
    Configuration configuration = new Configuration();
    configuration.locale = getLocale();
    final int layoutDirection = 1 + appDirectionality;
    configuration.screenLayout =
        (configuration.screenLayout & ~Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
            | (layoutDirection << Configuration.SCREENLAYOUT_LAYOUTDIR_SHIFT);
    return configuration;
  }

  public void applyOverrideConfiguration(ContextThemeWrapper wrapper) {
    if (hasUserOverrides) {
      wrapper.applyOverrideConfiguration(getLocaleConfiguration());
    }
  }

  public Locale withLanguage(String language) {
    return new Locale(deviceLocale, language);
  }

  public String getDisplayName() {
    java.util.Locale locale = getLocale();
    return locale.getDisplayName(locale);
  }

  public String formatNumber(int number) {
    return NumberFormat.getNumberInstance(appLocale).format(number);
  }

  public String formatNumber(double number) {
    return NumberFormat.getNumberInstance(appLocale).format(number);
  }

  public Integer parseInteger(String number) {
    try {
      return NumberFormat.getNumberInstance(appLocale).parse(number).intValue();
    } catch (ParseException e) {
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Locale)) {
      return false;
    }
    Locale locale = (Locale) o;
    return appDirectionality == locale.appDirectionality
        && appDirectionalityMark == locale.appDirectionalityMark
        && hasUserOverrides == locale.hasUserOverrides
        && Objects.equals(deviceLocale, locale.deviceLocale)
        && Objects.equals(appLocale, locale.appLocale)
        && Objects.equals(languageOverride, locale.languageOverride);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(deviceLocale, appLocale, appDirectionality, appDirectionalityMark, languageOverride,
            hasUserOverrides);
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
        + ", hasUserOverrides="
        + hasUserOverrides
        + '}';
  }

  public void applyDirectionality(Dialog dialog) {
    if (hasUserOverrides) {
      dialog.findViewById(android.R.id.content).setLayoutDirection(appDirectionality);
      for (int id : sDialogButtons) {
        ViewParent parent = dialog.findViewById(id).getParent();
        ((View) parent).setLayoutDirection(appDirectionality);
      }
    }
  }
}
