package org.tasks.themes;

import org.tasks.R;

public class LEDColor {

    public static final int[] LED_COLORS = new int[]{
            R.color.yellow_a400,
            R.color.blue_grey_400,
            R.color.red_a400,
            R.color.pink_a400,
            R.color.purple_a400,
            R.color.deep_purple_a400,
            R.color.indigo_a400,
            R.color.blue_a400,
            R.color.light_blue_a400,
            R.color.cyan_a400,
            R.color.teal_a400,
            R.color.green_a400,
            R.color.light_green_a400,
            R.color.lime_a400,
            R.color.amber_a400,
            R.color.orange_a400,
            R.color.deep_orange_a400
    };
    private final String name;
    private final int color;

    public LEDColor(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
