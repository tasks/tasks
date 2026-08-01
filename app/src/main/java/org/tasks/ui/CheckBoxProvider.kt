package org.tasks.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.themes.ColorProvider
import javax.inject.Inject

class CheckBoxProvider @Inject constructor(
    @param:ActivityContext private val context: Context,
    private val colorProvider: ColorProvider
) {
    /**
     * Inflating and mutating a vector drawable is expensive, and the task list asks for one on
     * every row bind. There are only a handful of (icon, priority) combinations, so tinted
     * drawables are cached and shared via their constant state.
     */
    private val cache = mutableMapOf<Long, Drawable>()

    fun getCheckBox(task: Task) = getDrawable(task.getCheckboxRes(), task.priority)

    private fun getDrawable(@DrawableRes resId: Int, priority: Int): Drawable =
        cache
            .getOrPut((resId.toLong() shl 32) or (priority.toLong() and 0xFFFFFFFFL)) {
                val original = AppCompatResources.getDrawable(context, resId)
                val wrapped = original!!.mutate()
                wrapped.setTint(colorProvider.getPriorityColor(priority))
                wrapped
            }
            .let { it.constantState?.newDrawable(context.resources) ?: it }

    companion object {
        fun Task.getCheckboxRes() = getCheckboxRes(isCompleted, isRecurring)

        fun getCheckboxRes(isCompleted: Boolean, isRecurring: Boolean) = when {
            isCompleted -> R.drawable.ic_outline_check_box_24px
            isRecurring -> R.drawable.ic_outline_repeat_24px
            else -> R.drawable.ic_outline_check_box_outline_blank_24px
        }
    }
}
