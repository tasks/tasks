package org.tasks.themes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;
import androidx.annotation.DrawableRes;

public class DrawableUtil {
  public static Drawable getWrapped(Context context, @DrawableRes int resId) {
    return context.getDrawable(resId).mutate();
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

  public static void setTint(Drawable drawable, int tint) {
    (drawable instanceof LayerDrawable ? ((LayerDrawable) drawable).getDrawable(0) : drawable)
        .setTint(tint);
  }
}
