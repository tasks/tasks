package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.calendars.CalendarProvider
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.reminders.AlarmToString
import org.tasks.repeats.RepeatRuleToString
import org.tasks.filters.CaldavFilter
import org.tasks.tags.TagPickerActivity.Companion.EXTRA_SELECTED
import javax.inject.Inject

@HiltViewModel
class TaskDefaultsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val calendarProvider: CalendarProvider,
    private val repeatRuleToString: RepeatRuleToString,
    private val locationDao: LocationDao,
    private val tagDataDao: TagDataDao,
) : ViewModel() {

    var addToTopEnabled by mutableStateOf(false)
        private set
    var defaultListName by mutableStateOf("")
        private set
    var defaultTagsSummary by mutableStateOf("")
        private set
    var importanceSummary by mutableStateOf("")
        private set
    var startDateSummary by mutableStateOf("")
        private set
    var dueDateSummary by mutableStateOf("")
        private set
    var calendarName by mutableStateOf("")
        private set
    var recurrenceSummary by mutableStateOf("")
        private set
    var recurrenceFromSummary by mutableStateOf("")
        private set
    var remindersSummary by mutableStateOf("")
        private set
    var randomReminderSummary by mutableStateOf("")
        private set
    var remindersModeSummary by mutableStateOf("")
        private set
    var locationName by mutableStateOf("")
        private set
    var hasDefaultLocation by mutableStateOf(false)
        private set
    var locationReminderSummary by mutableStateOf("")
        private set

    val importanceEntries: Array<String> = context.resources.getStringArray(R.array.EPr_default_importance)
    val importanceValues: Array<String> = context.resources.getStringArray(R.array.EPr_default_importance_values)
    val startDateEntries: Array<String> = context.resources.getStringArray(R.array.EPr_default_hideUntil)
    val startDateValues: Array<String> = context.resources.getStringArray(R.array.EPr_default_hideUntil_values)
    val dueDateEntries: Array<String> = context.resources.getStringArray(R.array.EPr_default_urgency)
    val dueDateValues: Array<String> = context.resources.getStringArray(R.array.EPr_default_urgency_values)
    val recurrenceFromEntries: Array<String> = context.resources.getStringArray(R.array.repeat_type_capitalized)
    val recurrenceFromValues: Array<String> = context.resources.getStringArray(R.array.repeat_type_values)
    val randomReminderEntries: Array<String> = context.resources.getStringArray(R.array.EPr_reminder_random)
    val randomReminderValues: Array<String> = context.resources.getStringArray(R.array.EPr_reminder_random_hours)
    val remindersModeEntries: Array<String> = context.resources.getStringArray(R.array.EPr_default_reminders_mode)
    val remindersModeValues: Array<String> = context.resources.getStringArray(R.array.EPr_default_reminders_mode_values)
    val locationReminderEntries: Array<String> = context.resources.getStringArray(R.array.EPr_default_location_reminder)
    val locationReminderValues: Array<String> = context.resources.getStringArray(R.array.EPR_default_location_reminder_values)

    init {
        refreshState()
    }

    fun refreshState() {
        addToTopEnabled = preferences.getBoolean(R.string.p_add_to_top, true)
        refreshDefaultList()
        refreshTags()
        refreshImportance()
        refreshStartDate()
        refreshDueDate()
        refreshCalendarName()
        refreshRecurrence()
        refreshRecurrenceFrom()
        refreshReminders()
        refreshRandomReminder()
        refreshRemindersMode()
        refreshLocation()
        refreshLocationReminder()
    }

    fun updateAddToTop(enabled: Boolean) {
        preferences.setBoolean(R.string.p_add_to_top, enabled)
        addToTopEnabled = enabled
    }

    suspend fun getDefaultList() = defaultFilterProvider.getDefaultList()

    fun setDefaultList(filter: CaldavFilter) {
        defaultFilterProvider.defaultList = filter
        refreshDefaultList()
    }

    suspend fun defaultTags(): List<TagData> =
        preferences.getStringValue(R.string.p_default_tags)
            ?.split(",")
            ?.let { tagDataDao.getByUuid(it) }
            ?.sortedBy { it.name }
            ?: emptyList()

    fun setListPreference(prefKey: Int, value: String) {
        preferences.setString(prefKey, value)
    }

    val defaultCalendar: String?
        get() = preferences.defaultCalendar

    fun getRecurrenceRule(): String? =
        preferences.getStringValue(R.string.p_default_recurrence)?.takeIf { it.isNotBlank() }

    fun getListPrefCurrentValue(prefKey: Int, defaultValue: Int): String =
        preferences.getIntegerFromString(prefKey, defaultValue).toString()

    fun handleCalendarResult(calendarId: String?) {
        preferences.setString(R.string.gcal_p_default, calendarId)
        refreshCalendarName()
    }

    fun handleRecurrenceResult(rrule: String?) {
        preferences.setString(R.string.p_default_recurrence, rrule)
        refreshRecurrence()
    }

    fun handleLocationResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            setDefaultLocation(data?.getParcelableExtra(EXTRA_PLACE))
        }
    }

    fun handleTagsResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            preferences.setString(
                R.string.p_default_tags,
                data?.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)
                    ?.mapNotNull { it.remoteId }
                    ?.joinToString(",")
            )
            refreshTags()
        }
    }

    fun setDefaultLocation(place: Place?) {
        preferences.setString(R.string.p_default_location, place?.uid)
        refreshLocation()
    }

    fun refreshImportance() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_default_importance_key, 2
        ).toString()
        val index = importanceValues.indexOf(currentValue).coerceAtLeast(0)
        importanceSummary = importanceEntries[index]
    }

    fun refreshStartDate() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_default_hideUntil_key, 0
        ).toString()
        val index = startDateValues.indexOf(currentValue).coerceAtLeast(0)
        startDateSummary = startDateEntries[index]
    }

    fun refreshDueDate() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_default_urgency_key, 0
        ).toString()
        val index = dueDateValues.indexOf(currentValue).coerceAtLeast(0)
        dueDateSummary = dueDateEntries[index]
    }

    fun refreshRecurrenceFrom() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_default_recurrence_from, 0
        ).toString()
        val index = recurrenceFromValues.indexOf(currentValue).coerceAtLeast(0)
        recurrenceFromSummary = recurrenceFromEntries[index]
    }

    fun refreshRandomReminder() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_rmd_default_random_hours, 0
        ).toString()
        val index = randomReminderValues.indexOf(currentValue).coerceAtLeast(0)
        randomReminderSummary = randomReminderEntries[index]
    }

    fun refreshRemindersMode() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_default_reminders_mode_key, 0
        ).toString()
        val index = remindersModeValues.indexOf(currentValue).coerceAtLeast(0)
        remindersModeSummary = remindersModeEntries[index]
    }

    fun refreshLocationReminder() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_default_location_reminder_key, 0
        ).toString()
        val index = locationReminderValues.indexOf(currentValue).coerceAtLeast(0)
        locationReminderSummary = locationReminderEntries[index]
    }

    private fun refreshDefaultList() = viewModelScope.launch {
        val defaultFilter = defaultFilterProvider.getDefaultList()
        defaultListName = defaultFilter.title ?: ""
    }

    private fun refreshTags() = viewModelScope.launch {
        defaultTagsSummary = defaultTags()
            .mapNotNull { it.name }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: context.getString(R.string.none)
    }

    private fun refreshCalendarName() {
        val calendarId = preferences.defaultCalendar
        val name = calendarProvider.getCalendar(calendarId)?.name
        calendarName = name ?: context.getString(R.string.dont_add_to_calendar)
    }

    private fun refreshRecurrence() {
        val rrule = preferences.getStringValue(R.string.p_default_recurrence)
        recurrenceSummary = rrule
            ?.takeIf { it.isNotBlank() }
            ?.let {
                try {
                    repeatRuleToString.toString(it)
                } catch (e: Exception) {
                    null
                }
            }
            ?: context.getString(R.string.repeat_option_does_not_repeat)
    }

    fun refreshReminders() {
        val alarms = preferences.defaultAlarms
        remindersSummary = if (alarms.isEmpty()) {
            context.getString(R.string.no_reminders)
        } else {
            val alarmToString = AlarmToString(context)
            alarms.joinToString("\n") {
                alarmToString.toString(it).replace("\n", ", ")
            }
        }
    }

    private fun refreshLocation() = viewModelScope.launch {
        val place = preferences
            .getStringValue(R.string.p_default_location)
            ?.let { locationDao.getByUid(it) }
        if (place == null) {
            hasDefaultLocation = false
            locationName = context.getString(R.string.none)
        } else {
            hasDefaultLocation = true
            locationName = place.displayName
        }
    }
}
