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

}
