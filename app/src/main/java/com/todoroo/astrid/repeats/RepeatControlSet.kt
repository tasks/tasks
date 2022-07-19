/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.repeats.RepeatRuleToString
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.ui.TaskEditControlComposeFragment
import javax.inject.Inject

@AndroidEntryPoint
class RepeatControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var repeatRuleToString: RepeatRuleToString

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_RECURRENCE) {
            if (resultCode == RESULT_OK) {
                viewModel.recurrence.value = data?.getStringExtra(BasicRecurrenceDialog.EXTRA_RRULE)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onDueDateChanged() {
        viewModel.recurrence.value?.takeIf { it.isNotBlank() }?.let { recurrence ->
            val recur = newRecur(recurrence)
            if (recur.frequency == Recur.Frequency.MONTHLY && recur.dayList.isNotEmpty()) {
                val weekdayNum = recur.dayList[0]
                val dateTime = DateTime(this.dueDate)
                val num: Int
                val dayOfWeekInMonth = dateTime.dayOfWeekInMonth
                num = if (weekdayNum.offset == -1 || dayOfWeekInMonth == 5) {
                    if (dayOfWeekInMonth == dateTime.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
                } else {
                    dayOfWeekInMonth
                }
                recur.dayList.let {
                    it.clear()
                    it.add(WeekDay(dateTime.weekDay, num))
                }
                viewModel.recurrence.value = recur.toString()
            }
        }
    }

    override fun createView(savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenResumed {
            viewModel.dueDate.collect {
                onDueDateChanged()
            }
        }
    }

    private val dueDate: Long
        get() = viewModel.dueDate.value.let { if (it > 0) it else currentTimeMillis() }

    override fun onRowClick() {
        BasicRecurrenceDialog.newBasicRecurrenceDialog(
                this, REQUEST_RECURRENCE, viewModel.recurrence.value, dueDate)
                .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
    }

    override val isClickable = true

    @Composable
    override fun Body() {
        RepeatRow(
            recurrence = viewModel.recurrence.collectAsStateLifecycleAware().value?.let {
                repeatRuleToString.toString(it)
            },
            repeatFromCompletion = viewModel.repeatAfterCompletion.collectAsStateLifecycleAware().value,
            onRepeatFromChanged = { viewModel.repeatAfterCompletion.value = it }
        )
    }

    override val icon = R.drawable.ic_outline_repeat_24px

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_repeat_pref
        private const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
        private const val REQUEST_RECURRENCE = 10000
    }
}

@Composable
fun RepeatRow(
    recurrence: String?,
    repeatFromCompletion: Boolean,
    onRepeatFromChanged: (Boolean) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(20.dp))
        if (recurrence.isNullOrBlank()) {
            DisabledText(text = stringResource(id = R.string.repeat_option_does_not_repeat))
        } else {
            Text(
                text = recurrence,
                modifier = Modifier.height(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(text = stringResource(id = R.string.repeats_from))
                Spacer(modifier = Modifier.width(4.dp))
                var expanded by remember { mutableStateOf(false) }
                Text(
                    text = stringResource(
                        id = if (repeatFromCompletion)
                            R.string.repeat_type_completion
                        else
                            R.string.repeat_type_due
                    ),
                    style = MaterialTheme.typography.body1.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    modifier = Modifier.clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onRepeatFromChanged(false)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.repeat_type_due))
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onRepeatFromChanged(true)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.repeat_type_completion))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}