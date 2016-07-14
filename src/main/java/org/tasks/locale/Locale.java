package org.tasks.locale;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewParent;

import com.google.common.base.Strings;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

public class Locale {

    public static Locale INSTANCE;

    private static final int[] sDialogButtons = new int[] { android.R.id.button1, android.R.id.button2, android.R.id.button3 };
    private static final char LEFT_TO_RIGHT_MARK = '\u200e';
    private static final char RIGHT_TO_LEFT_MARK = '\u200f';

    public static java.util.Locale localeFromString(String locale) {
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

    private final java.util.Locale deviceLocale;
    private final java.util.Locale appLocale;
    private final int deviceDirectionality;
    private final int appDirectionality;
    private final String override;

    public Locale(java.util.Locale deviceLocale, String override) {
        this.deviceLocale = deviceLocale;
        this.appLocale = localeFromString(override);
        this.override = override;

        deviceDirectionality = TextUtils.getLayoutDirectionFromLocale(deviceLocale);

        if (appLocale != null) {
            java.util.Locale.setDefault(appLocale);
            appDirectionality = TextUtils.getLayoutDirectionFromLocale(appLocale);
        } else {
            appDirectionality = deviceDirectionality;
        }
    }

    public java.util.Locale getLocale() {
        return appLocale == null ? deviceLocale : appLocale;
    }

    public char getDeviceDirectionalityMark() {
        return getDirectionalityMark(deviceDirectionality);
    }

    public char getDirectionalityMark() {
        return getDirectionalityMark(appDirectionality);
    }

    private char getDirectionalityMark(int directionality) {
        return directionality == View.LAYOUT_DIRECTION_RTL ? RIGHT_TO_LEFT_MARK : LEFT_TO_RIGHT_MARK;
    }

    public String getOverride() {
        return override;
    }

    public Context createConfigurationContext(Context context) {
        return appLocale == null ? context : context.createConfigurationContext(getLocaleConfiguration());
    }

    private Configuration getLocaleConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setLocale(appLocale);
        return configuration;
    }

    public void fixDialogButtonDirectionality(Dialog dialog) {
        if (appDirectionality != deviceDirectionality) {
            for (int id : sDialogButtons) {
                ViewParent parent = dialog.findViewById(id).getParent();
                ((View) parent).setLayoutDirection(appDirectionality);
            }
        }
    }

    public void applyOverrideConfiguration(ContextThemeWrapper wrapper) {
        if (appLocale != null && atLeastJellybeanMR1()) {
            wrapper.applyOverrideConfiguration(getLocaleConfiguration());
        }
    }

    public Locale withOverride(String language) {
        return new Locale(deviceLocale, language);
    }

    public String getDisplayName() {
        java.util.Locale locale = getLocale();
        return locale.getDisplayName(locale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Locale locale = (Locale) o;

        return override != null ? override.equals(locale.override) : locale.override == null;
    }

    @Override
    public int hashCode() {
        return override != null ? override.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Locale{" +
                "deviceLocale=" + deviceLocale +
                ", appLocale=" + appLocale +
                ", override='" + override + '\'' +
                '}';
    }
}
