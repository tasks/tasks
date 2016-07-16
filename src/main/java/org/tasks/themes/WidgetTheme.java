package org.tasks.themes;

import org.tasks.R;

public class WidgetTheme {
    public static final int[] BACKGROUNDS = new int[] {
            R.color.widget_background_light,
            R.color.widget_background_black,
            R.color.widget_background_dark
    };
    private final String name;
    private final int index;
    private final int backgroundColor;
    private final int textColor;

    public WidgetTheme(String name, int index, int backgroundColor, int textColor) {
        this.name = name;
        this.index = index;
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

    public int getIndex() {
        return index;
    }
}
