package org.tasks.reminders

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.reminders.NotificationActivity.Companion.EXTRA_COMPLETE_LABEL
import org.tasks.reminders.NotificationActivity.Companion.EXTRA_SNOOZE_LABEL
import org.tasks.reminders.NotificationActivity.Companion.EXTRA_READ_ONLY
import javax.inject.Inject

@AndroidEntryPoint
class NotificationDialog : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder

    private var title: String? = null
    private lateinit var handler: NotificationHandler

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        handler = activity as NotificationHandler
        val readOnly = requireArguments().getBoolean(EXTRA_READ_ONLY)

        val completeLabel = requireArguments().getString(EXTRA_COMPLETE_LABEL)
        val snoozeLabel = requireArguments().getString(EXTRA_SNOOZE_LABEL)
        val actions = buildList<Pair<String, () -> Unit>> {
            add(getString(R.string.TAd_actionEditTask) to handler::edit)
            add(snoozeLabel.orEmpty() to handler::snooze)
            if (!readOnly) {
                add(completeLabel.orEmpty() to handler::complete)
            }
        }
        return dialogBuilder
            .newDialog(title)
            .setItems(actions.map { it.first }) { _, which -> actions[which].second() }
            .setNegativeButton(R.string.cancel) { _, _ -> handler.dismiss() }
            .show()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        handler.dismiss()
    }

    fun setTitle(title: String?) {
        this.title = title
        dialog?.setTitle(title)
    }

    interface NotificationHandler {
        fun edit()

        fun snooze()

        fun complete()

        fun dismiss()
    }
}
