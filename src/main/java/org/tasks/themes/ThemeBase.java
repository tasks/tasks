package org.tasks.themes;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;

import org.tasks.R;

public class ThemeBase {

    public static final int[] THEMES = new int[] {
            R.style.LightOverride,
            R.style.BaseBlack,
            R.style.DarkOverride
    };

    private final String name;
    private final int index;
    private final int textColorPrimary;
    private final int textColorSecondary;
    private final int textColorTertiary;
    private final int style;
    private final int dialogStyle;
    private final int contentBackground;
    private final int textColor;

    public ThemeBase(String name, int index, int dialogStyle, int contentBackground,
                     int textColor, int textColorPrimary, int textColorSecondary,
                     int textColorTertiary) {
        this.name = name;
        this.index = index;
        this.textColorPrimary = textColorPrimary;
        this.textColorSecondary = textColorSecondary;
        this.textColorTertiary = textColorTertiary;
        this.style = THEMES[index];
        this.dialogStyle = dialogStyle;
        this.contentBackground = contentBackground;
        this.textColor = textColor;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getDialogStyle() {
        return dialogStyle;
    }

    public int getContentBackground() {
        return contentBackground;
    }

    public int getTextColor() {
        return textColor;
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

    public int getTextColorPrimary() {
        return textColorPrimary;
    }

    public int getTextColorSecondary() {
        return textColorSecondary;
    }

    public int getTextColorTertiary() {
        return textColorTertiary;
    }
}
