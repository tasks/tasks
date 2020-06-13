package org.tasks.tasklist

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.service.TaskCompleter
import org.tasks.R
import org.tasks.dialogs.Linkify
import org.tasks.injection.ActivityContext
import org.tasks.preferences.Preferences
import org.tasks.preferences.ResourceResolver
import org.tasks.tasklist.TaskViewHolder.ViewHolderCallbacks
import org.tasks.ui.CheckBoxProvider
import org.tasks.ui.ChipProvider
import java.util.*
import javax.inject.Inject

class ViewHolderFactory @Inject constructor(
        @param:ActivityContext private val context: Context,
        private val preferences: Preferences,
        private val chipProvider: ChipProvider,
        private val checkBoxProvider: CheckBoxProvider,
        private val taskCompleter: TaskCompleter,
        private val linkify: Linkify,
        private val locale: Locale) {
    private val textColorSecondary: Int = ResourceResolver.getData(context, android.R.attr.textColorSecondary)
    private val textColorOverdue: Int = context.getColor(R.color.overdue)
    private val fontSize: Int = preferences.fontSize
    private val metrics: DisplayMetrics = context.resources.displayMetrics
    private val background: Int = ResourceResolver.getResourceId(context, R.attr.selectableItemBackground)
    private val selectedColor: Int = ResourceResolver.getData(context, R.attr.colorControlHighlight)
    private val rowPadding: Int = AndroidUtilities.convertDpToPixels(metrics, preferences.getInt(R.string.p_rowPadding, 16))

    fun newHeaderViewHolder(parent: ViewGroup?, callback: (Long) -> Unit) =
            HeaderViewHolder(
                    context,
                    locale,
                    LayoutInflater.from(context).inflate(R.layout.task_adapter_header, parent, false),
                    callback)

    fun newViewHolder(parent: ViewGroup?, callbacks: ViewHolderCallbacks) =
            TaskViewHolder(
                    context as Activity,
                    LayoutInflater.from(context).inflate(R.layout.task_adapter_row, parent, false) as ViewGroup,
                    preferences,
                    fontSize,
                    chipProvider,
                    checkBoxProvider,
                    textColorOverdue,
                    textColorSecondary,
                    taskCompleter,
                    callbacks,
                    metrics,
                    background,
                    selectedColor,
                    rowPadding,
                    linkify,
                    locale)
}