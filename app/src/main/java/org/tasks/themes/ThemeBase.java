package org.tasks.themes;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcel;
import android.support.v7.app.AppCompatDelegate;
import android.view.ContextThemeWrapper;
import org.tasks.R;
import org.tasks.dialogs.ColorPickerDialog;

public class ThemeBase implements ColorPickerDialog.Pickable {

  private static final int[] THEMES =
      new int[] {
        R.style.Tasks, R.style.ThemeBlack, R.style.Tasks, R.style.Wallpaper, R.style.Tasks
      };
  public static Creator<ThemeBase> CREATOR =
      new Creator<ThemeBase>() {
        @Override
        public ThemeBase createFromParcel(Parcel source) {
          return new ThemeBase(source);
        }

        @Override
        public ThemeBase[] newArray(int size) {
          return new ThemeBase[size];
        }
      };
  private final String name;
  private final int index;
  private final int style;
  private final int contentBackground;
  private final int dayNightMode;

  public ThemeBase(String name, int index, int contentBackground, int dayNightMode) {
    this.name = name;
    this.index = index;
    this.dayNightMode = dayNightMode;
    this.style = THEMES[index];
    this.contentBackground = contentBackground;
  }

  private ThemeBase(Parcel source) {
    name = source.readString();
    index = source.readInt();
    style = source.readInt();
    contentBackground = source.readInt();
    dayNightMode = source.readInt();
  }

  public int getAlertDialogStyle() {
    return R.style.TasksDialogAlert;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPickerColor() {
    return contentBackground;
  }

  @Override
  public boolean isFree() {
    return index < 2;
  }

  @Override
  public int getIndex() {
    return index;
  }

  public boolean isDarkTheme(Activity activity) {
    return index == 4
        ? Configuration.UI_MODE_NIGHT_YES
            == (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK)
        : index > 0;
  }

  public ContextThemeWrapper wrap(Context context) {
    return new ContextThemeWrapper(context, style);
  }

  public void set(Activity activity) {
    activity.setTheme(style);
  }

  public void setDefaultNightMode() {
    AppCompatDelegate.setDefaultNightMode(dayNightMode);
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
    dest.writeInt(contentBackground);
    dest.writeInt(dayNightMode);
  }
}
