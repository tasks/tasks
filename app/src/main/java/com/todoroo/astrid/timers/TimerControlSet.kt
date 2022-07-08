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
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.ui.TimeDurationControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.dialogs.DialogBuilder
import org.tasks.themes.Theme
import org.tasks.ui.TaskEditControlComposeFragment
import java.lang.System.currentTimeMillis
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@AndroidEntryPoint
class TimerControlSet : TaskEditControlComposeFragment() {
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

    @Composable
    override fun Body() {
        var now by remember { mutableStateOf(currentTimeMillis()) }
        val started = viewModel.timerStarted.collectAsStateLifecycleAware().value
        val newElapsed = if (started > 0) (now - started) / 1000L else 0
        val estimated =
            viewModel.estimatedSeconds.collectAsStateLifecycleAware().value.takeIf { it > 0 }
                ?.let {
                    stringResource(id = R.string.TEA_timer_est, DateUtils.formatElapsedTime(it.toLong()))
                }
        val elapsed =
            viewModel.elapsedSeconds.collectAsStateLifecycleAware().value.takeIf { it > 0 }
                ?.let {
                    stringResource(id = R.string.TEA_timer_elap, DateUtils.formatElapsedTime(it + newElapsed))
                }
        val text = when {
            estimated != null && elapsed != null -> "$estimated, $elapsed"
            estimated != null -> estimated
            elapsed != null -> elapsed
            else -> null
        }
        Row {
            if (text == null) {
                DisabledText(
                    text = stringResource(id = R.string.TEA_timer_controls),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 20.dp),
                )
            } else {
                Text(
                    text = text,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 20.dp),
                )
            }
            IconButton(
                onClick = {
                    now = currentTimeMillis()
                    timerClicked()
                },
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Icon(
                    imageVector = if (started > 0) {
                        Icons.Outlined.Pause
                    } else {
                        Icons.Outlined.PlayArrow
                    },
                    modifier = Modifier.alpha(ContentAlpha.medium),
                    contentDescription = null
                )
            }
        }
        LaunchedEffect(key1 = started) {
            while (started > 0) {
                delay(1.seconds)
                now = currentTimeMillis()
            }
        }
    }

    override val icon = R.drawable.ic_outline_timer_24px

    override fun controlId() = TAG

    override val isClickable = true

    private fun timerActive() = viewModel.timerStarted.value > 0

    interface TimerControlSetCallback {
        suspend fun stopTimer(): Task
        suspend fun startTimer(): Task
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_timer_pref
    }
}