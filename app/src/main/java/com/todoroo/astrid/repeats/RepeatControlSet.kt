/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.R
import org.tasks.compose.edit.RepeatRow
import org.tasks.data.dao.CaldavDao
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.repeats.RepeatRuleToString
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class RepeatControlSet : TaskEditControlFragment() {
    @Inject lateinit var repeatRuleToString: RepeatRuleToString
    @Inject lateinit var caldavDao: CaldavDao

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_RECURRENCE) {
            if (resultCode == RESULT_OK) {
                val result = data?.getStringExtra(BasicRecurrenceDialog.EXTRA_RRULE)
                viewModel.setRecurrence(result)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onDueDateChanged() {
        // TODO: move to view model
        viewModel.viewState.value.task.recurrence?.takeIf { it.isNotBlank() }?.let { recurrence ->
            val recur = newRecur(recurrence)
            if (recur.frequency == Recur.Frequency.MONTHLY && recur.dayList.isNotEmpty()) {
                val weekdayNum = recur.dayList[0]
                val dateTime =
                    DateTime(this.viewModel.dueDate.value.let { if (it > 0) it else currentTimeMillis() })
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
                viewModel.setRecurrence(recur.toString())
            }
        }
    }

    @Composable
    override fun Content() {
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
        val dueDate = viewModel.dueDate.collectAsStateWithLifecycle().value
        LaunchedEffect(dueDate) {
            onDueDateChanged()
        }
        RepeatRow(
            recurrence = viewState.task.recurrence?.let { repeatRuleToString.toString(it) },
            repeatFrom = viewState.task.repeatFrom,
            onClick = {
                val accountType = viewState.list.account.accountType
                BasicRecurrenceDialog.newBasicRecurrenceDialog(
                    target = this@RepeatControlSet,
                    rc = REQUEST_RECURRENCE,
                    rrule = viewState.task.recurrence,
                    dueDate = dueDate,
                    accountType = accountType,
                )
                    .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
            },
            onRepeatFromChanged = { viewModel.setRepeatFrom(it) }
        )
    }

    companion object {
        val TAG = R.string.TEA_ctrl_repeat_pref
        private const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
        private const val REQUEST_RECURRENCE = 10000
    }
}
