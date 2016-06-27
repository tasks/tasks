package org.tasks.themes;

import org.tasks.R;

public class ThemeBase {

    public static final int[] THEMES = new int[] {
            R.style.LightOverride,
            R.style.BaseBlack,
            R.style.DarkOverride
    };

    private final String name;
    private final int index;
    private final int style;
    private final int dialogStyle;
    private final int contentBackground;
    private final int textColor;

    public ThemeBase(String name, int index, int dialogStyle, int contentBackground, int textColor) {
        this.name = name;
        this.index = index;
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

    public int getStyle() {
        return style;
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
}
