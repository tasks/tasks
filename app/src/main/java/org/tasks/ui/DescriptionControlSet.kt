package org.tasks.ui

import android.os.Bundle
import android.widget.EditText
import butterknife.BindView
import butterknife.OnTextChanged
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.dialogs.Linkify
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class DescriptionControlSet : TaskEditControlFragment() {
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var preferences: Preferences

    @BindView(R.id.notes)
    lateinit var editText: EditText
    
    private var description: String? = null

    override suspend fun createView(savedInstanceState: Bundle?) {
        description = if (savedInstanceState == null) {
            stripCarriageReturns(task.notes)
        } else {
            savedInstanceState.getString(EXTRA_DESCRIPTION)
        }
        if (!isNullOrEmpty(description)) {
            editText.setTextKeepState(description)
        }
        if (preferences.getBoolean(R.string.p_linkify_task_edit, false)) {
            linkify.linkify(editText)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_DESCRIPTION, description)
    }

    override val layout: Int
        get() = R.layout.control_set_description

    override val icon: Int
        get() = R.drawable.ic_outline_notes_24px

    override fun controlId() = TAG

    @OnTextChanged(R.id.notes)
    fun textChanged(text: CharSequence) {
        description = text.toString().trim { it <= ' ' }
    }

    override suspend fun apply(task: Task) {
        task.notes = description
    }

    override suspend fun hasChanges(original: Task): Boolean {
        return !if (isNullOrEmpty(description)) isNullOrEmpty(original.notes) else description == stripCarriageReturns(original.notes)
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_notes_pref
        private const val EXTRA_DESCRIPTION = "extra_description"
        fun stripCarriageReturns(original: String?): String? {
            return original?.replace("\\r\\n?".toRegex(), "\n")
        }
    }
}