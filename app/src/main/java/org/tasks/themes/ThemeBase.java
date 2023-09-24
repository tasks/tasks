package org.tasks.themes;

import static org.tasks.extensions.Context.INSTANCE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.ContextThemeWrapper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.preferences.Preferences;

public class ThemeBase implements Parcelable {

  public static final String EXTRA_THEME_OVERRIDE = "extra_theme_override";
  public static final int DEFAULT_BASE_THEME = 5;
  public static final Parcelable.Creator<ThemeBase> CREATOR =
      new Parcelable.Creator<>() {
        @Override
        public ThemeBase createFromParcel(Parcel source) {
          return new ThemeBase(source);
        }

        @Override
        public ThemeBase[] newArray(int size) {
          return new ThemeBase[size];
        }
      };
  private static final int[] NIGHT_MODE =
      new int[] {
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_AUTO_TIME,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      };
  private static final int[] THEMES =
      new int[] {
        R.style.Tasks,
        R.style.ThemeBlack,
        R.style.Tasks,
        R.style.Wallpaper,
        R.style.Tasks,
        R.style.Tasks
      };
  private final int index;

  public static ThemeBase getThemeBase(
      Preferences preferences, Inventory inventory, @Nullable Intent intent) {
    if (intent != null && intent.hasExtra(EXTRA_THEME_OVERRIDE)) {
      return new ThemeBase(intent.getIntExtra(EXTRA_THEME_OVERRIDE, ThemeBase.DEFAULT_BASE_THEME));
    }
    ThemeBase themeBase = new ThemeBase(preferences.getThemeBase());
    return themeBase.isFree() || inventory.purchasedThemes()
        ? themeBase
        : new ThemeBase(ThemeBase.DEFAULT_BASE_THEME);
  }

  public ThemeBase(int index) {
    this.index = index;
  }

  private ThemeBase(Parcel source) {
    index = source.readInt();
  }

  public boolean isFree() {
    return index < 3 || index == 5;
  }

  public int getIndex() {
    return index;
  }

  public boolean isDarkTheme(Activity activity) {
    return index == 4 || index == 5 ? INSTANCE.isNightMode(activity) : index > 0;
  }

  public ContextThemeWrapper wrap(Context context) {
    return new ContextThemeWrapper(context, THEMES[index]);
  }

  public void set(Activity activity) {
    activity.setTheme(THEMES[index]);
  }

  public void setDefaultNightMode() {
    AppCompatDelegate.setDefaultNightMode(NIGHT_MODE[index]);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(index);
  }
}
