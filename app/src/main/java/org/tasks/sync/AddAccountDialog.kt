package org.tasks.sync

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.compose.SyncAccount
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.openUri
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder

    private val hasTasksAccount: Boolean
        get() = arguments?.getBoolean(EXTRA_HAS_TASKS_ACCOUNT) ?: false

    enum class Platform {
        TASKS_ORG,
        GOOGLE_TASKS,
        MICROSOFT,
        DAVX5,
        CALDAV,
        ETESYNC,
        DECSYNC_CC
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setTitle(R.string.choose_synchronization_service)
            .setContent {
                MdcTheme {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (!hasTasksAccount) {
                            SyncAccount(
                                title = R.string.tasks_org,
                                description = R.string.tasks_org_description,
                                icon = R.drawable.ic_round_icon,
                                onClick = { selected(Platform.TASKS_ORG) }
                            )
                        }
                        SyncAccount(
                            title = R.string.gtasks_GPr_header,
                            description = R.string.google_tasks_selection_description,
                            icon = R.drawable.ic_google,
                            onClick = { selected(Platform.GOOGLE_TASKS) }
                        )
                        if (BuildConfig.DEBUG) {
                            SyncAccount(
                                title = R.string.microsoft,
                                description = R.string.microsoft_selection_description,
                                icon = R.drawable.ic_microsoft_tasks,
                                onClick = { selected(Platform.MICROSOFT) }
                            )
                        }
                        SyncAccount(
                            title = R.string.davx5,
                            description = R.string.davx5_selection_description,
                            icon = R.drawable.ic_davx5_icon_green_bg,
                            onClick = { selected(Platform.DAVX5) }
                        )
                        SyncAccount(
                            title = R.string.caldav,
                            description = R.string.caldav_selection_description,
                            icon = R.drawable.ic_webdav_logo,
                            tint = MaterialTheme.colors.onSurface.copy(
                                alpha = ContentAlpha.medium
                            ),
                            onClick = { selected(Platform.CALDAV) }
                        )
                        SyncAccount(
                            title = R.string.etesync,
                            description = R.string.etesync_selection_description,
                            icon = R.drawable.ic_etesync,
                            onClick = { selected(Platform.ETESYNC) }
                        )
                        SyncAccount(
                            title = R.string.decsync,
                            description = R.string.decsync_selection_description,
                            icon = R.drawable.ic_decsync,
                            onClick = { selected(Platform.DECSYNC_CC) }
                        )
                    }
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

        fun newAccountDialog(hasTasksAccount: Boolean) =
            AddAccountDialog().apply {
                arguments = bundleOf(EXTRA_HAS_TASKS_ACCOUNT to hasTasksAccount)
            }
    }
}