package com.todoroo.astrid.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.compose.AddReminderDialog
import org.tasks.compose.AlarmRow
import org.tasks.compose.collectAsStateLifecycleAware
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
import org.tasks.reminders.AlarmToString
import org.tasks.ui.TaskEditControlComposeFragment
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReminderControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var alarmToString: AlarmToString

    private val showCustomDialog = mutableStateOf(false)
    private val showRandomDialog = mutableStateOf(false)
    private val ringMode = mutableStateOf(0)

    override fun createView(savedInstanceState: Bundle?) {
        showCustomDialog.value = savedInstanceState?.getBoolean(CUSTOM_DIALOG_VISIBLE) ?: false
        showRandomDialog.value = savedInstanceState?.getBoolean(RANDOM_DIALOG_VISIBLE) ?: false
        when {
            viewModel.ringNonstop!! -> setRingMode(2)
            viewModel.ringFiveTimes!! -> setRingMode(1)
            else -> setRingMode(0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(CUSTOM_DIALOG_VISIBLE, showCustomDialog.value)
        outState.putBoolean(RANDOM_DIALOG_VISIBLE, showRandomDialog.value)
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
        val id = viewModel.task?.id ?: 0
        when (selected) {
            getString(R.string.when_started) ->
                addAlarmRow(whenStarted(id))
            getString(R.string.when_due) ->
                addAlarmRow(whenDue(id))
            getString(R.string.when_overdue) ->
                addAlarmRow(whenOverdue(id))
            getString(R.string.randomly) ->
                addRandomAlarm()
            getString(R.string.pick_a_date_and_time) ->
                addNewAlarm()
            getString(R.string.repeat_option_custom) ->
                addCustomAlarm()
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

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Body() {
        val alarms = viewModel.selectedAlarms.collectAsStateLifecycleAware()
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            alarms.value.forEach { alarm ->
                AlarmRow(alarmToString.toString(alarm)) {
                    viewModel.selectedAlarms.value =
                        viewModel.selectedAlarms.value.minus(alarm)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.add_reminder),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = { addAlarm() }
                        )
                        .alpha(
                            ResourcesCompat.getFloat(
                                LocalContext.current.resources,
                                R.dimen.alpha_disabled
                            )
                        )
                )
                Spacer(modifier = Modifier.weight(1f))
                val ringMode = remember { this@ReminderControlSet.ringMode }
                if (alarms.value.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            id = when (ringMode.value) {
                                2 -> R.string.ring_nonstop
                                1 -> R.string.ring_five_times
                                else -> R.string.ring_once
                            }
                        ),
                        style = MaterialTheme.typography.body1.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = false),
                                onClick = { onClickRingType() }
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val openCustomDialog = remember { showCustomDialog }
        AddReminderDialog.AddCustomReminderDialog(
            openCustomDialog,
            addAlarm = this::addAlarmRow,
            closeDialog = {
                openCustomDialog.value = false
                AndroidUtilities.hideKeyboard(activity)
            }
        )

        val openRandomDialog = remember { showRandomDialog }
        AddReminderDialog.AddRandomReminderDialog(
            openRandomDialog,
            addAlarm = this::addAlarmRow,
            closeDialog = {
                openRandomDialog.value = false
                AndroidUtilities.hideKeyboard(activity)
            }
        )
    }

    override val icon = R.drawable.ic_outline_notifications_24px

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_NEW_ALARM) {
            if (resultCode == Activity.RESULT_OK) {
                val timestamp = data!!.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L)
                addAlarmRow(Alarm(0, timestamp, TYPE_DATE_TIME))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addAlarmRow(alarm: Alarm) {
        with (viewModel.selectedAlarms) {
            if (value.none { it.same(alarm) }) {
                value = value.plus(alarm)
            }
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

    private fun addCustomAlarm() {
        showCustomDialog.value = true
    }

    private fun addRandomAlarm() {
        showRandomDialog.value = true
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
        private const val CUSTOM_DIALOG_VISIBLE = "custom_dialog_visible"
        private const val RANDOM_DIALOG_VISIBLE = "random_dialog_visible"
    }
}

