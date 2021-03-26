/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.ui.TimeDurationControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.databinding.ControlSetTimersBinding
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
    
    private lateinit var displayEdit: TextView
    private lateinit var chronometer: Chronometer
    private lateinit var timerButton: ImageView
    
    private lateinit var estimated: TimeDurationControlSet
    private lateinit var elapsed: TimeDurationControlSet
    private var dialog: AlertDialog? = null
    private lateinit var dialogView: View
    private lateinit var callback: TimerControlSetCallback

    override fun createView(savedInstanceState: Bundle?) {
        dialogView = activity.layoutInflater.inflate(R.layout.control_set_timers_dialog, null)
        estimated = TimeDurationControlSet(activity, dialogView, R.id.estimatedDuration, theme)
        elapsed = TimeDurationControlSet(activity, dialogView, R.id.elapsedDuration, theme)
        estimated.setTimeDuration(viewModel.estimatedSeconds!!)
        elapsed.setTimeDuration(viewModel.elapsedSeconds!!)
        refresh()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = activity as TimerControlSetCallback
    }

    override fun onRowClick() {
        if (dialog == null) {
            dialog = buildDialog()
        }
        dialog!!.show()
    }

    private fun buildDialog(): AlertDialog {
        return dialogBuilder
                .newDialog()
                .setView(dialogView)
                .setPositiveButton(R.string.ok) { _, _ -> refreshDisplayView() }
                .setOnCancelListener { refreshDisplayView() }
                .create()
    }

    private fun timerClicked() {
        lifecycleScope.launch {
            if (timerActive()) {
                val task = callback.stopTimer()
                elapsed.setTimeDuration(task.elapsedSeconds)
                viewModel.timerStarted = 0
                chronometer.stop()
                refreshDisplayView()
            } else {
                val task = callback.startTimer()
                viewModel.timerStarted = task.timerStart
                chronometer.start()
            }
            updateChronometer()
        }
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetTimersBinding.inflate(layoutInflater, parent, true).let {
            displayEdit = it.displayRowEdit
            chronometer = it.timer
            timerButton = it.timerButton
            it.timerContainer.setOnClickListener { timerClicked() }
            it.root
        }

    override val icon = R.drawable.ic_outline_timer_24px

    override fun controlId() = TAG

    override val isClickable = true

    private fun refresh() {
        refreshDisplayView()
        updateChronometer()
    }

    private fun refreshDisplayView() {
        var est: String? = null
        viewModel.estimatedSeconds = estimated.timeDurationInSeconds
        if (viewModel.estimatedSeconds!! > 0) {
            est = getString(
                    R.string.TEA_timer_est,
                    DateUtils.formatElapsedTime(viewModel.estimatedSeconds!!.toLong()))
        }
        var elap: String? = null
        viewModel.elapsedSeconds = elapsed.timeDurationInSeconds
        if (viewModel.elapsedSeconds!! > 0) {
            elap = getString(
                    R.string.TEA_timer_elap,
                    DateUtils.formatElapsedTime(viewModel.elapsedSeconds!!.toLong()))
        }
        val toDisplay: String?
        toDisplay = if (!isNullOrEmpty(est) && !isNullOrEmpty(elap)) {
            "$est, $elap" // $NON-NLS-1$
        } else if (!isNullOrEmpty(est)) {
            est
        } else if (!isNullOrEmpty(elap)) {
            elap
        } else {
            null
        }
        displayEdit.text = toDisplay
    }

    private fun updateChronometer() {
        timerButton.setImageResource(
                if (timerActive()) R.drawable.ic_outline_pause_24px else R.drawable.ic_outline_play_arrow_24px)
        var elapsed = elapsed.timeDurationInSeconds * 1000L
        if (timerActive()) {
            chronometer.visibility = View.VISIBLE
            elapsed += DateUtilities.now() - viewModel.timerStarted
            chronometer.base = SystemClock.elapsedRealtime() - elapsed
            if (elapsed > DateUtilities.ONE_DAY) {
                chronometer.onChronometerTickListener = OnChronometerTickListener { cArg: Chronometer ->
                    val t = SystemClock.elapsedRealtime() - cArg.base
                    cArg.text = DateFormat.format("d'd' h:mm", t) // $NON-NLS-1$
                }
            }
            chronometer.start()
        } else {
            chronometer.visibility = View.GONE
            chronometer.stop()
        }
    }

    private fun timerActive() = viewModel.timerStarted > 0

    interface TimerControlSetCallback {
        suspend fun stopTimer(): Task
        suspend fun startTimer(): Task
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_timer_pref
    }
}