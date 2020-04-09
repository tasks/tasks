package org.tasks.themes;

import android.graphics.Color;

class ColorUtil {

  /**
   * https://stackoverflow.com/a/40964456
   * Darkens a given color
   * @param base base color
   * @param amount amount between 0 and 100
   * @return darken color
   */
  static int darken(int base, int amount) {
    if (base == Color.WHITE) {
      return Color.WHITE;
    } else if (base == Color.BLACK) {
      return Color.BLACK;
    }

    float[] hsv = new float[3];
    Color.colorToHSV(base, hsv);
    float[] hsl = hsv2hsl(hsv);
    hsl[2] -= amount / 100f;
    if (hsl[2] < 0)
      hsl[2] = 0f;
    hsv = hsl2hsv(hsl);
    return Color.HSVToColor(hsv);
  }

  /**
   * Converts HSV (Hue, Saturation, Value) color to HSL (Hue, Saturation, Lightness)
   * Credit goes to xpansive
   * https://gist.github.com/xpansive/1337890
   * @param hsv HSV color array
   * @return hsl
   */
  private static float[] hsv2hsl(float[] hsv) {
    float hue = hsv[0];
    float sat = hsv[1];
    float val = hsv[2];

    //Saturation is very different between the two color spaces
    //If (2-sat)*val < 1 set it to sat*val/((2-sat)*val)
    //Otherwise sat*val/(2-(2-sat)*val)
    //Conditional is not operating with hue, it is reassigned!
    // sat*val/((hue=(2-sat)*val)<1?hue:2-hue)
    float nhue = (2f - sat) * val;
    float nsat = sat * val / (nhue < 1f ? nhue : 2f - nhue);
    if (nsat > 1f)
      nsat = 1f;

    return new float[]{
        //[hue, saturation, lightness]
        //Range should be between 0 - 1
        hue, //Hue stays the same

        // check nhue and nsat logic
        nsat,

        nhue / 2f //Lightness is (2-sat)*val/2
        //See reassignment of hue above
    };
  }

  /**
   * Reverses hsv2hsl
   * Credit goes to xpansive
   * https://gist.github.com/xpansive/1337890
   * @param hsl HSL color array
   * @return hsv color array
   */
  private static float[] hsl2hsv(float[] hsl) {
    float hue = hsl[0];
    float sat = hsl[1];
    float light = hsl[2];

    sat *= light < .5 ? light : 1 - light;

    return new float[]{
        //[hue, saturation, value]
        //Range should be between 0 - 1

        hue, //Hue stays the same
        2f * sat / (light + sat), //Saturation
        light + sat //Value
    };
  }
}