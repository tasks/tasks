package org.tasks.compose.pickers

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.todoroo.andlib.utility.DateUtilities
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.R
import org.tasks.compose.OutlinedBox
import org.tasks.compose.OutlinedNumberInput
import org.tasks.compose.OutlinedSpinner
import org.tasks.compose.border
import org.tasks.repeats.CustomRecurrenceViewModel
import org.tasks.themes.TasksTheme
import java.time.DayOfWeek
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRecurrence(
    state: CustomRecurrenceViewModel.ViewState,
    save: () -> Unit,
    discard: () -> Unit,
    setInterval: (Int) -> Unit,
    setSelectedFrequency: (Recur.Frequency) -> Unit,
    setEndDate: (Long) -> Unit,
    setSelectedEndType: (Int) -> Unit,
    setOccurrences: (Int) -> Unit,
    toggleDay: (DayOfWeek) -> Unit,
    setMonthSelection: (Int) -> Unit,
) {
    BackHandler {
        save()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.repeats_custom_recurrence),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = save) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(id = R.string.save),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = discard) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFeatureSettings = "c2sc, smcp"
                            )
                        )
                    }
                },
            )
        }
    ) { padding ->
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Header(R.string.repeats_every)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    OutlinedNumberInput(
                        number = state.interval,
                        onTextChanged = setInterval,
                    )
                    val context = LocalContext.current
                    val options by remember(state.interval, state.frequency) {
                        derivedStateOf {
                            state.frequencyOptions.map {
                                context.resources.getQuantityString(
                                    it.plural,
                                    state.interval,
                                    state.interval,
                                )
                            }
                        }
                    }
                    OutlinedSpinner(
                        text = pluralStringResource(
                            id = state.frequency.plural,
                            count = state.interval
                        ),
                        options = options,
                        onSelected = { setSelectedFrequency(state.frequencyOptions[it]) },
                    )
                }
                if (state.frequency == Recur.Frequency.WEEKLY) {
                    WeekdayPicker(
                        daysOfWeek = state.daysOfWeek,
                        selected = state.selectedDays,
                        toggle = toggleDay,
                    )
                } else if (state.frequency == Recur.Frequency.MONTHLY && !state.isMicrosoftTask) {
                    MonthlyPicker(
                        monthDay = state.monthDay,
                        dayNumber = state.dueDayOfMonth,
                        dayOfWeek = state.dueDayOfWeek,
                        nthWeek = state.nthWeek,
                        isLastWeek = state.lastWeekDayOfMonth,
                        locale = state.locale,
                        onSelected = setMonthSelection,
                    )
                }
                if (!state.isMicrosoftTask) {
                    Divider(
                        modifier = Modifier.padding(vertical = if (state.frequency == Recur.Frequency.WEEKLY) 11.dp else 16.dp),
                        color = border()
                    )
                    EndsPicker(
                        selection = state.endSelection,
                        endDate = state.endDate,
                        endOccurrences = state.endCount,
                        setEndDate = setEndDate,
                        setSelection = setSelectedEndType,
                        setOccurrences = setOccurrences,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(resId: Int) {
    Text(
        text = stringResource(id = resId),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayPicker(
    daysOfWeek: List<DayOfWeek>,
    selected: List<DayOfWeek>,
    toggle: (DayOfWeek) -> Unit,
) {
    val context = LocalContext.current
    val locale = remember {
        ConfigurationCompat
            .getLocales(context.resources.configuration)
            .get(0)
            ?: Locale.getDefault()
    }
    Divider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = border()
    )
    Header(R.string.repeats_weekly_on)
    Spacer(modifier = Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        daysOfWeek.forEach { dayOfWeek ->
            val string = remember(dayOfWeek) {
                dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 5.dp) // hack until compose 1.5
                    .size(36.dp)
                    .let {
                        if (selected.contains(dayOfWeek)) {
                            it.background(MaterialTheme.colorScheme.secondary, shape = CircleShape)
                        } else {
                            it.border(1.dp, border(), shape = CircleShape)
                        }
                    }
                    .clickable { toggle(dayOfWeek) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = string,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected.contains(dayOfWeek)) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MonthlyPicker(
    monthDay: WeekDay?,
    dayNumber: Int,
    dayOfWeek: DayOfWeek,
    nthWeek: Int,
    isLastWeek: Boolean,
    locale: Locale,
    onSelected: (Int) -> Unit,
) {
    val selection = remember(monthDay) {
        when (monthDay?.offset) {
            null -> 0
            -1 -> 2
            else -> 1
        }
    }
    Divider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = border()
    )
    val context = LocalContext.current
    val options = remember(dayNumber, dayOfWeek, nthWeek, isLastWeek, locale) {
        ArrayList<String>().apply {
            add(context.getString(R.string.repeat_monthly_on_day_number, dayNumber))
            val nth = context.getString(
                when (nthWeek - 1) {
                    0 -> R.string.repeat_monthly_first_week
                    1 -> R.string.repeat_monthly_second_week
                    2 -> R.string.repeat_monthly_third_week
                    3 -> R.string.repeat_monthly_fourth_week
                    4 -> R.string.repeat_monthly_fifth_week
                    else -> throw IllegalArgumentException()
                }
            )
            val dayOfWeekDisplayName = dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            add(
                context.getString(
                    R.string.repeat_monthly_on_the_nth_weekday,
                    nth,
                    dayOfWeekDisplayName
                )
            )
            if (isLastWeek) {
                add(
                    context.getString(
                        R.string.repeat_monthly_on_the_nth_weekday,
                        context.getString(R.string.repeat_monthly_last_week),
                        dayOfWeekDisplayName
                    )
                )
            }
        }
    }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        OutlinedSpinner(
            text = options[selection],
            options = options,
            onSelected = onSelected,
        )
    }
}

@Composable
private fun EndsPicker(
    selection: Int,
    endDate: Long,
    endOccurrences: Int,
    setOccurrences: (Int) -> Unit,
    setEndDate: (Long) -> Unit,
    setSelection: (Int) -> Unit,
) {
    Header(R.string.repeats_ends)
    Spacer(modifier = Modifier.height(8.dp))
    RadioRow(selected = selection == 0, onClick = { setSelection(0) }) {
        Text(text = stringResource(id = R.string.repeats_never))
    }
    Divider(
        modifier = Modifier.padding(start = 50.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        color = border()
    )
    RadioRow(selected = selection == 1, onClick = { setSelection(1) }) {
        Text(text = stringResource(id = R.string.repeats_on))
        Spacer(modifier = Modifier.width(8.dp))
        val context = LocalContext.current
        val locale = remember { LocaleList.getDefault()[0] }
        val endDateString by remember(context, endDate) {
            derivedStateOf {
                DateUtilities.getRelativeDay(context, endDate, locale, FormatStyle.MEDIUM)
            }
        }
        var showDatePicker by remember { mutableStateOf(false) }
        if (showDatePicker) {
            DatePickerDialog(
                initialDate = endDate,
                selected = { setEndDate(it) },
                dismiss = { showDatePicker = false },
            )
        }
        OutlinedBox(
            modifier = Modifier.clickable {
                setSelection(1)
                showDatePicker = true
            }
        ) {
            Text(text = endDateString)
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
    Divider(
        modifier = Modifier.padding(start = 50.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        color = border()
    )
    RadioRow(selected = selection == 2, onClick = { setSelection(2) }) {
        Text(text = stringResource(id = R.string.repeats_after))
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedNumberInput(
            number = endOccurrences,
            onTextChanged = setOccurrences,
            onFocus = { setSelection(2) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = pluralStringResource(id = R.plurals.repeat_occurrence, endOccurrences))
    }
}

@Composable
fun RadioRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private val Recur.Frequency.plural: Int
    get() = when (this) {
        Recur.Frequency.MINUTELY -> R.plurals.repeat_minutes
        Recur.Frequency.HOURLY -> R.plurals.repeat_hours
        Recur.Frequency.DAILY -> R.plurals.repeat_days
        Recur.Frequency.WEEKLY -> R.plurals.repeat_weeks
        Recur.Frequency.MONTHLY -> R.plurals.repeat_months
        Recur.Frequency.YEARLY -> R.plurals.repeat_years
        else -> throw RuntimeException()
    }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WeeklyPreview() {
    TasksTheme {
        CustomRecurrence(
            state = CustomRecurrenceViewModel.ViewState(frequency = Recur.Frequency.WEEKLY),
            save = {},
            discard = {},
            setSelectedFrequency = {},
            setSelectedEndType = {},
            setEndDate = {},
            setInterval = {},
            setOccurrences = {},
            toggleDay = {},
            setMonthSelection = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MonthlyPreview() {
    TasksTheme {
        CustomRecurrence(
            state = CustomRecurrenceViewModel.ViewState(frequency = Recur.Frequency.MONTHLY),
            save = {},
            discard = {},
            setSelectedFrequency = {},
            setSelectedEndType = {},
            setEndDate = {},
            setInterval = {},
            setOccurrences = {},
            toggleDay = {},
            setMonthSelection = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MinutelyPreview() {
    TasksTheme {
        CustomRecurrence(
            state = CustomRecurrenceViewModel.ViewState(frequency = Recur.Frequency.MINUTELY),
            save = {},
            discard = {},
            setSelectedFrequency = {},
            setSelectedEndType = {},
            setEndDate = {},
            setInterval = {},
            setOccurrences = {},
            toggleDay = {},
            setMonthSelection = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HourlyPreview() {
    TasksTheme {
        CustomRecurrence(
            state = CustomRecurrenceViewModel.ViewState(frequency = Recur.Frequency.HOURLY),
            save = {},
            discard = {},
            setSelectedFrequency = {},
            setSelectedEndType = {},
            setEndDate = {},
            setInterval = {},
            setOccurrences = {},
            toggleDay = {},
            setMonthSelection = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DailyPreview() {
    TasksTheme {
        CustomRecurrence(
            state = CustomRecurrenceViewModel.ViewState(frequency = Recur.Frequency.DAILY),
            save = {},
            discard = {},
            setSelectedFrequency = {},
            setSelectedEndType = {},
            setEndDate = {},
            setInterval = {},
            setOccurrences = {},
            toggleDay = {},
            setMonthSelection = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun YearlyPreview() {
    TasksTheme {
        CustomRecurrence(
            state = CustomRecurrenceViewModel.ViewState(frequency = Recur.Frequency.YEARLY),
            save = {},
            discard = {},
            setSelectedFrequency = {},
            setSelectedEndType = {},
            setEndDate = {},
            setInterval = {},
            setOccurrences = {},
            toggleDay = {},
            setMonthSelection = {},
        )
    }
}
