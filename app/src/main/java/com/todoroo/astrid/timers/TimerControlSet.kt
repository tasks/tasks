/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import android.app.Activity
import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.ui.TimeDurationControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.edit.TimerRow
import org.tasks.data.entity.Task
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@AndroidEntryPoint
class TimerControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var timerPlugin: TimerPlugin
    
    private lateinit var estimated: TimeDurationControlSet
    private lateinit var elapsed: TimeDurationControlSet
    private var dialog: AlertDialog? = null
    private lateinit var dialogView: View

    private fun onRowClick() {
        if (dialog == null) {
            dialog = buildDialog()
        }
        dialog!!.show()
    }

    private fun buildDialog(): AlertDialog {
        return dialogBuilder
                .newDialog()
                .setView(dialogView)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.estimatedSeconds.value = estimated.timeDurationInSeconds
                    viewModel.elapsedSeconds.value = elapsed.timeDurationInSeconds
                }
                .setOnCancelListener {}
                .create()
    }

    private fun timerClicked() {
        lifecycleScope.launch {
            if (timerActive()) {
                val task = stopTimer()
                viewModel.elapsedSeconds.value = task.elapsedSeconds
                elapsed.setTimeDuration(task.elapsedSeconds)
                viewModel.timerStarted.value = 0
            } else {
                val task = startTimer()
                viewModel.timerStarted.value = task.timerStart
            }
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            dialogView = activity.layoutInflater.inflate(R.layout.control_set_timers_dialog, null)
            estimated = TimeDurationControlSet(activity, dialogView, R.id.estimatedDuration)
            elapsed = TimeDurationControlSet(activity, dialogView, R.id.elapsedDuration)
            estimated.setTimeDuration(viewModel.estimatedSeconds.value)
            elapsed.setTimeDuration(viewModel.elapsedSeconds.value)
        }
        TimerRow(
            started = viewModel.timerStarted.collectAsStateWithLifecycle().value,
            estimated = viewModel.estimatedSeconds.collectAsStateWithLifecycle().value,
            elapsed = viewModel.elapsedSeconds.collectAsStateWithLifecycle().value,
            timerClicked = this@TimerControlSet::timerClicked,
            onClick = this@TimerControlSet::onRowClick,
        )
    }

    private fun timerActive() = viewModel.timerStarted.value > 0

    private suspend fun stopTimer(): Task {
        val model = viewModel.viewState.value.task
        timerPlugin.stopTimer(model)
        val elapsedTime = DateUtils.formatElapsedTime(model.elapsedSeconds.toLong())
        viewModel.addComment(String.format(
            "%s %s\n%s %s",  // $NON-NLS-1$
            getString(R.string.TEA_timer_comment_stopped),
            getTimeString(currentTimeMillis(), requireContext().is24HourFormat),
            getString(R.string.TEA_timer_comment_spent),
            elapsedTime
        ),
            null)
        return model
    }

    private suspend fun startTimer(): Task {
        val model = viewModel.viewState.value.task
        timerPlugin.startTimer(model)
        viewModel.addComment(String.format(
            "%s %s",
            getString(R.string.TEA_timer_comment_started),
            getTimeString(currentTimeMillis(), requireContext().is24HourFormat)
        ),
            null)
        return model
    }

    companion object {
        val TAG = R.string.TEA_ctrl_timer_pref
    }
}
