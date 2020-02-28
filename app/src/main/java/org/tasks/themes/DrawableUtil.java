package org.tasks.themes;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class DrawableUtil {
  public static Drawable getWrapped(Context context, @DrawableRes int resId) {
    return DrawableCompat.wrap(ContextCompat.getDrawable(context, resId).mutate());
  }

  public static void setLeftDrawable(Context context, TextView tv, @DrawableRes int resId) {
    Drawable wrapped = getWrapped(context, resId);
    if (atLeastJellybeanMR1()) {
      tv.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
    } else {
      tv.setCompoundDrawablesWithIntrinsicBounds(wrapped, null, null, null);
    }
  }

  public static Drawable getLeftDrawable(TextView tv) {
    return atLeastJellybeanMR1()
        ? tv.getCompoundDrawablesRelative()[0]
        : tv.getCompoundDrawables()[0];
  }
}
