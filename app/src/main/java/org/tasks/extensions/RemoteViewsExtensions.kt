package org.tasks.extensions

import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.ColorUtils
import org.tasks.R

fun RemoteViews.setColorFilter(viewId: Int, @ColorInt color: Int) =
    setInt(viewId, "setColorFilter", color)

fun RemoteViews.setBackgroundColor(viewId: Int, color: Int, opacity: Int) =
    setInt(viewId, "setBackgroundColor", ColorUtils.setAlphaComponent(color, opacity))

fun RemoteViews.setBackgroundResource(viewId: Int, @DrawableRes resId: Int) =
    setInt(viewId, "setBackgroundResource", resId)

fun RemoteViews.setTextSize(viewId: Int, size: Float) =
    setFloat(viewId, "setTextSize", size)

fun RemoteViews.strikethrough(viewId: Int, strikethrough: Boolean) =
    setInt(
        viewId,
        "setPaintFlags",
        if (strikethrough) STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG else ANTI_ALIAS_FLAG
    )

fun RemoteViews.setMaxLines(viewId: Int, maxLines: Int) =
    setInt(viewId, "setMaxLines", maxLines)

fun RemoteViews.setRipple(viewId: Int, dark: Boolean) =
    setBackgroundResource(
        viewId,
        if (dark) R.drawable.widget_ripple_circle_light else R.drawable.widget_ripple_circle_dark
    )
