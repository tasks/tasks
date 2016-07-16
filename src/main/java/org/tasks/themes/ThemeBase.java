package org.tasks.themes;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatDelegate;
import android.view.ContextThemeWrapper;

import org.tasks.R;

public class ThemeBase {

    public static final int[] THEMES = new int[]{
            R.style.TasksOverride,
            R.style.ThemeBlack,
            R.style.TasksOverride,
            R.style.Wallpaper,
            R.style.TasksOverride
    };

    private final String name;
    private final int index;
    private final int style;
    private final int contentBackground;
    private final int dayNightMode;

    public ThemeBase(String name, int index, int dayNightMode) {
        this.name = name;
        this.index = index;
        this.dayNightMode = dayNightMode;
        this.style = THEMES[index];
        this.contentBackground = 0;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getContentBackground() {
        return contentBackground;
    }

    public boolean isDarkTheme() {
        return index > 0;
    }

    public ContextThemeWrapper wrap(Context context) {
        return new ContextThemeWrapper(context, style);
    }

    public void set(Activity activity) {
        activity.setTheme(style);
    }

    public void applyDayNightMode() {
        AppCompatDelegate.setDefaultNightMode(dayNightMode);
    }

    public void applyDayNightMode(AppCompatDelegate delegate) {
        applyDayNightMode();
        delegate.setLocalNightMode(dayNightMode);
    }
}
