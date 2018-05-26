package org.tasks.themes;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.support.v7.app.AppCompatDelegate;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import javax.inject.Inject;
import org.tasks.R;

public class Theme {

  private final ThemeBase themeBase;
  private final ThemeColor themeColor;
  private final ThemeAccent themeAccent;

  @Inject
  public Theme(ThemeBase themeBase, ThemeColor themeColor, ThemeAccent themeAccent) {
    this.themeBase = themeBase;
    this.themeColor = themeColor;
    this.themeAccent = themeAccent;
  }

  public Theme withThemeColor(ThemeColor themeColor) {
    return new Theme(themeBase, themeColor, themeAccent);
  }

  public ThemeBase getThemeBase() {
    return themeBase;
  }

  public ThemeColor getThemeColor() {
    return themeColor;
  }

  public ThemeAccent getThemeAccent() {
    return themeAccent;
  }

  public LayoutInflater getLayoutInflater(Context context) {
    return (LayoutInflater) wrap(context).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public ContextThemeWrapper getThemedDialog(Context context) {
    return new ContextThemeWrapper(context, themeBase.getAlertDialogStyle());
  }

  public void applyThemeAndStatusBarColor(Activity activity, AppCompatDelegate delegate) {
    delegate.applyDayNight();
    applyTheme(activity);
    applyStatusBarColor(activity);
  }

  public void applyStatusBarColor(Activity activity) {
    themeColor.applyToStatusBar(activity);
    themeColor.applyTaskDescription(activity, activity.getString(R.string.app_name));
  }

  public void applyTheme(Activity activity) {
    themeBase.set(activity);
    applyToContext(activity);
    activity.getWindow().setFormat(PixelFormat.RGBA_8888);
  }

  public void applyToContext(Context context) {
    Resources.Theme theme = context.getTheme();
    themeColor.applyStyle(theme);
    themeAccent.apply(theme);
  }

  public Context wrap(Context context) {
    ContextThemeWrapper wrapper = themeBase.wrap(context);
    applyToContext(wrapper);
    return wrapper;
  }
}
