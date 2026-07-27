package org.tasks.compose.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.RepeatRuleSummary
import org.tasks.compose.pickers.RepeatFromPickerSheet
import org.tasks.compose.rememberRepeatRuleSummary
import org.tasks.data.entity.Task
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.TEA_control_repeat
import tasks.kmp.generated.resources.due_date
import tasks.kmp.generated.resources.repeat_option_does_not_repeat
import tasks.kmp.generated.resources.repeat_type_completion_capitalized
import tasks.kmp.generated.resources.repeats_from

private val LabelChipGap = 8.dp
private val ChipContentPadding = PaddingValues(start = 16.dp, end = 8.dp)

@Composable
fun RepeatRow(
    recurrence: String?,
    repeatFrom: @Task.RepeatFrom Int,
    onClick: () -> Unit,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = rememberRepeatRuleSummary(recurrence)
    if (summary is RepeatRuleSummary.Loading) {
        return
    }
    val repeats = summary as? RepeatRuleSummary.Repeats
    var showRepeatFromPicker by remember { mutableStateOf(false) }
    TaskEditCardRow(
        value = repeats?.text ?: stringResource(Res.string.repeat_option_does_not_repeat),
        valueColor = if (repeats == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        onClick = onClick,
        modifier = modifier,
        title = stringResource(Res.string.TEA_control_repeat),
        icon = TasksIcons.REPEAT,
        content = if (repeats == null) {
            null
        } else {
            {
                RepeatFromRow(
                    repeatFrom = repeatFrom,
                    onClick = { showRepeatFromPicker = true },
                )
            }
        },
    )
    if (showRepeatFromPicker) {
        RepeatFromPickerSheet(
            repeatFrom = repeatFrom,
            onSelected = {
                onRepeatFromChanged(it)
                showRepeatFromPicker = false
            },
            onDismiss = { showRepeatFromPicker = false },
        )
    }
}

@Composable
private fun RepeatFromRow(
    repeatFrom: @Task.RepeatFrom Int,
    onClick: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LabelChipGap),
        verticalArrangement = Arrangement.spacedBy(LabelChipGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.repeats_from),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        FilledTonalButton(
            onClick = onClick,
            contentPadding = ChipContentPadding,
        ) {
            Text(
                text = stringResource(
                    when (repeatFrom) {
                        Task.RepeatFrom.COMPLETION_DATE -> Res.string.repeat_type_completion_capitalized
                        else -> Res.string.due_date
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
