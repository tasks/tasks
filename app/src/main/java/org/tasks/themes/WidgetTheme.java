package org.tasks.themes;

import android.os.Parcel;
import org.tasks.R;
import org.tasks.dialogs.ColorPickerDialog;

public class WidgetTheme implements ColorPickerDialog.Pickable {

  static final int[] BACKGROUNDS =
      new int[] {R.color.grey_50, R.color.widget_background_black, R.color.md_background_dark};
  public static Creator<WidgetTheme> CREATOR =
      new Creator<WidgetTheme>() {
        @Override
        public WidgetTheme createFromParcel(Parcel source) {
          return new WidgetTheme(source);
        }

        @Override
        public WidgetTheme[] newArray(int size) {
          return new WidgetTheme[size];
        }
      };
  private final String name;
  private final int index;
  private final int backgroundColor;
  private final int textColorPrimary;
  private final int textColorSecondary;

  public WidgetTheme(
      String name, int index, int backgroundColor, int textColorPrimary, int textColorSecondary) {
    this.name = name;
    this.index = index;
    this.backgroundColor = backgroundColor;
    this.textColorPrimary = textColorPrimary;
    this.textColorSecondary = textColorSecondary;
  }

  private WidgetTheme(Parcel source) {
    name = source.readString();
    index = source.readInt();
    backgroundColor = source.readInt();
    textColorPrimary = source.readInt();
    textColorSecondary = source.readInt();
  }

  public int getBackgroundColor() {
    return backgroundColor;
  }

  public int getTextColorPrimary() {
    return textColorPrimary;
  }

  public int getTextColorSecondary() {
    return textColorSecondary;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPickerColor() {
    return backgroundColor;
  }

  @Override
  public boolean isFree() {
    return index < 2;
  }

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
    dest.writeInt(index);
    dest.writeInt(backgroundColor);
    dest.writeInt(textColorPrimary);
    dest.writeInt(textColorSecondary);
  }
}
