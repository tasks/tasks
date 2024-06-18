package com.todoroo.astrid.ui

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.compose.edit.AlarmRow
import org.tasks.compose.rememberReminderPermissionState
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.extensions.Context.openReminderSettings
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.themes.TasksTheme
import org.tasks.ui.TaskEditControlFragment
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ReminderControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var locale: Locale

    private val ringMode = mutableStateOf(0)

    override fun createView(savedInstanceState: Bundle?) {
        when {
            viewModel.ringNonstop -> setRingMode(2)
            viewModel.ringFiveTimes -> setRingMode(1)
            else -> setRingMode(0)
        }
    }

    private fun onClickRingType() {
        val modes = resources.getStringArray(R.array.reminder_ring_modes)
        val ringMode = when {
            viewModel.ringNonstop -> 2
            viewModel.ringFiveTimes -> 1
            else -> 0
        }
        dialogBuilder
                .newDialog()
                .setSingleChoiceItems(modes, ringMode) { dialog: DialogInterface, which: Int ->
                    setRingMode(which)
                    dialog.dismiss()
                }
                .show()
    }

    private fun setRingMode(ringMode: Int) {
        viewModel.ringNonstop = ringMode == 2
        viewModel.ringFiveTimes = ringMode == 1
        this.ringMode.value = ringMode
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                TasksTheme {
                    val ringMode by remember { this@ReminderControlSet.ringMode }
                    val hasReminderPermissions by rememberReminderPermissionState()
                    val notificationPermissions = if (AndroidUtilities.atLeastTiramisu()) {
                        rememberPermissionState(
                            Manifest.permission.POST_NOTIFICATIONS,
                            onPermissionResult = { success ->
                                if (success) {
                                    NotificationSchedulerIntentService.enqueueWork(context)
                                }
                            }
                        )
                    } else {
                        null
                    }
                    val pickDateAndTime =
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                            val data = result.data ?: return@rememberLauncherForActivityResult
                            val timestamp =
                                data.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L)
                            val replace: Alarm? = data.getParcelableExtra(EXTRA_REPLACE)
                            replace?.let { viewModel.removeAlarm(it) }
                            viewModel.addAlarm(Alarm(time = timestamp, type = TYPE_DATE_TIME))
                        }
                    AlarmRow(
                        locale = locale,
                        alarms = viewModel.selectedAlarms.collectAsStateWithLifecycle().value,
                        hasNotificationPermissions = hasReminderPermissions &&
                                (notificationPermissions == null || notificationPermissions.status == PermissionStatus.Granted),
                        fixNotificationPermissions = {
                            if (hasReminderPermissions) {
                                notificationPermissions?.launchPermissionRequest()
                            } else {
                                context.openReminderSettings()
                            }
                        },
                        ringMode = ringMode,
                        addAlarm = viewModel::addAlarm,
                        openRingType = this@ReminderControlSet::onClickRingType,
                        deleteAlarm = viewModel::removeAlarm,
                        pickDateAndTime = { replace ->
                            val timestamp = replace?.takeIf { it.type == TYPE_DATE_TIME }?.time
                                ?: DateTimeUtils.newDateTime().noon().millis
                            pickDateAndTime.launch(
                                Intent(activity, DateAndTimePickerActivity::class.java)
                                    .putExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, timestamp)
                                    .putExtra(EXTRA_REPLACE, replace)
                            )
                        }
                    )
                }
            }
        }

    override fun controlId() = TAG

    companion object {
        val TAG = R.string.TEA_ctrl_reminders_pref
        private const val EXTRA_REPLACE = "extra_replace"
    }
}
