package org.tasks.tasklist

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.R
import org.tasks.databinding.TaskAdapterRowBinding
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Preferences
import org.tasks.preferences.ResourceResolver
import org.tasks.tasklist.TaskViewHolder.ViewHolderCallbacks
import org.tasks.ui.CheckBoxProvider
import org.tasks.ui.ChipProvider
import java.util.Locale
import javax.inject.Inject

class ViewHolderFactory @Inject constructor(
        @param:ActivityContext private val context: Context,
        private val preferences: Preferences,
        private val chipProvider: ChipProvider,
        private val checkBoxProvider: CheckBoxProvider,
        private val linkify: Linkify,
        private val locale: Locale,
        private val headerFormatter: HeaderFormatter,
) {
    private val textColorSecondary: Int = ResourceResolver.getData(context, android.R.attr.textColorSecondary)
    private val textColorOverdue: Int = context.getColor(R.color.overdue)
    private val fontSize: Int = preferences.fontSize
    private val metrics: DisplayMetrics = context.resources.displayMetrics
    private val background: Int = ResourceResolver.getResourceId(context, androidx.appcompat.R.attr.selectableItemBackground)
    private val selectedColor: Int = ResourceResolver.getData(context, androidx.appcompat.R.attr.colorControlHighlight)
    private val rowPaddingDp = preferences.getInt(R.string.p_rowPadding, 16)
    private val rowPaddingPx: Int = AndroidUtilities.convertDpToPixels(metrics, rowPaddingDp)
    private val markdown =
        MarkdownProvider(context, preferences).markdown(R.string.p_linkify_task_list)

    fun newHeaderViewHolder(parent: ViewGroup?, callback: (Long) -> Unit) =
            HeaderViewHolder(
                    context,
                    headerFormatter,
                    LayoutInflater.from(context).inflate(R.layout.task_adapter_header, parent, false),
                    callback,
            )

    fun newViewHolder(parent: ViewGroup?, callbacks: ViewHolderCallbacks) =
            TaskViewHolder(
                    context as Activity,
                    TaskAdapterRowBinding.inflate(LayoutInflater.from(context), parent, false),
                    preferences,
                    fontSize,
                    chipProvider,
                    checkBoxProvider,
                    textColorOverdue,
                    textColorSecondary,
                    callbacks,
                    metrics,
                    background,
                    selectedColor,
                    rowPaddingDp,
                    rowPaddingPx,
                    linkify,
                    locale,
                    markdown
            )
}