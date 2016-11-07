package org.tasks.themes;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatDelegate;
import android.view.ContextThemeWrapper;

import org.tasks.R;

public class ThemeBase {

    private static final int[] THEMES = new int[]{
            R.style.Tasks,
            R.style.ThemeBlack,
            R.style.Tasks,
            R.style.Wallpaper,
            R.style.Tasks
    };

    private final String name;
    private final int index;
    private final int style;
    private final int contentBackground;
    private final int dayNightMode;

    public ThemeBase(String name, int index, int contentBackground, int dayNightMode) {
        this.name = name;
        this.index = index;
        this.dayNightMode = dayNightMode;
        this.style = THEMES[index];
        this.contentBackground = contentBackground;
    }

    public int getAlertDialogStyle() {
        return R.style.TasksDialogAlert;
    }

    public String getName() {
        return name;
    }

    public int getContentBackground() {
        return contentBackground;
    }

    public boolean isDarkTheme(Activity activity) {
        return index == 4
                ? Configuration.UI_MODE_NIGHT_YES == (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                : index > 0;
    }

    public ContextThemeWrapper wrap(Context context) {
        return new ContextThemeWrapper(context, style);
    }

    public void set(Activity activity) {
        activity.setTheme(style);
    }

    public void setDefaultNightMode() {
        AppCompatDelegate.setDefaultNightMode(dayNightMode);
    }
}
