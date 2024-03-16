package org.tasks.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.R
import org.tasks.themes.ColorProvider
import javax.inject.Inject

class CheckBoxProvider @Inject constructor(
    @param:ActivityContext private val context: Context,
    private val colorProvider: ColorProvider
) {
    fun getCheckBox(task: Task) = getDrawable(task.getCheckboxRes(), task.priority)

    private fun getDrawable(@DrawableRes resId: Int, priority: Int): Drawable {
        val original = AppCompatResources.getDrawable(context, resId)
        val wrapped = original!!.mutate()
        wrapped.setTint(colorProvider.getPriorityColor(priority))
        return wrapped
    }

    companion object {
        fun Task.getCheckboxRes() = when {
            isCompleted -> R.drawable.ic_outline_check_box_24px
            isRecurring -> R.drawable.ic_outline_repeat_24px
            else -> R.drawable.ic_outline_check_box_outline_blank_24px
        }
    }
}
