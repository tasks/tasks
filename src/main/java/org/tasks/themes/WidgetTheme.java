package org.tasks.themes;

import org.tasks.R;

public class WidgetTheme {
    public static final int[] BACKGROUNDS = new int[] {
            R.color.grey_50,
            R.color.widget_background_black,
            R.color.md_background_dark
    };
    private final String name;
    private final int backgroundColor;
    private final int textColorPrimary;
    private final int textColorSecondary;

    public WidgetTheme(String name, int backgroundColor, int textColorPrimary, int textColorSecondary) {
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.textColorPrimary = textColorPrimary;
        this.textColorSecondary = textColorSecondary;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getTextColorPrimary() {
        return textColorPrimary;
    }

    public int getTextColorSecondary() {
        return textColorSecondary;
    }

    public String getName() {
        return name;
    }
}
