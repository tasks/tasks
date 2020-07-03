package org.tasks.ui

import android.os.Bundle
import android.widget.EditText
import butterknife.BindView
import butterknife.OnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.dialogs.Linkify
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class DescriptionControlSet : TaskEditControlFragment() {
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var preferences: Preferences

    @BindView(R.id.notes)
    lateinit var editText: EditText
    
    override fun createView(savedInstanceState: Bundle?) {
        viewModel.description?.let(editText::setTextKeepState)
        if (preferences.getBoolean(R.string.p_linkify_task_edit, false)) {
            linkify.linkify(editText)
        }
    }

    override val layout = R.layout.control_set_description

    override val icon = R.drawable.ic_outline_notes_24px

    override fun controlId() = TAG

    @OnTextChanged(R.id.notes)
    fun textChanged(text: CharSequence) {
        viewModel.description = text.toString().trim { it <= ' ' }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_notes_pref
    }
}