package org.tasks.themes;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import org.tasks.R;
import org.tasks.dialogs.ColorPalettePicker.Pickable;
import timber.log.Timber;

public class ThemeColor implements Pickable {

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

  public static final int[] COLORS =
      new int[] {
        R.color.theme_blue_grey,
        R.color.theme_dark_grey,
        R.color.theme_red,
        R.color.theme_pink,
        R.color.theme_purple,
        R.color.theme_deep_purple,
        R.color.theme_indigo,
        R.color.theme_blue,
        R.color.theme_light_blue,
        R.color.theme_cyan,
        R.color.theme_teal,
        R.color.theme_green,
        R.color.theme_light_green,
        R.color.theme_lime,
        R.color.theme_yellow,
        R.color.theme_amber,
        R.color.theme_orange,
        R.color.theme_deep_orange,
        R.color.theme_brown,
        R.color.theme_grey,
        R.color.theme_day_night
      };

  public static final int[] LAUNCHER_COLORS =
      new int[] {
        R.color.blue_grey_500,
        R.color.grey_900,
        R.color.red_500,
        R.color.pink_500,
        R.color.purple_500,
        R.color.deep_purple_500,
        R.color.indigo_500,
        R.color.blue_500,
        R.color.light_blue_500,
        R.color.cyan_500,
        R.color.teal_500,
        R.color.green_500,
        R.color.light_green_500,
        R.color.lime_500,
        R.color.yellow_500,
        R.color.amber_500,
        R.color.orange_500,
        R.color.deep_orange_500,
        R.color.brown_500,
        R.color.grey_500
      };

  public static final Parcelable.Creator<ThemeColor> CREATOR =
      new Parcelable.Creator<ThemeColor>() {
        @Override
        public ThemeColor createFromParcel(Parcel source) {
          return new ThemeColor(source);
        }

        @Override
        public ThemeColor[] newArray(int size) {
          return new ThemeColor[size];
        }
      };
  private final int index;
  private final int colorOnPrimary;
  private final int colorPrimary;
  private final int colorPrimaryVariant;
  private final boolean isDark;

  public ThemeColor(Context context, int color) {
    this(context, -1, color == 0 ? ContextCompat.getColor(context, R.color.blue_500) : color);
  }

  public ThemeColor(Context context, int index, int colorPrimary) {
    this.index = index;
    colorPrimary |= 0xFF000000; // remove alpha
    this.colorPrimary = colorPrimary;
    this.colorPrimaryVariant = ColorUtil.darken(colorPrimary, 12);

    int whiteText = context.getResources().getColor(R.color.white_87);
    double contrast = ColorUtils.calculateContrast(whiteText, colorPrimary);
    this.isDark = contrast < 3;
    colorOnPrimary = isDark ? context.getResources().getColor(R.color.black_87) : whiteText;
  }

  private ThemeColor(Parcel source) {
    index = source.readInt();
    colorOnPrimary = source.readInt();
    colorPrimary = source.readInt();
    colorPrimaryVariant = source.readInt();
    isDark = source.readInt() == 1;
  }

  public static ThemeColor newThemeColor(Context context, int color) {
    try {
      return new ThemeColor(context, color);
    } catch (Exception e) {
      Timber.e(e);
      return new ThemeColor(context, 0);
    }
  }

  private static void colorMenu(Menu menu, int color) {
    for (int i = 0, size = menu.size(); i < size; i++) {
      final MenuItem menuItem = menu.getItem(i);
      colorMenuItem(menuItem, color);
      if (menuItem.hasSubMenu()) {
        final SubMenu subMenu = menuItem.getSubMenu();
        for (int j = 0; j < subMenu.size(); j++) {
          colorMenuItem(subMenu.getItem(j), color);
        }
      }
    }
  }

  private static void colorMenuItem(final MenuItem menuItem, final int color) {
    colorDrawable(menuItem.getIcon(), color);
  }

  private static Drawable colorDrawable(Drawable drawable, int color) {
    if (drawable != null) {
      drawable.mutate();
      drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
    return drawable;
  }

  public void applyToSystemBars(Activity activity) {
    setStatusBarColor(activity);

    applyToStatusBarIcons(activity);

    applyToNavigationBar(activity);
  }

  public void setStatusBarColor(Activity activity) {
    if (atLeastLollipop()) {
      activity.getWindow().setStatusBarColor(colorPrimaryVariant);
    }
  }

  public void setStatusBarColor(DrawerLayout drawerLayout) {
    if (atLeastLollipop()) {
      drawerLayout.setStatusBarBackgroundColor(colorPrimaryVariant);
    }
    if (atLeastMarshmallow()) {
      int systemUiVisibility = applyLightStatusBarFlag(drawerLayout.getSystemUiVisibility());
      drawerLayout.setSystemUiVisibility(systemUiVisibility);
    }
  }

  public void setStatusBarColor(CollapsingToolbarLayout layout) {
    layout.setContentScrimColor(colorPrimary);
    layout.setStatusBarScrimColor(colorPrimaryVariant);
  }

  public void applyToStatusBarIcons(Activity activity) {
    if (atLeastMarshmallow()) {
      View decorView = activity.getWindow().getDecorView();
      int systemUiVisibility = applyLightStatusBarFlag(decorView.getSystemUiVisibility());
      decorView.setSystemUiVisibility(systemUiVisibility);
    }
  }

  public void applyToNavigationBar(Activity activity) {
    if (atLeastLollipop()) {
      activity.getWindow().setNavigationBarColor(getPrimaryColor());
    }

    if (atLeastOreo()) {
      View decorView = activity.getWindow().getDecorView();
      int systemUiVisibility = applyLightNavigationBar(decorView.getSystemUiVisibility());
      decorView.setSystemUiVisibility(systemUiVisibility);
    }
  }

  @RequiresApi(api = VERSION_CODES.M)
  private int applyLightStatusBarFlag(int flag) {
    return isDark
        ? flag | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        : flag & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
  }

  @RequiresApi(api = VERSION_CODES.O)
  private int applyLightNavigationBar(int flag) {
    return isDark
        ? flag | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        : flag & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
  }

  void applyStyle(Resources.Theme theme) {
    theme.applyStyle(isDark ? R.style.BlackToolbarTheme : R.style.WhiteToolbarTheme, true);
  }

  public void applyTaskDescription(Activity activity, String description) {
    if (atLeastLollipop()) {
      activity.setTaskDescription(
          new ActivityManager.TaskDescription(description, null, getPrimaryColor()));
    }
  }

  @Override
  public int getPickerColor() {
    return colorPrimary;
  }

  @Override
  public boolean isFree() {
    switch (colorPrimary) {
      case -14575885: // blue_500
      case -10453621: // blue_grey_500
      case -14606047: // grey_900
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

  public int getColorOnPrimary() {
    return colorOnPrimary;
  }

  public void apply(Toolbar toolbar) {
    toolbar.setBackgroundColor(getPrimaryColor());
    toolbar.setNavigationIcon(colorDrawable(toolbar.getNavigationIcon(), colorOnPrimary));
    toolbar.setTitleTextColor(colorOnPrimary);
    colorMenu(toolbar.getMenu(), colorOnPrimary);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(index);
    dest.writeInt(colorOnPrimary);
    dest.writeInt(colorPrimary);
    dest.writeInt(colorPrimaryVariant);
    dest.writeInt(isDark ? 1 : 0);
  }

  public void colorMenu(Menu menu) {
    colorMenu(menu, colorOnPrimary);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ThemeColor)) {
      return false;
    }

    ThemeColor that = (ThemeColor) o;

    return colorPrimary == that.colorPrimary;
  }

  @Override
  public int hashCode() {
    return colorPrimary;
  }
}
