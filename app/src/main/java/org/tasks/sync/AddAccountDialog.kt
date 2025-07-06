package org.tasks.sync

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.AddAccountDialog
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var theme: Theme
    @Inject lateinit var preferences: Preferences

    private val hasTasksAccount: Boolean
        get() = arguments?.getBoolean(EXTRA_HAS_TASKS_ACCOUNT) ?: false

    private val hasPro: Boolean
        get() = arguments?.getBoolean(EXTRA_HAS_PRO) ?: false

    enum class Platform {
        TASKS_ORG,
        GOOGLE_TASKS,
        TODOIST,
        MICROSOFT,
        DAVX5,
        CALDAV,
        ETESYNC,
        DECSYNC_CC,
        LOCAL,
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setTitle(R.string.choose_synchronization_service)
            .setContent {
                TasksTheme(
                    theme = theme.themeBase.index,
                    primary = theme.themeColor.primaryColor,
                ) {
                    AddAccountDialog(
                        hasTasksAccount = hasTasksAccount,
                        hasPro = hasPro,
                        selected = this::selected
                    )
                }
            }
            .setNeutralButton(R.string.help) { _, _ -> activity?.openUri(R.string.help_url_sync) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun selected(platform: Platform) {
        setFragmentResult(ADD_ACCOUNT, bundleOf(EXTRA_SELECTED to platform))
        dismiss()
    }

    companion object {
        const val ADD_ACCOUNT = "add_account"
        const val EXTRA_SELECTED = "selected"
        private const val EXTRA_HAS_TASKS_ACCOUNT = "extra_has_tasks_account"
        private const val EXTRA_HAS_PRO = "extra_has_pro"

        fun newAccountDialog(
            hasTasksAccount: Boolean,
            hasPro: Boolean,
        ) =
            AddAccountDialog().apply {
                arguments = bundleOf(
                    EXTRA_HAS_TASKS_ACCOUNT to hasTasksAccount,
                    EXTRA_HAS_PRO to hasPro,
                )
            }
    }
}