package org.tasks.themes;

import org.tasks.R;

public class ThemeAccent {

    public static final int[] ACCENTS = new int[] {
            R.style.BlueGreyAccent,
            R.style.RedAccent,
            R.style.PinkAccent,
            R.style.PurpleAccent,
            R.style.DeepPurpleAccent,
            R.style.IndigoAccent,
            R.style.BlueAccent,
            R.style.LightBlueAccent,
            R.style.CyanAccent,
            R.style.TealAccent,
            R.style.GreenAccent,
            R.style.LightGreenAccent,
            R.style.LimeAccent,
            R.style.YellowAccent,
            R.style.AmberAccent,
            R.style.OrangeAccent,
            R.style.DeepOrangeAccent
    };

    private final String name;
    private final int index;
    private final int style;
    private final int accentColor;

    public ThemeAccent(String name, int index, int accentColor) {
        this.name = name;
        this.index = index;
        this.style = ACCENTS[index];
        this.accentColor = accentColor;
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

    public int getAccentColor() {
        return accentColor;
    }
}
