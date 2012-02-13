package com.todoroo.astrid.service;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.view.WindowManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

@SuppressWarnings("nls")
public class ThemeService {

    public static final String THEME_WHITE = "white";
    public static final String THEME_WHITE_BLUE = "white-blue";
    public static final String THEME_BLACK = "black";
    public static final String THEME_TRANSPARENT = "transparent";
    public static final String THEME_TRANSPARENT_WHITE = "transparent-white";

    public static void applyTheme(Activity activity) {
        activity.setTheme(getTheme());

        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    public static int getTheme() {
        String preference = Preferences.getStringValue(R.string.p_theme);
        if(THEME_BLACK.equals(preference))
            return R.style.Theme;
        else if(THEME_TRANSPARENT.equals(preference))
            return R.style.Theme_Transparent;
        else if(THEME_TRANSPARENT_WHITE.equals(preference))
            return R.style.Theme_TransparentWhite;
        else if (THEME_WHITE.equals(preference))
            return R.style.Theme_White;
        else
            return R.style.Theme_White_Blue;
    }

    public static int getEditDialogTheme() {
        int themeSetting = ThemeService.getTheme();
        int theme;
        if (themeSetting == R.style.Theme || themeSetting == R.style.Theme_Transparent) {
            theme = R.style.TEA_Dialog;
        } else {
            theme = R.style.TEA_Dialog_White;
        }
        return theme;
    }

    public static int getDialogTheme() {
        int themeSetting = ThemeService.getTheme();
        int theme;
        if (themeSetting == R.style.Theme || themeSetting == R.style.Theme_Transparent) {
            theme = R.style.Theme_Dialog;
        } else {
            theme = R.style.Theme_Dialog_White;
        }
        return theme;
    }
}
