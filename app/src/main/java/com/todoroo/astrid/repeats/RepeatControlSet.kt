/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.R
import org.tasks.compose.edit.RepeatRow
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.filters.CaldavFilter
import org.tasks.filters.GtasksFilter
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.repeats.RepeatRuleToString
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
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
                viewModel.recurrence.value = result
                if (result?.isNotBlank() == true && viewModel.dueDate.value == 0L) {
                    viewModel.setDueDate(currentTimeMillis().startOfDay())
                }
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

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                TasksTheme {
                    RepeatRow(
                        recurrence = viewModel.recurrence.collectAsStateWithLifecycle().value?.let {
                            repeatRuleToString.toString(it)
                        },
                        repeatAfterCompletion = viewModel.repeatAfterCompletion.collectAsStateWithLifecycle().value,
                        onClick = {
                            lifecycleScope.launch {
                                val accountType = viewModel.selectedList.value
                                    .let {
                                        when (it) {
                                            is CaldavFilter -> it.account
                                            is GtasksFilter -> it.account
                                            else -> null
                                        }
                                    }
                                    ?.let { caldavDao.getAccountByUuid(it) }
                                    ?.accountType
                                    ?: CaldavAccount.TYPE_LOCAL
                                BasicRecurrenceDialog.newBasicRecurrenceDialog(
                                    target = this@RepeatControlSet,
                                    rc = REQUEST_RECURRENCE,
                                    rrule = viewModel.recurrence.value,
                                    dueDate = viewModel.dueDate.value,
                                    accountType = accountType,
                                )
                                    .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
                            }
                        },
                        onRepeatFromChanged = { viewModel.repeatAfterCompletion.value = it }
                    )
                }
            }
        }

    override fun controlId() = TAG

    companion object {
        val TAG = R.string.TEA_ctrl_repeat_pref
        private const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
        private const val REQUEST_RECURRENCE = 10000
    }
}
