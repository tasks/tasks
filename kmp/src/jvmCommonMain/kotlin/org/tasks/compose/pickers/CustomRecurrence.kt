package org.tasks.compose.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.OutlinedBox
import org.tasks.compose.OutlinedNumberInput
import org.tasks.compose.OutlinedSpinner
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.border
import org.tasks.extensions.formatNumber
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.repeats.CustomRecurrenceViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.repeat_days
import tasks.kmp.generated.resources.repeat_hours
import tasks.kmp.generated.resources.repeat_minutes
import tasks.kmp.generated.resources.repeat_monthly_fifth_week
import tasks.kmp.generated.resources.repeat_monthly_first_week
import tasks.kmp.generated.resources.repeat_monthly_fourth_week
import tasks.kmp.generated.resources.repeat_monthly_last_week
import tasks.kmp.generated.resources.repeat_monthly_on_day_number
import tasks.kmp.generated.resources.repeat_monthly_on_the_nth_weekday
import tasks.kmp.generated.resources.repeat_monthly_second_week
import tasks.kmp.generated.resources.repeat_monthly_third_week
import tasks.kmp.generated.resources.repeat_months
import tasks.kmp.generated.resources.repeat_occurrence
import tasks.kmp.generated.resources.repeat_weeks
import tasks.kmp.generated.resources.repeat_years
import tasks.kmp.generated.resources.repeats_after
import tasks.kmp.generated.resources.repeats_custom_recurrence
import tasks.kmp.generated.resources.repeats_ends
import tasks.kmp.generated.resources.repeats_every
import tasks.kmp.generated.resources.repeats_never
import tasks.kmp.generated.resources.repeats_on
import tasks.kmp.generated.resources.repeats_weekly_on
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val DialogHorizontalPadding = 20.dp
private val RadioButtonVisualInset = 14.dp
private val RadioButtonSize = 48.dp
private val TextButtonVisualInset = 12.dp

private val RadioRowPadding = PaddingValues(
    start = DialogHorizontalPadding - RadioButtonVisualInset,
    end = DialogHorizontalPadding,
)

private val RadioContentInset =
    DialogHorizontalPadding - RadioButtonVisualInset + RadioButtonSize

private val ButtonRowPadding = DialogHorizontalPadding - TextButtonVisualInset
private val ContentBottomGap = 8.dp

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
    calendarDisplayMode: DisplayMode,
    setDisplayMode: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = discard)
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
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
                        text = stringResource(Res.string.repeats_custom_recurrence),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Header(Res.string.repeats_every)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = DialogHorizontalPadding),
                ) {
                    OutlinedNumberInput(
                        number = state.interval,
                        locale = state.locale,
                        onTextChanged = setInterval,
                    )
                    val options = state.frequencyOptions.map {
                        pluralStringResource(
                            it.plural,
                            state.interval,
                            state.interval,
                        )
                    }
                    OutlinedSpinner(
                        text = pluralStringResource(
                            state.frequency.plural,
                            state.interval
                        ),
                        options = options,
                        onSelected = { setSelectedFrequency(state.frequencyOptions[it]) },
                    )
                }
                if (state.frequency == Recur.Frequency.WEEKLY) {
                    WeekdayPicker(
                        daysOfWeek = state.daysOfWeek,
                        selected = state.selectedDays,
                        locale = state.locale,
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
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = if (state.frequency == Recur.Frequency.WEEKLY) 11.dp else 16.dp),
                        color = border()
                    )
                    EndsPicker(
                        selection = state.endSelection,
                        endDate = state.endDate,
                        endOccurrences = state.endCount,
                        locale = state.locale,
                        setEndDate = setEndDate,
                        setSelection = setSelectedEndType,
                        setOccurrences = setOccurrences,
                        calendarDisplayMode = calendarDisplayMode,
                        setDisplayMode = setDisplayMode,
                    )
                }
            }
            Spacer(modifier = Modifier.height(ContentBottomGap))
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ButtonRowPadding,
                        end = ButtonRowPadding,
                        bottom = ContentBottomGap,
                    ),
            ) {
                TextButton(onClick = discard) {
                    Text(text = stringResource(Res.string.cancel))
                }
                TextButton(onClick = save) {
                    Text(text = stringResource(Res.string.ok))
                }
            }
        }
    }
}

@Composable
private fun Header(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = DialogHorizontalPadding),
    )
}

@Composable
private fun WeekdayPicker(
    daysOfWeek: List<DayOfWeek>,
    selected: List<DayOfWeek>,
    locale: Locale,
    toggle: (DayOfWeek) -> Unit,
) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = border()
    )
    Header(Res.string.repeats_weekly_on)
    Spacer(modifier = Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = DialogHorizontalPadding),
    ) {
        daysOfWeek.forEach { dayOfWeek ->
            val string = remember(dayOfWeek, locale) {
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
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = border()
    )
    val dayOfWeekDisplayName = remember(dayOfWeek, locale) {
        dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    }
    val onDayNumber = stringResource(Res.string.repeat_monthly_on_day_number, locale.formatNumber(dayNumber))
    val nth = stringResource(
        when (nthWeek - 1) {
            0 -> Res.string.repeat_monthly_first_week
            1 -> Res.string.repeat_monthly_second_week
            2 -> Res.string.repeat_monthly_third_week
            3 -> Res.string.repeat_monthly_fourth_week
            4 -> Res.string.repeat_monthly_fifth_week
            else -> throw IllegalArgumentException()
        }
    )
    val onNthWeekday = stringResource(
        Res.string.repeat_monthly_on_the_nth_weekday,
        nth,
        dayOfWeekDisplayName
    )
    val lastWeekString = stringResource(Res.string.repeat_monthly_last_week)
    val onLastWeekday = stringResource(
        Res.string.repeat_monthly_on_the_nth_weekday,
        lastWeekString,
        dayOfWeekDisplayName
    )
    val options = remember(onDayNumber, onNthWeekday, onLastWeekday, isLastWeek, selection) {
        ArrayList<String>().apply {
            add(onDayNumber)
            add(onNthWeekday)
            if (isLastWeek || selection == 2) {
                add(onLastWeekday)
            }
        }
    }
    Row(
        modifier = Modifier.padding(horizontal = DialogHorizontalPadding),
    ) {
        OutlinedSpinner(
            text = options[selection],
            options = options,
            onSelected = onSelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndsPicker(
    selection: Int,
    endDate: Long,
    endOccurrences: Int,
    locale: Locale,
    calendarDisplayMode: DisplayMode,
    setDisplayMode: (DisplayMode) -> Unit,
    setOccurrences: (Int) -> Unit,
    setEndDate: (Long) -> Unit,
    setSelection: (Int) -> Unit,
) {
    Header(Res.string.repeats_ends)
    Spacer(modifier = Modifier.height(8.dp))
    RadioRow(selected = selection == 0, onClick = { setSelection(0) }) {
        Text(text = stringResource(Res.string.repeats_never))
    }
    HorizontalDivider(
        modifier = Modifier.padding(
            start = RadioContentInset,
            end = DialogHorizontalPadding,
            top = 8.dp,
            bottom = 8.dp,
        ),
        color = border()
    )
    RadioRow(selected = selection == 1, onClick = { setSelection(1) }) {
        Text(text = stringResource(Res.string.repeats_on))
        Spacer(modifier = Modifier.width(8.dp))
        val endDateString = remember(endDate) { runBlocking { getRelativeDay(endDate) } }
        var showDatePicker by remember { mutableStateOf(false) }
        if (showDatePicker) {
            DatePickerDialog(
                initialDate = endDate,
                displayMode = calendarDisplayMode,
                setDisplayMode = setDisplayMode,
                selected = {
                    setEndDate(it)
                    showDatePicker = false
                },
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
    HorizontalDivider(
        modifier = Modifier.padding(
            start = RadioContentInset,
            end = DialogHorizontalPadding,
            top = 8.dp,
            bottom = 8.dp,
        ),
        color = border()
    )
    RadioRow(selected = selection == 2, onClick = { setSelection(2) }) {
        Text(text = stringResource(Res.string.repeats_after))
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedNumberInput(
            number = endOccurrences,
            locale = locale,
            onTextChanged = setOccurrences,
            onFocus = { setSelection(2) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = pluralStringResource(Res.plurals.repeat_occurrence, endOccurrences))
    }
}

@Composable
fun RadioRow(
    selected: Boolean,
    onClick: () -> Unit,
    contentPadding: PaddingValues = RadioRowPadding,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(contentPadding),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private val Recur.Frequency.plural: PluralStringResource
    get() = when (this) {
        Recur.Frequency.MINUTELY -> Res.plurals.repeat_minutes
        Recur.Frequency.HOURLY -> Res.plurals.repeat_hours
        Recur.Frequency.DAILY -> Res.plurals.repeat_days
        Recur.Frequency.WEEKLY -> Res.plurals.repeat_weeks
        Recur.Frequency.MONTHLY -> Res.plurals.repeat_months
        Recur.Frequency.YEARLY -> Res.plurals.repeat_years
        else -> throw RuntimeException()
    }
