package org.tasks.locale;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewParent;

import com.google.common.base.Strings;

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

    private static final int[] sDialogButtons = new int[] { android.R.id.button1, android.R.id.button2, android.R.id.button3 };
    private static final char LEFT_TO_RIGHT_MARK = '\u200e';
    private static final char RIGHT_TO_LEFT_MARK = '\u200f';
    private static String sLocaleString;
    private static Locale sLocale;

    public static Locale getLocale() {
        return sLocale == null ? Locale.getDefault() : sLocale;
    }

    public static char getDirectionalityMark() {
        return TextUtils.getLayoutDirectionFromLocale(getLocale()) == View.LAYOUT_DIRECTION_LTR
                ? LEFT_TO_RIGHT_MARK
                : RIGHT_TO_LEFT_MARK;
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

    public static void fixDialogButtons(Dialog dialog) {
        if (sLocale != null) {
            int layoutDirectionFromLocale = TextUtils.getLayoutDirectionFromLocale(sLocale);
            for (int id : sDialogButtons) {
                ViewParent parent = dialog.findViewById(id).getParent();
                setDirection((View) parent, layoutDirectionFromLocale);
            }
        }
    }

    private static void setDirection(View view, int direction) {
        view.setLayoutDirection(direction);
    }

    public static void applyOverrideConfiguration(ContextThemeWrapper wrapper) {
        if (sLocale != null && atLeastJellybeanMR1()) {
            wrapper.applyOverrideConfiguration(getLocaleConfiguration());
        }
    }

    public static Context createConfigurationContext(Context context) {
        return sLocale == null ? context : context.createConfigurationContext(getLocaleConfiguration());
    }

    private static Configuration getLocaleConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setLocale(sLocale);
        return configuration;
    }
}
