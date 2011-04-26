package com.todoroo.astrid.service;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class ThemeService {

    public static void applyTheme(View parent) {
        if(Preferences.getBoolean(R.string.p_transparent, false))
            parent.setBackgroundResource(R.drawable.background_transparent);
        else
            parent.setBackgroundResource(R.drawable.background_gradient);
    }

    public static void applyTheme(Activity activity) {
        View view = ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
        applyTheme(view);
    }

}
