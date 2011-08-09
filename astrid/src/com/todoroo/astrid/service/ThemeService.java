package com.todoroo.astrid.service;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.view.WindowManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class ThemeService {

    @SuppressWarnings("nls")
    public static void applyTheme(Activity activity) {
        String preference = Preferences.getStringValue(R.string.p_theme);
        if(preference != null && preference.equals("black"))
            activity.setTheme(R.style.Theme);
        else if(preference != null && preference.equals("transparent"))
            activity.setTheme(R.style.Theme_Transparent);
        else if(preference != null && preference.equals("transparent-white"))
            activity.setTheme(R.style.Theme_TransparentWhite);
        else
            activity.setTheme(R.style.Theme_White);

        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

}
