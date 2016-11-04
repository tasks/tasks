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
    private final int textColor;

    public WidgetTheme(String name, int backgroundColor, int textColor) {
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public String getName() {
        return name;
    }
}
