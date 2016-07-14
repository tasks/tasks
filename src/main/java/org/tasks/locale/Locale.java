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

    private final java.util.Locale deviceLocale;
    private final java.util.Locale appLocale;
    private final int deviceDirectionality;
    private final int appDirectionality;
    private final String languageOverride;
    private final int directionOverride;
    private final boolean hasUserOverrides;

    public Locale(java.util.Locale deviceLocale, String languageOverride, int directionOverride) {
        this.deviceLocale = deviceLocale;
        this.languageOverride = languageOverride;
        this.directionOverride = directionOverride;
        deviceDirectionality = TextUtils.getLayoutDirectionFromLocale(deviceLocale);

        java.util.Locale override = localeFromString(languageOverride);
        if (override != null) {
            appLocale = override;
        } else {
            appLocale = deviceLocale;
        }

        if (directionOverride == View.LAYOUT_DIRECTION_LTR || directionOverride == View.LAYOUT_DIRECTION_RTL) {
            appDirectionality = directionOverride;
        } else if (appLocale != null) {
            appDirectionality = TextUtils.getLayoutDirectionFromLocale(appLocale);
        } else {
            appDirectionality = deviceDirectionality;
        }

        hasUserOverrides = !(deviceLocale.equals(appLocale) && appDirectionality == deviceDirectionality) && atLeastJellybeanMR1();
    }

    public java.util.Locale getLocale() {
        return appLocale;
    }

    public char getDirectionalityMark() {
        return getDirectionalityMark(appDirectionality);
    }

    private char getDirectionalityMark(int directionality) {
        return directionality == View.LAYOUT_DIRECTION_RTL ? RIGHT_TO_LEFT_MARK : LEFT_TO_RIGHT_MARK;
    }

    public int getDirectionality() {
        return appDirectionality;
    }

    public String getLanguageOverride() {
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
        configuration.screenLayout = (configuration.screenLayout&~Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)|
                (layoutDirection << Configuration.SCREENLAYOUT_LAYOUTDIR_SHIFT);
        return configuration;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Locale locale = (Locale) o;

        return languageOverride != null ? languageOverride.equals(locale.languageOverride) : locale.languageOverride == null;
    }

    @Override
    public int hashCode() {
        return languageOverride != null ? languageOverride.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Locale{" +
                "deviceLocale=" + deviceLocale +
                ", appLocale=" + appLocale +
                ", deviceDirectionality=" + deviceDirectionality +
                ", appDirectionality=" + appDirectionality +
                ", languageOverride='" + languageOverride + '\'' +
                ", directionOverride=" + directionOverride +
                ", hasUserOverrides=" + hasUserOverrides +
                '}';
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
