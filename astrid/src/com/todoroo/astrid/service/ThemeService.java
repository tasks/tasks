package com.todoroo.astrid.service;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class ThemeService {

    private static void applyTheme(View parent) {
        if(Preferences.getBoolean(R.string.p_transparent, false))
            parent.setBackgroundResource(R.drawable.background_transparent);
        else
            parent.setBackgroundResource(R.drawable.background_gradient);
    }

    public static void applyTheme(Activity activity) {
        View root = ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
        applyTheme(root);

        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

}
