/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.ui.TimeDurationControlSet
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.dialogs.DialogBuilder
import org.tasks.injection.ActivityContext
import org.tasks.injection.FragmentComponent
import org.tasks.themes.Theme
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class TimerControlSet : TaskEditControlFragment() {
    @Inject @ActivityContext lateinit var activity: Context
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var theme: Theme
    
    @BindView(R.id.display_row_edit)
    lateinit var displayEdit: TextView

    @BindView(R.id.timer)
    lateinit var chronometer: Chronometer

    @BindView(R.id.timer_button)
    lateinit var timerButton: ImageView
    
    private lateinit var estimated: TimeDurationControlSet
    private lateinit var elapsed: TimeDurationControlSet
    private var timerStarted: Long = 0
    private var dialog: AlertDialog? = null
    private lateinit var dialogView: View
    private lateinit var callback: TimerControlSetCallback

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val elapsedSeconds: Int
        val estimatedSeconds: Int
        if (savedInstanceState == null) {
            timerStarted = task.timerStart
            elapsedSeconds = task.elapsedSeconds
            estimatedSeconds = task.estimatedSeconds
        } else {
            timerStarted = savedInstanceState.getLong(EXTRA_STARTED)
            elapsedSeconds = savedInstanceState.getInt(EXTRA_ELAPSED)
            estimatedSeconds = savedInstanceState.getInt(EXTRA_ESTIMATED)
        }
        dialogView = inflater.inflate(R.layout.control_set_timers_dialog, null)
        estimated = TimeDurationControlSet(activity, dialogView, R.id.estimatedDuration, theme)
        elapsed = TimeDurationControlSet(activity, dialogView, R.id.elapsedDuration, theme)
        estimated.setTimeDuration(estimatedSeconds)
        elapsed.setTimeDuration(elapsedSeconds)
        refresh()
        return view
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = activity as TimerControlSetCallback
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_ELAPSED, elapsed.timeDurationInSeconds)
        outState.putInt(EXTRA_ESTIMATED, estimated.timeDurationInSeconds)
        outState.putLong(EXTRA_STARTED, timerStarted)
    }

    override fun onRowClick() {
        if (dialog == null) {
            dialog = buildDialog()
        }
        dialog!!.show()
    }

    override val isClickable: Boolean
        get() = true

    private fun buildDialog(): AlertDialog {
        return dialogBuilder
                .newDialog()
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok) { _, _ -> refreshDisplayView() }
                .setOnCancelListener { refreshDisplayView() }
                .create()
    }

    @OnClick(R.id.timer_container)
    fun timerClicked() {
        if (timerActive()) {
            val task = callback.stopTimer()
            elapsed.setTimeDuration(task.elapsedSeconds)
            timerStarted = 0
            chronometer.stop()
            refreshDisplayView()
        } else {
            val task = callback.startTimer()
            timerStarted = task.timerStart
            chronometer.start()
        }
        updateChronometer()
    }

    override val layout: Int
        get() = R.layout.control_set_timers

    override val icon: Int
        get() = R.drawable.ic_outline_timer_24px

    override fun controlId() = TAG

    override fun hasChanges(original: Task): Boolean {
        return (elapsed.timeDurationInSeconds != original.elapsedSeconds
                || estimated.timeDurationInSeconds != original.estimatedSeconds)
    }

    override fun apply(task: Task) {
        task.elapsedSeconds = elapsed.timeDurationInSeconds
        task.estimatedSeconds = estimated.timeDurationInSeconds
    }

    private fun refresh() {
        refreshDisplayView()
        updateChronometer()
    }

    private fun refreshDisplayView() {
        var est: String? = null
        val estimatedSeconds = estimated.timeDurationInSeconds
        if (estimatedSeconds > 0) {
            est = getString(R.string.TEA_timer_est, DateUtils.formatElapsedTime(estimatedSeconds.toLong()))
        }
        var elap: String? = null
        val elapsedSeconds = elapsed.timeDurationInSeconds
        if (elapsedSeconds > 0) {
            elap = getString(R.string.TEA_timer_elap, DateUtils.formatElapsedTime(elapsedSeconds.toLong()))
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
            elapsed += DateUtilities.now() - timerStarted
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

    private fun timerActive(): Boolean {
        return timerStarted > 0
    }

    interface TimerControlSetCallback {
        fun stopTimer(): Task
        fun startTimer(): Task
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_timer_pref
        private const val EXTRA_STARTED = "extra_started"
        private const val EXTRA_ESTIMATED = "extra_estimated"
        private const val EXTRA_ELAPSED = "extra_elapsed"
    }
}