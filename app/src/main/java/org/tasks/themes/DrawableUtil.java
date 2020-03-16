package org.tasks.themes;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import org.jetbrains.annotations.NotNull;

public class DrawableUtil {
  public static Drawable getWrapped(Context context, @DrawableRes int resId) {
    return wrap(ContextCompat.getDrawable(context, resId));
  }

  public static void setLeftDrawable(Context context, TextView tv, @DrawableRes int resId) {
    Drawable wrapped = getWrapped(context, resId);
    if (atLeastJellybeanMR1()) {
      tv.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
    } else {
      tv.setCompoundDrawablesWithIntrinsicBounds(wrapped, null, null, null);
    }
  }

  public static void setRightDrawable(Context context, TextView tv, @DrawableRes int resId) {
    Drawable wrapped = getWrapped(context, resId);
    if (atLeastJellybeanMR1()) {
      tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, wrapped, null);
    } else {
      tv.setCompoundDrawablesWithIntrinsicBounds(null, null, wrapped, null);
    }
  }

  public static Drawable getLeftDrawable(TextView tv) {
    return atLeastJellybeanMR1()
        ? tv.getCompoundDrawablesRelative()[0]
        : tv.getCompoundDrawables()[0];
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
