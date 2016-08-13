package org.tasks.themes;

import android.content.res.Resources;

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
    private final int style;
    private final int accentColor;

    public ThemeAccent(String name, int index, int accentColor) {
        this.name = name;
        this.style = ACCENTS[index];
        this.accentColor = accentColor;
    }

    public void apply(Resources.Theme theme) {
        theme.applyStyle(style, true);
    }

    public String getName() {
        return name;
    }

    public int getAccentColor() {
        return accentColor;
    }
}
