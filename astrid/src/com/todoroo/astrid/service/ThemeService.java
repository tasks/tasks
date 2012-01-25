package com.todoroo.astrid.service;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.view.WindowManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class ThemeService {

    @SuppressWarnings("nls")
    public static void applyTheme(Activity activity) {
        activity.setTheme(getTheme());

        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    public static int getTheme() {
        String preference = Preferences.getStringValue(R.string.p_theme);
        if(preference != null && preference.equals("black"))
            return R.style.Theme;
        else if(preference != null && preference.equals("transparent"))
            return R.style.Theme_Transparent;
        else if(preference != null && preference.equals("transparent-white"))
            return R.style.Theme_TransparentWhite;
        else
            return R.style.Theme_White;
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
