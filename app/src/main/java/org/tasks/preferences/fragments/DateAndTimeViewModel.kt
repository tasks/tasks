package org.tasks.preferences.fragments

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay
import javax.inject.Inject

@HiltViewModel
class DateAndTimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
) : ViewModel() {

    var fullDateEnabled by mutableStateOf(false)
        private set
    var morningSummary by mutableStateOf("")
        private set
    var afternoonSummary by mutableStateOf("")
        private set
    var eveningSummary by mutableStateOf("")
        private set
    var nightSummary by mutableStateOf("")
        private set
    var autoDismissListEnabled by mutableStateOf(false)
        private set
    var autoDismissEditEnabled by mutableStateOf(false)
        private set
    var autoDismissWidgetEnabled by mutableStateOf(false)
        private set
    var showAutoDismissInfo by mutableStateOf(false)
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        fullDateEnabled = preferences.getBoolean(R.string.p_always_display_full_date, false)
        refreshMorning()
        refreshAfternoon()
        refreshEvening()
        refreshNight()
        autoDismissListEnabled = preferences.getBoolean(
            R.string.p_auto_dismiss_datetime_list_screen, false,
        )
        autoDismissEditEnabled = preferences.getBoolean(
            R.string.p_auto_dismiss_datetime_edit_screen, false,
        )
        autoDismissWidgetEnabled = preferences.getBoolean(
            R.string.p_auto_dismiss_datetime_widget, false,
        )
    }

    fun updateFullDate(enabled: Boolean) {
        preferences.setBoolean(R.string.p_always_display_full_date, enabled)
        fullDateEnabled = enabled
    }

    fun updateAutoDismissList(enabled: Boolean) {
        preferences.setBoolean(R.string.p_auto_dismiss_datetime_list_screen, enabled)
        autoDismissListEnabled = enabled
    }

    fun updateAutoDismissEdit(enabled: Boolean) {
        preferences.setBoolean(R.string.p_auto_dismiss_datetime_edit_screen, enabled)
        autoDismissEditEnabled = enabled
    }

    fun updateAutoDismissWidget(enabled: Boolean) {
        preferences.setBoolean(R.string.p_auto_dismiss_datetime_widget, enabled)
        autoDismissWidgetEnabled = enabled
    }

    fun openAutoDismissInfo() {
        showAutoDismissInfo = true
    }

    fun dismissAutoDismissInfo() {
        showAutoDismissInfo = false
    }

    fun getMillisOfDay(prefKey: Int, defaultRes: Int): Int =
        preferences.getInt(prefKey, context.resources.getInteger(defaultRes))

    fun setTimePreference(prefKey: Int, millisOfDay: Int) {
        preferences.setInt(prefKey, millisOfDay)
    }

    sealed interface TimePickerResult {
        data object Success : TimePickerResult
        data class Error(val messageResId: Int, val setting: Int, val relative: Int) : TimePickerResult
    }

    fun handleMorningResult(millisOfDay: Int): TimePickerResult {
        if (millisOfDay >= getMillisOfDay(R.string.p_date_shortcut_afternoon, R.integer.default_afternoon)) {
            return TimePickerResult.Error(R.string.date_shortcut_must_come_before, R.string.date_shortcut_morning, R.string.date_shortcut_afternoon)
        }
        setTimePreference(R.string.p_date_shortcut_morning, millisOfDay)
        refreshMorning()
        return TimePickerResult.Success
    }

    fun handleAfternoonResult(millisOfDay: Int): TimePickerResult {
        if (millisOfDay <= getMillisOfDay(R.string.p_date_shortcut_morning, R.integer.default_morning)) {
            return TimePickerResult.Error(R.string.date_shortcut_must_come_after, R.string.date_shortcut_afternoon, R.string.date_shortcut_morning)
        }
        if (millisOfDay >= getMillisOfDay(R.string.p_date_shortcut_evening, R.integer.default_evening)) {
            return TimePickerResult.Error(R.string.date_shortcut_must_come_before, R.string.date_shortcut_afternoon, R.string.date_shortcut_evening)
        }
        setTimePreference(R.string.p_date_shortcut_afternoon, millisOfDay)
        refreshAfternoon()
        return TimePickerResult.Success
    }

    fun handleEveningResult(millisOfDay: Int): TimePickerResult {
        if (millisOfDay <= getMillisOfDay(R.string.p_date_shortcut_afternoon, R.integer.default_afternoon)) {
            return TimePickerResult.Error(R.string.date_shortcut_must_come_after, R.string.date_shortcut_evening, R.string.date_shortcut_afternoon)
        }
        if (millisOfDay >= getMillisOfDay(R.string.p_date_shortcut_night, R.integer.default_night)) {
            return TimePickerResult.Error(R.string.date_shortcut_must_come_before, R.string.date_shortcut_evening, R.string.date_shortcut_night)
        }
        setTimePreference(R.string.p_date_shortcut_evening, millisOfDay)
        refreshEvening()
        return TimePickerResult.Success
    }

    fun handleNightResult(millisOfDay: Int): TimePickerResult {
        if (millisOfDay <= getMillisOfDay(R.string.p_date_shortcut_evening, R.integer.default_evening)) {
            return TimePickerResult.Error(R.string.date_shortcut_must_come_after, R.string.date_shortcut_night, R.string.date_shortcut_evening)
        }
        setTimePreference(R.string.p_date_shortcut_night, millisOfDay)
        refreshNight()
        return TimePickerResult.Success
    }

    private fun formatTime(millisOfDay: Int): String =
        getTimeString(
            currentTimeMillis().withMillisOfDay(millisOfDay),
            context.is24HourFormat,
        )

    private fun refreshMorning() {
        morningSummary = formatTime(
            getMillisOfDay(R.string.p_date_shortcut_morning, R.integer.default_morning)
        )
    }

    private fun refreshAfternoon() {
        afternoonSummary = formatTime(
            getMillisOfDay(R.string.p_date_shortcut_afternoon, R.integer.default_afternoon)
        )
    }

    private fun refreshEvening() {
        eveningSummary = formatTime(
            getMillisOfDay(R.string.p_date_shortcut_evening, R.integer.default_evening)
        )
    }

    private fun refreshNight() {
        nightSummary = formatTime(
            getMillisOfDay(R.string.p_date_shortcut_night, R.integer.default_night)
        )
    }
}
