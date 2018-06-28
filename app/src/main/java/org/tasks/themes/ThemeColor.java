package org.tasks.themes;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Parcel;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import org.tasks.R;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.ui.MenuColorizer;

public class ThemeColor implements ColorPickerDialog.Pickable {

  public static final int DEFAULT = 7;

  public static final int[] ICONS =
      new int[] {
        R.mipmap.ic_launcher_blue_grey,
        R.mipmap.ic_launcher_dark_grey,
        R.mipmap.ic_launcher_red,
        R.mipmap.ic_launcher_pink,
        R.mipmap.ic_launcher_purple,
        R.mipmap.ic_launcher_deep_purple,
        R.mipmap.ic_launcher_indigo,
        R.mipmap.ic_launcher_blue,
        R.mipmap.ic_launcher_light_blue,
        R.mipmap.ic_launcher_cyan,
        R.mipmap.ic_launcher_teal,
        R.mipmap.ic_launcher_green,
        R.mipmap.ic_launcher_light_green,
        R.mipmap.ic_launcher_lime,
        R.mipmap.ic_launcher_yellow,
        R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_orange,
        R.mipmap.ic_launcher_deep_orange,
        R.mipmap.ic_launcher_brown,
        R.mipmap.ic_launcher_grey
      };

  public static final String[] LAUNCHERS =
      new String[] {
        ".BlueGrey",
        ".DarkGrey",
        ".Red",
        ".Pink",
        ".Purple",
        ".DeepPurple",
        ".Indigo",
        "",
        ".LightBlue",
        ".Cyan",
        ".Teal",
        ".Green",
        ".LightGreen",
        ".Lime",
        ".Yellow",
        ".Amber",
        ".Orange",
        ".DeepOrange",
        ".Brown",
        ".Grey"
      };

  static final int[] COLORS =
      new int[] {
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

  public static Creator<ThemeColor> CREATOR =
      new Creator<ThemeColor>() {
        @Override
        public ThemeColor createFromParcel(Parcel source) {
          return new ThemeColor(source);
        }

        @Override
        public ThemeColor[] newArray(int size) {
          return new ThemeColor[size];
        }
      };
  private final String name;
  private final int index;
  private final int actionBarTint;
  private final int style;
  private final int colorPrimary;
  private final int colorPrimaryDark;
  private final boolean isDark;

  public ThemeColor(
      String name,
      int index,
      int colorPrimary,
      int colorPrimaryDark,
      int actionBarTint,
      boolean isDark) {
    this.name = name;
    this.index = index;
    this.actionBarTint = actionBarTint;
    this.style = COLORS[index];
    this.colorPrimary = colorPrimary;
    this.colorPrimaryDark = colorPrimaryDark;
    this.isDark = isDark;
  }

  private ThemeColor(Parcel source) {
    name = source.readString();
    index = source.readInt();
    actionBarTint = source.readInt();
    style = source.readInt();
    colorPrimary = source.readInt();
    colorPrimaryDark = source.readInt();
    isDark = source.readInt() == 1;
  }

  @SuppressLint("NewApi")
  public void applyToStatusBar(Activity activity) {
    setStatusBarColor(activity);

    if (atLeastMarshmallow()) {
      View decorView = activity.getWindow().getDecorView();
      int systemUiVisibility = applyLightStatusBarFlag(decorView.getSystemUiVisibility());
      decorView.setSystemUiVisibility(systemUiVisibility);
    }
  }

  @SuppressLint("NewApi")
  public void setStatusBarColor(Activity activity) {
    if (atLeastLollipop()) {
      activity.getWindow().setStatusBarColor(getColorPrimaryDark());
    }
  }

  @SuppressLint("NewApi")
  public void applyToStatusBar(DrawerLayout drawerLayout) {
    if (atLeastLollipop()) {
      drawerLayout.setStatusBarBackgroundColor(getColorPrimaryDark());
    }
    if (atLeastMarshmallow()) {
      int systemUiVisibility = applyLightStatusBarFlag(drawerLayout.getSystemUiVisibility());
      drawerLayout.setSystemUiVisibility(systemUiVisibility);
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  private int applyLightStatusBarFlag(int flag) {
    return isDark
        ? flag | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        : flag & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
  }

  public void applyStyle(Resources.Theme theme) {
    theme.applyStyle(style, true);
  }

  @SuppressLint("NewApi")
  public void applyTaskDescription(Activity activity, String description) {
    if (atLeastLollipop()) {
      activity.setTaskDescription(
          new ActivityManager.TaskDescription(description, null, getPrimaryColor()));
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPickerColor() {
    return colorPrimary;
  }

  @Override
  public boolean isFree() {
    switch (style) {
      case R.style.Blue:
      case R.style.BlueGrey:
      case R.style.DarkGrey:
        return true;
      default:
        return false;
    }
  }

  @Override
  public int getIndex() {
    return index;
  }

  public int getPrimaryColor() {
    return colorPrimary;
  }

  public int getActionBarTint() {
    return actionBarTint;
  }

  private int getColorPrimaryDark() {
    return colorPrimaryDark;
  }

  public void apply(Toolbar toolbar) {
    toolbar.setBackgroundColor(getPrimaryColor());
    MenuColorizer.colorToolbar(toolbar, actionBarTint);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
    dest.writeInt(index);
    dest.writeInt(actionBarTint);
    dest.writeInt(style);
    dest.writeInt(colorPrimary);
    dest.writeInt(colorPrimaryDark);
    dest.writeInt(isDark ? 1 : 0);
  }
}
