package org.tasks.sync

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.compose.SyncAccount
import org.tasks.dialogs.DialogBuilder
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_GOOGLE_TASKS
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_TASKS_ORG
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder

    private val hasTasksAccount: Boolean
        get() = arguments?.getBoolean(EXTRA_HAS_TASKS_ACCOUNT) ?: false

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
                                onClick = {
                                    activity?.startActivityForResult(
                                        Intent(activity, SignInActivity::class.java),
                                        REQUEST_TASKS_ORG
                                    )
                                    dismiss()
                                }
                            )
                        }
                        SyncAccount(
                            title = R.string.gtasks_GPr_header,
                            description = R.string.google_tasks_selection_description,
                            icon = R.drawable.ic_google,
                            onClick = {
                                activity?.startActivityForResult(
                                    Intent(activity, GtasksLoginActivity::class.java),
                                    REQUEST_GOOGLE_TASKS
                                )
                                dismiss()
                            }
                        )
                        SyncAccount(
                            title = R.string.davx5,
                            description = R.string.davx5_selection_description,
                            icon = R.drawable.ic_davx5_icon_green_bg,
                            onClick = {
                                activity?.openUri(R.string.url_davx5)
                                dismiss()
                            }
                        )
                        SyncAccount(
                            title = R.string.caldav,
                            description = R.string.caldav_selection_description,
                            icon = R.drawable.ic_webdav_logo,
                            tint = MaterialTheme.colors.onSurface.copy(
                                alpha = ContentAlpha.medium
                            ),
                            onClick = {
                                activity?.startActivityForResult(
                                    Intent(activity, CaldavAccountSettingsActivity::class.java),
                                    REQUEST_CALDAV_SETTINGS
                                )
                                dismiss()
                            }
                        )
                        SyncAccount(
                            title = R.string.etesync,
                            description = R.string.etesync_selection_description,
                            icon = R.drawable.ic_etesync,
                            onClick = {
                                activity?.startActivityForResult(
                                    Intent(
                                        activity,
                                        EtebaseAccountSettingsActivity::class.java
                                    ),
                                    REQUEST_CALDAV_SETTINGS
                                )
                                dismiss()
                            }
                        )
                        SyncAccount(
                            title = R.string.decsync,
                            description = R.string.decsync_selection_description,
                            icon = R.drawable.ic_decsync,
                            onClick = {
                                activity?.openUri(R.string.url_decsync)
                                dismiss()
                            }
                        )
                    }
                }
            }
            .setNeutralButton(R.string.help) { _, _ -> activity?.openUri(R.string.help_url_sync) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val EXTRA_HAS_TASKS_ACCOUNT = "extra_has_tasks_account"

        fun newAccountDialog(
            targetFragment: Fragment,
            rc: Int,
            hasTasksAccount: Boolean
        ): AddAccountDialog {
            val dialog = AddAccountDialog()
            dialog.arguments = Bundle().apply {
                putBoolean(EXTRA_HAS_TASKS_ACCOUNT, hasTasksAccount)
            }
            dialog.setTargetFragment(targetFragment, rc)
            return dialog
        }
    }
}