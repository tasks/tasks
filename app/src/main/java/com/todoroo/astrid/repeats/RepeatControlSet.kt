/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.edit.RepeatRow
import org.tasks.data.dao.CaldavDao
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.RepeatRuleToString
import org.tasks.repeats.anchoredToDueDate
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class RepeatControlSet : TaskEditControlFragment() {
    @Inject lateinit var repeatRuleToString: RepeatRuleToString
    @Inject lateinit var caldavDao: CaldavDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            BasicRecurrenceDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val result = bundle.getString(BasicRecurrenceDialog.EXTRA_RRULE)
            viewModel.setRecurrence(result)
        }
    }

    private fun onDueDateChanged() {
        // TODO: move to view model
        viewModel.viewState.value.task.recurrence?.takeIf { it.isNotBlank() }?.let { recurrence ->
            val anchored = recurrence.anchoredToDueDate(viewModel.dueDate.value)
            if (anchored != recurrence) {
                viewModel.setRecurrence(anchored)
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
            recurrence = viewState.task.recurrence?.let { repeatRuleToString.toStringBlocking(it) },
            repeatFrom = viewState.task.repeatFrom,
            onClick = {
                val accountType = viewState.list.account.accountType
                BasicRecurrenceDialog.newBasicRecurrenceDialog(
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
    }
}
