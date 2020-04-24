package org.tasks.themes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.core.graphics.drawable.DrawableCompat;
import org.jetbrains.annotations.NotNull;

public class DrawableUtil {
  public static Drawable getWrapped(Context context, @DrawableRes int resId) {
    return wrap(context.getDrawable(resId));
  }

  public static void setLeftDrawable(Context context, TextView tv, @DrawableRes int resId) {
    Drawable wrapped = getWrapped(context, resId);
    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
  }

  public static void setRightDrawable(Context context, TextView tv, @DrawableRes int resId) {
    Drawable wrapped = getWrapped(context, resId);
    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, wrapped, null);
  }

  public static Drawable getLeftDrawable(TextView tv) {
    return tv.getCompoundDrawablesRelative()[0];
  }

  public static Drawable wrap(@NotNull Drawable icon) {
    return DrawableCompat.wrap(icon.mutate());
  }

  public static void setTint(Drawable drawable, int tint) {
    DrawableCompat.setTint(
        drawable instanceof LayerDrawable ? ((LayerDrawable) drawable).getDrawable(0) : drawable,
        tint);
  }
}
