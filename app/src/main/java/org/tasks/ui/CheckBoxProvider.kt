package org.tasks.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.R
import org.tasks.themes.ColorProvider
import org.tasks.themes.DrawableUtil
import javax.inject.Inject

class CheckBoxProvider @Inject constructor(
    @param:ActivityContext private val context: Context,
    private val colorProvider: ColorProvider
) {
    fun getCheckBox(task: Task) = getDrawable(task.getCheckboxRes(), task.priority)

    fun getWidgetCheckBox(task: Task): Bitmap {
        val wrapped =
            DrawableUtil.getWrapped(context, task.getCheckboxRes())
        DrawableUtil.setTint(wrapped, colorProvider.getPriorityColor(task.priority, false))
        return convertToBitmap(wrapped)
    }

    private fun getDrawable(@DrawableRes resId: Int, priority: Int): Drawable {
        val original = context.getDrawable(resId)
        val wrapped = original!!.mutate()
        wrapped.setTint(colorProvider.getPriorityColor(priority))
        return wrapped
    }

    private fun convertToBitmap(d: Drawable): Bitmap {
        val bitmap =
            Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bitmap
    }

    companion object {
        fun Task.getCheckboxRes() = when {
            isCompleted -> R.drawable.ic_outline_check_box_24px
            isRecurring -> R.drawable.ic_outline_repeat_24px
            else -> R.drawable.ic_outline_check_box_outline_blank_24px
        }
    }
}
