package org.tasks.themes;

import android.content.res.Resources;
import android.os.Parcel;
import org.tasks.R;
import org.tasks.dialogs.ColorPickerDialog;

public class ThemeAccent implements ColorPickerDialog.Pickable {

  static final int[] ACCENTS =
      new int[] {
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
  public static Creator<ThemeAccent> CREATOR =
      new Creator<ThemeAccent>() {
        @Override
        public ThemeAccent createFromParcel(Parcel source) {
          return new ThemeAccent(source);
        }

        @Override
        public ThemeAccent[] newArray(int size) {
          return new ThemeAccent[size];
        }
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

  private ThemeAccent(Parcel source) {
    name = source.readString();
    index = source.readInt();
    style = source.readInt();
    accentColor = source.readInt();
  }

  public void apply(Resources.Theme theme) {
    theme.applyStyle(style, true);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPickerColor() {
    return accentColor;
  }

  @Override
  public boolean isFree() {
    switch (style) {
      case R.style.BlueGreyAccent:
      case R.style.RedAccent:
        return true;
      default:
        return false;
    }
  }

  @Override
  public int getIndex() {
    return index;
  }

  public int getAccentColor() {
    return accentColor;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
    dest.writeInt(index);
    dest.writeInt(style);
    dest.writeInt(accentColor);
  }
}
