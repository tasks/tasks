/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.ui.TimeDurationControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.TimerRow
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
    
    private lateinit var estimated: TimeDurationControlSet
    private lateinit var elapsed: TimeDurationControlSet
    private var dialog: AlertDialog? = null
    private lateinit var dialogView: View
    private lateinit var callback: TimerControlSetCallback

    override fun createView(savedInstanceState: Bundle?) {
        dialogView = activity.layoutInflater.inflate(R.layout.control_set_timers_dialog, null)
        estimated = TimeDurationControlSet(activity, dialogView, R.id.estimatedDuration, theme)
        elapsed = TimeDurationControlSet(activity, dialogView, R.id.elapsedDuration, theme)
        estimated.setTimeDuration(viewModel.estimatedSeconds.value)
        elapsed.setTimeDuration(viewModel.elapsedSeconds.value)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = activity as TimerControlSetCallback
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
                val task = callback.stopTimer()
                viewModel.elapsedSeconds.value = task.elapsedSeconds
                elapsed.setTimeDuration(task.elapsedSeconds)
                viewModel.timerStarted.value = 0
            } else {
                val task = callback.startTimer()
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

    interface TimerControlSetCallback {
        suspend fun stopTimer(): Task
        suspend fun startTimer(): Task
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_timer_pref
    }
}
