package org.tasks.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.injection.ActivityContext
import org.tasks.themes.ColorProvider
import org.tasks.themes.DrawableUtil
import javax.inject.Inject

class CheckBoxProvider @Inject constructor(@param:ActivityContext private val context: Context, private val colorProvider: ColorProvider) {

    fun getCheckBox(task: Task) = getCheckBox(task.isCompleted, task.isRecurring, task.priority)

    fun getCheckBox(complete: Boolean, repeating: Boolean, priority: Int) =
            getDrawable(getDrawableRes(complete, repeating), priority)

    fun getWidgetCheckBox(task: Task): Bitmap {
        val wrapped = DrawableUtil.getWrapped(context, getDrawableRes(task.isCompleted, task.isRecurring))
        DrawableUtil.setTint(wrapped, colorProvider.getPriorityColor(task.priority, false))
        return convertToBitmap(wrapped)
    }

    private fun getDrawableRes(complete: Boolean, repeating: Boolean) = when {
        complete -> R.drawable.ic_outline_check_box_24px
        repeating -> R.drawable.ic_outline_repeat_24px
        else -> R.drawable.ic_outline_check_box_outline_blank_24px
    }

    private fun getDrawable(@DrawableRes resId: Int, priority: Int): Drawable {
        val original = context.getDrawable(resId)
        val wrapped = original!!.mutate()
        wrapped.setTint(colorProvider.getPriorityColor(priority))
        return wrapped
    }

    private fun convertToBitmap(d: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bitmap
    }
}
