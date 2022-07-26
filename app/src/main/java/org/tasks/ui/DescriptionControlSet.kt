package org.tasks.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.edit.DescriptionRow
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditViewModel.Companion.stripCarriageReturns
import javax.inject.Inject

@AndroidEntryPoint
class DescriptionControlSet : TaskEditControlFragment() {
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var markdownProvider: MarkdownProvider

    private val linkifyEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_linkify_task_edit, false)

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    DescriptionRow(
                        text = viewModel.description.stripCarriageReturns(),
                        onChanged = { text -> viewModel.description = text.toString().trim { it <= ' ' } },
                        linkify = if (linkifyEnabled) linkify else null,
                        markdownProvider = markdownProvider,
                    )
                }
            }
        }

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_notes_pref
    }
}
