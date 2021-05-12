package org.tasks.themes;

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
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.core.os.ParcelCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import org.tasks.R;
import org.tasks.dialogs.ColorPalettePicker.Pickable;

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
        R.color.grey_900,
        R.color.tomato,
        R.color.red_500,
        R.color.deep_orange_500,
        R.color.tangerine,
        R.color.pumpkin,
        R.color.orange_500,
        R.color.mango,
        R.color.banana,
        R.color.amber_500,
        R.color.citron,
        R.color.yellow_500,
        R.color.lime_500,
        R.color.avocado,
        R.color.light_green_500,
        R.color.pistachio,
        R.color.green_500,
        R.color.basil,
        R.color.teal_500,
        R.color.sage,
        R.color.cyan_500,
        R.color.light_blue_500,
        R.color.peacock,
        R.color.blue_500,
        R.color.cobalt,
        R.color.indigo_500,
        R.color.lavender,
        R.color.wisteria,
        R.color.amethyst,
        R.color.deep_purple_500,
        R.color.grape,
        R.color.purple_500,
        R.color.radicchio,
        R.color.pink_500,
        R.color.cherry_blossom,
        R.color.flamingo,
        R.color.brown_500,
        R.color.graphite,
        R.color.birch,
        R.color.grey_500,
        R.color.blue_grey_500,
        R.color.white_100,
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

  private static final int BLUE = -14575885;
  private static final int WHITE = -1;

  private final int original;
  private final int colorOnPrimary;
  private final int hintOnPrimary;
  private final int colorPrimary;
  private final boolean isDark;

  public ThemeColor(Context context, int color) {
    this(context, color, color);
  }

  public ThemeColor(Context context, int original, int color) {
    this.original = original;
    if (color == 0) {
      color = BLUE;
    } else {
      color |= 0xFF000000; // remove alpha
    }
    colorPrimary = color;

    double contrast = ColorUtils.calculateContrast(WHITE, colorPrimary);
    isDark = contrast < 3;
    if (isDark) {
      colorOnPrimary = context.getColor(R.color.black_87);
      hintOnPrimary = context.getColor(R.color.black_60);
    } else {
      colorOnPrimary = WHITE;
      hintOnPrimary = context.getColor(R.color.white_60);
    }
  }

  private ThemeColor(Parcel source) {
    colorOnPrimary = source.readInt();
    colorPrimary = source.readInt();
    isDark = ParcelCompat.readBoolean(source);
    original = source.readInt();
    hintOnPrimary = source.readInt();
  }

  public static ThemeColor getLauncherColor(Context context, int index) {
    return new ThemeColor(context, context.getColor(LAUNCHER_COLORS[index]));
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
    activity.getWindow().setStatusBarColor(colorPrimary);
  }

  public void setStatusBarColor(DrawerLayout drawerLayout) {
    drawerLayout.setStatusBarBackgroundColor(colorPrimary);
    int systemUiVisibility = applyLightStatusBarFlag(drawerLayout.getSystemUiVisibility());
    drawerLayout.setSystemUiVisibility(systemUiVisibility);
  }

  public void setStatusBarColor(CollapsingToolbarLayout layout) {
    layout.setContentScrimColor(colorPrimary);
    layout.setStatusBarScrimColor(colorPrimary);
  }

  public void apply(CollapsingToolbarLayout layout, Toolbar toolbar) {
    setStatusBarColor(layout);
    layout.setBackgroundColor(colorPrimary);
    layout.setCollapsedTitleTextColor(colorOnPrimary);
    layout.setExpandedTitleColor(colorOnPrimary);
    toolbar.setNavigationIcon(colorDrawable(toolbar.getNavigationIcon(), colorOnPrimary));
    colorMenu(toolbar.getMenu(), colorOnPrimary);
  }

  public void applyToStatusBarIcons(Activity activity) {
    View decorView = activity.getWindow().getDecorView();
    int systemUiVisibility = applyLightStatusBarFlag(decorView.getSystemUiVisibility());
    decorView.setSystemUiVisibility(systemUiVisibility);
  }

  public void applyToNavigationBar(Activity activity) {
    activity.getWindow().setNavigationBarColor(getPrimaryColor());

    if (atLeastOreo()) {
      View decorView = activity.getWindow().getDecorView();
      int systemUiVisibility = applyLightNavigationBar(decorView.getSystemUiVisibility());
      decorView.setSystemUiVisibility(systemUiVisibility);
    }
  }

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
    activity.setTaskDescription(
        new ActivityManager.TaskDescription(description, null, getPrimaryColor()));
  }

  @Override
  public int getPickerColor() {
    return colorPrimary;
  }

  @Override
  public boolean isFree() {
    switch (original) {
      case -14575885: // blue_500
      case -10453621: // blue_grey_500
      case -14606047: // grey_900
        return true;
      default:
        return false;
    }
  }

  public int getOriginalColor() {
    return original;
  }

  @ColorInt
  public int getPrimaryColor() {
    return colorPrimary;
  }

  public int getColorOnPrimary() {
    return colorOnPrimary;
  }

  public int getHintOnPrimary() {
    return hintOnPrimary;
  }

  public boolean isDark() {
    return isDark;
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
    dest.writeInt(colorOnPrimary);
    dest.writeInt(colorPrimary);
    ParcelCompat.writeBoolean(dest, isDark);
    dest.writeInt(original);
    dest.writeInt(hintOnPrimary);
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

    return original == that.original;
  }

  @Override
  public int hashCode() {
    return original;
  }
}
