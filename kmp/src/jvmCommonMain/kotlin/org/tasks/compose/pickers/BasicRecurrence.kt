package org.tasks.compose.pickers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.fortuna.ical4j.model.Recur
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.repeat_option_custom
import tasks.kmp.generated.resources.repeat_option_does_not_repeat
import tasks.kmp.generated.resources.repeat_option_every_day
import tasks.kmp.generated.resources.repeat_option_every_month
import tasks.kmp.generated.resources.repeat_option_every_week
import tasks.kmp.generated.resources.repeat_option_every_year

fun Recur.isCustomRecurrence(): Boolean =
    (frequency == Recur.Frequency.WEEKLY || frequency == Recur.Frequency.MONTHLY) && !dayList.isEmpty() ||
            frequency == Recur.Frequency.HOURLY ||
            frequency == Recur.Frequency.MINUTELY ||
            until != null ||
            interval > 1 ||
            count > 0

sealed interface BasicRecurrenceOption {
    data object KeepCustom : BasicRecurrenceOption

    data object DoesNotRepeat : BasicRecurrenceOption

    data class Frequency(val frequency: Recur.Frequency) : BasicRecurrenceOption

    data object Custom : BasicRecurrenceOption
}

@Composable
fun BasicRecurrence(
    customLabel: String?,
    selectedFrequency: Recur.Frequency?,
    onSelected: (BasicRecurrenceOption) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val rows = buildList {
        if (customLabel != null) {
            add(customLabel to BasicRecurrenceOption.KeepCustom)
        }
        add(stringResource(Res.string.repeat_option_does_not_repeat) to BasicRecurrenceOption.DoesNotRepeat)
        add(stringResource(Res.string.repeat_option_every_day) to BasicRecurrenceOption.Frequency(Recur.Frequency.DAILY))
        add(stringResource(Res.string.repeat_option_every_week) to BasicRecurrenceOption.Frequency(Recur.Frequency.WEEKLY))
        add(stringResource(Res.string.repeat_option_every_month) to BasicRecurrenceOption.Frequency(Recur.Frequency.MONTHLY))
        add(stringResource(Res.string.repeat_option_every_year) to BasicRecurrenceOption.Frequency(Recur.Frequency.YEARLY))
        add(stringResource(Res.string.repeat_option_custom) to BasicRecurrenceOption.Custom)
    }
    val selectedIndex = when {
        customLabel != null -> 0
        selectedFrequency != null ->
            rows
                .indexOfFirst {
                    val option = it.second
                    option is BasicRecurrenceOption.Frequency && option.frequency == selectedFrequency
                }
                .coerceAtLeast(0)
        else -> 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        rows.forEachIndexed { index, (label, option) ->
            RadioRow(
                selected = index == selectedIndex,
                onClick = { onSelected(option) },
                contentPadding = contentPadding,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                )
            }
        }
    }
}
