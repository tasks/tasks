/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import android.app.Activity
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.ui.TimeDurationControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.TimerRow
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DialogBuilder
import org.tasks.themes.Theme
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
    @Inject lateinit var theme: Theme
    @Inject lateinit var timerPlugin: TimerPlugin
    
    private lateinit var estimated: TimeDurationControlSet
    private lateinit var elapsed: TimeDurationControlSet
    private var dialog: AlertDialog? = null
    private lateinit var dialogView: View

    override fun createView(savedInstanceState: Bundle?) {
        dialogView = activity.layoutInflater.inflate(R.layout.control_set_timers_dialog, null)
        estimated = TimeDurationControlSet(activity, dialogView, R.id.estimatedDuration, theme)
        elapsed = TimeDurationControlSet(activity, dialogView, R.id.elapsedDuration, theme)
        estimated.setTimeDuration(viewModel.estimatedSeconds.value)
        elapsed.setTimeDuration(viewModel.elapsedSeconds.value)
    }

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

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    TimerRow(
                        started = viewModel.timerStarted.collectAsStateLifecycleAware().value,
                        estimated = viewModel.estimatedSeconds.collectAsStateLifecycleAware().value,
                        elapsed = viewModel.elapsedSeconds.collectAsStateLifecycleAware().value,
                        timerClicked = this@TimerControlSet::timerClicked,
                        onClick = this@TimerControlSet::onRowClick,
                    )
                }
            }
        }

    override fun controlId() = TAG

    private fun timerActive() = viewModel.timerStarted.value > 0

    private suspend fun stopTimer(): Task {
        val model = viewModel.task
        timerPlugin.stopTimer(model)
        val elapsedTime = DateUtils.formatElapsedTime(model.elapsedSeconds.toLong())
        viewModel.addComment(String.format(
            "%s %s\n%s %s",  // $NON-NLS-1$
            getString(R.string.TEA_timer_comment_stopped),
            DateUtilities.getTimeString(context, DateTimeUtils.newDateTime()),
            getString(R.string.TEA_timer_comment_spent),
            elapsedTime),
            null)
        return model
    }

    private suspend fun startTimer(): Task {
        val model = viewModel.task
        timerPlugin.startTimer(model)
        viewModel.addComment(String.format(
            "%s %s",
            getString(R.string.TEA_timer_comment_started),
            DateUtilities.getTimeString(context, DateTimeUtils.newDateTime())),
            null)
        return model
    }

    companion object {
        val TAG = R.string.TEA_ctrl_timer_pref
    }
}
