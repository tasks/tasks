package org.tasks.repeats

/* This is mostly a copy of the BasicRecurrenceDialog with UI made @Composable  */

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.common.collect.Lists
import net.fortuna.ical4j.model.Recur
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.compose.Constants.TextButton
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import org.tasks.repeats.RecurrenceUtils.newRecur
import timber.log.Timber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicRecurrencePicker (
    dismiss: () -> Unit,
    recurrence: String?,
    setRecurrence: (String?) -> Unit,
    peekCustomRecurrence: () -> Unit,
    repeatFrom: @Task.RepeatFrom Int = Task.RepeatFrom.COMPLETION_DATE,
    onRepeatFromChanged: ((@Task.RepeatFrom Int) -> Unit)? = null,
) {

    val context = LocalContext.current
    val helper = remember { RecurrenceHelper(context) }
    helper.setRecurrence(recurrence)

    fun setSelection(index: Int) {
        when (index) {
            0 -> setRecurrence(null)
            5 -> {
                peekCustomRecurrence()
                return // to avoid dismiss() call
            }
            6 -> Unit
            else -> {
                setRecurrence(
                    newRecur().apply {
                        interval = 1
                        setFrequency(helper.selectedFrequency(index).name)
                    }.toString()
                )
            }
        }
        dismiss()
    }

    BasicAlertDialog(
        onDismissRequest = dismiss,
    ) {
        Card {
            Column (modifier = Modifier.padding(16.dp)) {
                onRepeatFromChanged?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row (modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp)){
                        Text(
                            text = stringResource(id = R.string.repeats_from),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Text(
                            text = stringResource(
                                id = if (repeatFrom == Task.RepeatFrom.COMPLETION_DATE)
                                    R.string.repeat_type_completion
                                else
                                    R.string.repeat_type_due
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = TextDecoration.Underline,
                            ),
                            modifier = Modifier.clickable { expanded = true },
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    onRepeatFromChanged(Task.RepeatFrom.DUE_DATE)
                                },
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.repeat_type_due),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    onRepeatFromChanged(Task.RepeatFrom.COMPLETION_DATE)
                                },
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.repeat_type_completion),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (helper.isCustomValue()) {
                    SelectableText(
                        text = helper.repeatRuleToString.toString(recurrence)!!,
                        index = 6,
                        selected = 6,
                        setSelection = { setSelection(6) }
                    )
                }
                for (i in 0..5) {
                    SelectableText(
                        text = helper.title(i),
                        index = i,
                        selected = helper.selectionIndex(),
                        setSelection = { setSelection(i) }
                    )
                }
                Row (
                    modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(R.string.ok, dismiss)
                }
            }
        }
    }
}

@Composable
fun SelectableText (
    text: String,
    index: Int,
    selected: Int,
    setSelection: (Int) -> Unit
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable{ setSelection(index) }
    ) {
        RadioButton(
            selected = index == selected,
            onClick = { setSelection(index) }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, textDecoration = TextDecoration.Underline)
    }
}

/**
 *  Helper object over recurrence string, to share basic access to rrule and to avoid reloading
 *  R.array.repeat_options during each recomposition
 *  Intended use:
 *
 *      val helper = remember { RecurrenceHelper(context) }
 *      helper.setRecurrence(recurrence)
 *      ...
 *      Text(text = helper.title(selectedIndex), ...)
 */
class RecurrenceHelper (
    context: Context,
) {
    val repeatRuleToString = RepeatRuleToString(context,Locale.getDefault(),Firebase(context, Preferences(context)))
    private var _recurrence: String? = null
    val recurrence: String? get() = _recurrence

    private var _rrule: Recur? = null
    val rrule: Recur? get() = _rrule

    private val titles: MutableList<String> =
        Lists.newArrayList(*context.resources.getStringArray(R.array.repeat_options))
    private val ruleTitle = if (isCustomValue()) repeatRuleToString.toString(recurrence)!! else titles[5]

    fun title(index: Int): String =
        if (index < 5) titles[index]
        else ruleTitle

    fun isCustomValue(): Boolean {
        if (rrule == null) {
            return false
        }
        val frequency = rrule!!.frequency
        return (frequency == Recur.Frequency.WEEKLY || frequency == Recur.Frequency.MONTHLY) && !rrule!!.dayList.isEmpty()
                || frequency == Recur.Frequency.HOURLY
                || frequency == Recur.Frequency.MINUTELY
                || rrule!!.until != null
                || rrule!!.interval > 1
                || rrule!!.count > 0
    }

    fun selectionIndex(): Int =
        when {
            rrule == null -> 0
            isCustomValue() -> 6
            rrule!!.frequency == Recur.Frequency.DAILY -> 1
            rrule!!.frequency == Recur.Frequency.WEEKLY -> 2
            rrule!!.frequency == Recur.Frequency.MONTHLY -> 3
            rrule!!.frequency == Recur.Frequency.YEARLY -> 4
            else -> 0
        }

    fun selectedFrequency(index: Int): Recur.Frequency =
        when (index) {
            1 -> Recur.Frequency.DAILY
            2 -> Recur.Frequency.WEEKLY
            3 -> Recur.Frequency.MONTHLY
            4 -> Recur.Frequency.YEARLY
            else -> throw IllegalArgumentException()
        }

    fun setRecurrence(recurrence: String?) {
        _recurrence = recurrence
        _rrule = recurrence
            .takeIf { !it.isNullOrBlank() }
            ?.let {
                try {
                    newRecur(it)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
    }
}

@Composable
fun rememberRepeatRuleToString(): RepeatRuleToString {
    val context = LocalContext.current
    return remember { RepeatRuleToString(context,Locale.getDefault(),Firebase(context, Preferences(context))) }
}
