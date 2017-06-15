package org.tasks.themes;

import org.tasks.R;

public class LEDColor {

    public static final int[] LED_COLORS = new int[]{
            R.color.led_white,
            R.color.led_red,
            R.color.led_orange,
            R.color.led_yellow,
            R.color.led_green,
            R.color.led_blue,
            R.color.led_purple
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
