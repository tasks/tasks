package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarPicker.Companion.newCalendarPicker
import org.tasks.compose.DefaultRemindersActivity
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.settings.TaskDefaultsScreen
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.location.LocationPickerActivity
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.BasePreferences
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.tags.TagPickerActivity
import org.tasks.tags.TagPickerActivity.Companion.EXTRA_SELECTED
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

private const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"

@AndroidEntryPoint
class TaskDefaults : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: TaskDefaultsViewModel by viewModels()

    private val listPickerLauncher = registerForListPickerResult {
        viewModel.setDefaultList(it)
    }

    private val locationLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val place = result.data?.getParcelableExtra<Place>(EXTRA_PLACE)
            viewModel.setDefaultLocation(place)
        }
    }

    private val tagsLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val tags = result.data?.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)
            viewModel.handleTagsResult(tags)
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            CalendarPicker.REQUEST_KEY, this
        ) { _, bundle ->
            val calendarId = bundle.getString(CalendarPicker.EXTRA_CALENDAR_ID)
            viewModel.handleCalendarResult(calendarId)
        }
        parentFragmentManager.setFragmentResultListener(
            BasicRecurrenceDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val rrule = bundle.getString(BasicRecurrenceDialog.EXTRA_RRULE)
            viewModel.handleRecurrenceResult(rrule)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            var showImportanceDialog by rememberSaveable { mutableStateOf(false) }
            var showStartDateDialog by rememberSaveable { mutableStateOf(false) }
            var showDueDateDialog by rememberSaveable { mutableStateOf(false) }
            var showRecurrenceFromDialog by rememberSaveable { mutableStateOf(false) }
            var showRandomReminderDialog by rememberSaveable { mutableStateOf(false) }
            var showRemindersModeDialog by rememberSaveable { mutableStateOf(false) }
            var showLocationReminderDialog by rememberSaveable { mutableStateOf(false) }

            TaskDefaultsScreen(
                addToTopEnabled = viewModel.addToTopEnabled,
                defaultListName = viewModel.defaultListName,
                defaultTagsSummary = viewModel.defaultTagsSummary,
                importanceSummary = viewModel.importanceSummary,
                startDateSummary = viewModel.startDateSummary,
                dueDateSummary = viewModel.dueDateSummary,
                calendarName = viewModel.calendarName,
                recurrenceSummary = viewModel.recurrenceSummary,
                recurrenceFromSummary = viewModel.recurrenceFromSummary,
                remindersSummary = viewModel.remindersSummary,
                randomReminderSummary = viewModel.randomReminderSummary,
                remindersModeSummary = viewModel.remindersModeSummary,
                locationName = viewModel.locationName,
                hasDefaultLocation = viewModel.hasDefaultLocation,
                locationReminderSummary = viewModel.locationReminderSummary,
                onAddToTop = { viewModel.updateAddToTop(it) },
                onDefaultList = {
                    lifecycleScope.launch {
                        listPickerLauncher.launch(
                            context = requireContext(),
                            selectedFilter = viewModel.getDefaultList(),
                            listsOnly = true,
                        )
                    }
                },
                onDefaultTags = {
                    lifecycleScope.launch {
                        val intent = Intent(context, TagPickerActivity::class.java)
                            .putParcelableArrayListExtra(
                                EXTRA_SELECTED,
                                ArrayList(viewModel.defaultTags())
                            )
                        tagsLauncher.launch(intent)
                    }
                },
                onImportance = { showImportanceDialog = true },
                onStartDate = { showStartDateDialog = true },
                onDueDate = { showDueDateDialog = true },
                onCalendar = {
                    newCalendarPicker(
                        viewModel.defaultCalendar,
                    ).show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
                },
                onRecurrence = {
                    BasicRecurrenceDialog
                        .newBasicRecurrenceDialog(
                            rrule = viewModel.getRecurrenceRule(),
                            dueDate = 0,
                            accountType = CaldavAccount.TYPE_LOCAL
                        )
                        .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
                },
                onRecurrenceFrom = { showRecurrenceFromDialog = true },
                onReminders = {
                    startActivity(
                        Intent(requireContext(), DefaultRemindersActivity::class.java)
                    )
                },
                onRandomReminder = { showRandomReminderDialog = true },
                onRemindersMode = { showRemindersModeDialog = true },
                onLocation = {
                    locationLauncher.launch(
                        Intent(context, LocationPickerActivity::class.java),
                    )
                },
                onDeleteLocation = {
                    viewModel.setDefaultLocation(null)
                },
                onLocationReminder = { showLocationReminderDialog = true },
            )

            if (showImportanceDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.EPr_default_importance_title),
                    entries = viewModel.importanceEntries,
                    values = viewModel.importanceValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_default_importance_key, 2
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_default_importance_key, value)
                        viewModel.refreshImportance()
                    },
                    onDismiss = { showImportanceDialog = false },
                )
            }

            if (showStartDateDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.default_start_date),
                    entries = viewModel.startDateEntries,
                    values = viewModel.startDateValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_default_hideUntil_key, 0
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_default_hideUntil_key, value)
                        viewModel.refreshStartDate()
                    },
                    onDismiss = { showStartDateDialog = false },
                )
            }

            if (showDueDateDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.default_due_date),
                    entries = viewModel.dueDateEntries,
                    values = viewModel.dueDateValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_default_urgency_key, 0
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_default_urgency_key, value)
                        viewModel.refreshDueDate()
                    },
                    onDismiss = { showDueDateDialog = false },
                )
            }

            if (showRecurrenceFromDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.repeats_from),
                    entries = viewModel.recurrenceFromEntries,
                    values = viewModel.recurrenceFromValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_default_recurrence_from, 0
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_default_recurrence_from, value)
                        viewModel.refreshRecurrenceFrom()
                    },
                    onDismiss = { showRecurrenceFromDialog = false },
                )
            }

            if (showRandomReminderDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.rmd_EPr_defaultRemind_title),
                    entries = viewModel.randomReminderEntries,
                    values = viewModel.randomReminderValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_rmd_default_random_hours, 0
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_rmd_default_random_hours, value)
                        viewModel.refreshRandomReminder()
                    },
                    onDismiss = { showRandomReminderDialog = false },
                )
            }

            if (showRemindersModeDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.EPr_default_reminders_mode_title),
                    entries = viewModel.remindersModeEntries,
                    values = viewModel.remindersModeValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_default_reminders_mode_key, 0
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_default_reminders_mode_key, value)
                        viewModel.refreshRemindersMode()
                    },
                    onDismiss = { showRemindersModeDialog = false },
                )
            }

            if (showLocationReminderDialog) {
                ListPreferenceDialog(
                    title = stringResource(R.string.EPr_default_location_reminder_title),
                    entries = viewModel.locationReminderEntries,
                    values = viewModel.locationReminderValues,
                    currentValue = viewModel.getListPrefCurrentValue(
                        R.string.p_default_location_reminder_key, 0
                    ),
                    onSelect = { value ->
                        viewModel.setListPreference(R.string.p_default_location_reminder_key, value)
                        viewModel.refreshLocationReminder()
                    },
                    onDismiss = { showLocationReminderDialog = false },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }

    companion object {
        const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
    }
}

@Composable
private fun ListPreferenceDialog(
    title: String,
    entries: Array<String>,
    values: Array<String>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(values[index])
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = values[index] == currentValue,
                            onClick = null,
                        )
                        Text(
                            text = entry,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
