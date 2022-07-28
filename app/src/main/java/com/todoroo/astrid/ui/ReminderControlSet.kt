package com.todoroo.astrid.ui

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.AlarmRow
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.Alarm.Companion.TYPE_REL_END
import org.tasks.data.Alarm.Companion.TYPE_REL_START
import org.tasks.data.Alarm.Companion.whenDue
import org.tasks.data.Alarm.Companion.whenOverdue
import org.tasks.data.Alarm.Companion.whenStarted
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.ui.TaskEditControlFragment
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReminderControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var locale: Locale

    private val ringMode = mutableStateOf(0)
    private val vm: ReminderControlSetViewModel by viewModels()

    override fun createView(savedInstanceState: Bundle?) {
        when {
            viewModel.ringNonstop!! -> setRingMode(2)
            viewModel.ringFiveTimes!! -> setRingMode(1)
            else -> setRingMode(0)
        }
    }

    private fun onClickRingType() {
        val modes = resources.getStringArray(R.array.reminder_ring_modes)
        val ringMode = when {
            viewModel.ringNonstop == true -> 2
            viewModel.ringFiveTimes == true -> 1
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

    private fun addAlarm(selected: String) {
        val id = viewModel.task.id
        when (selected) {
            getString(R.string.when_started) ->
                viewModel.addAlarm(whenStarted(id))
            getString(R.string.when_due) ->
                viewModel.addAlarm(whenDue(id))
            getString(R.string.when_overdue) ->
                viewModel.addAlarm(whenOverdue(id))
            getString(R.string.randomly) ->
                vm.showRandomDialog(visible = true)
            getString(R.string.pick_a_date_and_time) ->
                addNewAlarm()
            getString(R.string.repeat_option_custom) ->
                vm.showCustomDialog(visible = true)
        }
    }

    private fun addAlarm() {
        val options = options
        if (options.size == 1) {
            addNewAlarm()
        } else {
            dialogBuilder
                    .newDialog()
                    .setItems(options) { dialog: DialogInterface, which: Int ->
                        addAlarm(options[which])
                        dialog.dismiss()
                    }
                    .show()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    val ringMode by remember { this@ReminderControlSet.ringMode }
                    val notificationPermissions = if (AndroidUtilities.atLeastTiramisu()) {
                        rememberPermissionState(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        null
                    }
                    AlarmRow(
                        locale = locale,
                        alarms = viewModel.selectedAlarms.collectAsStateLifecycleAware().value,
                        permissionStatus = notificationPermissions?.status
                            ?: PermissionStatus.Granted,
                        launchPermissionRequest = {
                            notificationPermissions?.launchPermissionRequest()
                        },
                        ringMode = ringMode,
                        newAlarm = this@ReminderControlSet::addAlarm,
                        addAlarm = viewModel::addAlarm,
                        openRingType = this@ReminderControlSet::onClickRingType,
                        deleteAlarm = {
                            viewModel.selectedAlarms.value = viewModel.selectedAlarms.value.minus(it)
                        }
                    )
                }
            }
        }

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_NEW_ALARM) {
            if (resultCode == Activity.RESULT_OK) {
                val timestamp = data!!.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L)
                viewModel.addAlarm(Alarm(0, timestamp, TYPE_DATE_TIME))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addNewAlarm() {
        val intent = Intent(activity, DateAndTimePickerActivity::class.java)
            .putExtra(
                DateAndTimePickerActivity.EXTRA_TIMESTAMP,
                DateTimeUtils.newDateTime().noon().millis
            )
        startActivityForResult(intent, REQUEST_NEW_ALARM)
    }

    private val options: List<String>
        get() {
            val options: MutableList<String> = ArrayList()
            if (viewModel.selectedAlarms.value.find { it.type == TYPE_REL_START && it.time == 0L } == null) {
                options.add(getString(R.string.when_started))
            }
            if (viewModel.selectedAlarms.value.find { it.type == TYPE_REL_END && it.time == 0L } == null) {
                options.add(getString(R.string.when_due))
            }
            if (viewModel.selectedAlarms.value.find { it.type == TYPE_REL_END && it.time == TimeUnit.HOURS.toMillis(24) } == null) {
                options.add(getString(R.string.when_overdue))
            }
            options.add(getString(R.string.randomly))
            options.add(getString(R.string.pick_a_date_and_time))
            options.add(getString(R.string.repeat_option_custom))
            return options
        }

    companion object {
        const val TAG = R.string.TEA_ctrl_reminders_pref
        private const val REQUEST_NEW_ALARM = 12152
    }
}
