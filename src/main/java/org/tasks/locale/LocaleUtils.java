package org.tasks.locale;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import java.util.Locale;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

public class LocaleUtils {
    public static Locale localeFromString(String locale) {
        if (Strings.isNullOrEmpty(locale)) {
            return null;
        }

        String[] split = locale.split("-");
        if (split.length == 1) {
            return new Locale(split[0]);
        } else if (split.length == 2) {
            return new Locale(split[0], split[1]);
        }
        throw new RuntimeException();
    }

    private static String sLocaleString;
    private static Locale sLocale;

    public static Locale getLocale() {
        return sLocale == null ? Locale.getDefault() : sLocale;
    }

    public static String getsLocaleString() {
        return sLocaleString;
    }

    public static void setLocale(String locale) {
        sLocaleString = locale;
        sLocale = localeFromString(locale);
        if (sLocale != null) {
            Locale.setDefault(sLocale);
        }
    }

    public static void updateConfig(ContextThemeWrapper wrapper) {
        if (sLocale != null && atLeastJellybeanMR1()) {
            wrapper.applyOverrideConfiguration(getLocaleConfiguration());
        }
    }

    public static Context withLocale(Context context) {
        return sLocale == null ? context : context.createConfigurationContext(getLocaleConfiguration());
    }

    private static Configuration getLocaleConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setLocale(sLocale);
        return configuration;
    }
}
