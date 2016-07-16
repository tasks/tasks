package org.tasks.themes;

import org.tasks.R;

public class WidgetBackground {
    public static final int[] BACKGROUNDS = new int[] {
            R.color.md_light_background,
            android.R.color.black,
            R.color.md_dark_background
    };
    private final String name;
    private final int index;
    private final int backgroundColor;

    public WidgetBackground(String name, int index, int backgroundColor) {
        this.name = name;
        this.index = index;
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }
}
