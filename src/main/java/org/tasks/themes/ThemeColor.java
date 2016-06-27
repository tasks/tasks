package org.tasks.themes;

import org.tasks.R;

public class ThemeColor {

    public static final int[] COLORS = new int[] {
            R.style.BlueGrey,
            R.style.DarkGrey,
            R.style.Red,
            R.style.Pink,
            R.style.Purple,
            R.style.DeepPurple,
            R.style.Indigo,
            R.style.Blue,
            R.style.LightBlue,
            R.style.Cyan,
            R.style.Teal,
            R.style.Green,
            R.style.LightGreen,
            R.style.Lime,
            R.style.Yellow,
            R.style.Amber,
            R.style.Orange,
            R.style.DeepOrange,
            R.style.Brown,
            R.style.Grey
    };

    private final String name;
    private final int index;
    private final int style;
    private final int colorPrimary;
    private final int colorPrimaryDark;

    public ThemeColor(String name, int index, int colorPrimary, int colorPrimaryDark) {
        this.name = name;
        this.index = index;
        this.style = COLORS[index];
        this.colorPrimary = colorPrimary;
        this.colorPrimaryDark = colorPrimaryDark;
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

    public int getPrimaryColor() {
        return colorPrimary;
    }

    public int getColorPrimaryDark() {
        return colorPrimaryDark;
    }
}
