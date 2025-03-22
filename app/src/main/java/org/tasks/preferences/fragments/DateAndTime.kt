package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.dialogs.MyTimePickerDialog.Companion.newTimePicker
import org.tasks.extensions.Context.toast
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import org.tasks.ui.TimePreference
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

private const val REQUEST_MORNING = 10007
private const val REQUEST_AFTERNOON = 10008
private const val REQUEST_EVENING = 10009
private const val REQUEST_NIGHT = 10010

@AndroidEntryPoint
class DateAndTime : InjectingPreferenceFragment(), Preference.OnPreferenceChangeListener {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale

    override fun getPreferenceXml() = R.xml.preferences_date_and_time

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        val startOfWeekPreference: ListPreference = getStartOfWeekPreference()
        startOfWeekPreference.entries = getWeekdayEntries()
        startOfWeekPreference.onPreferenceChangeListener = this

        initializeTimePreference(getMorningPreference(), REQUEST_MORNING)
        initializeTimePreference(getAfternoonPreference(), REQUEST_AFTERNOON)
        initializeTimePreference(getEveningPreference(), REQUEST_EVENING)
        initializeTimePreference(getNightPreference(), REQUEST_NIGHT)

        updateStartOfWeek(preferences.getStringValue(R.string.p_start_of_week)!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MORNING) {
            if (resultCode == RESULT_OK) {
                getMorningPreference().handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_AFTERNOON) {
            if (resultCode == RESULT_OK) {
                getAfternoonPreference().handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_EVENING) {
            if (resultCode == RESULT_OK) {
                getEveningPreference().handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_NIGHT) {
            if (resultCode == RESULT_OK) {
                getNightPreference().handleTimePickerActivityIntent(data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun initializeTimePreference(preference: TimePreference, requestCode: Int) {
        preference.onPreferenceChangeListener = this
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val current = DateTime().withMillisOfDay(preference.millisOfDay)
            newTimePicker(this, requestCode, current.millis)
                .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
            false
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference == getStartOfWeekPreference()) {
            updateStartOfWeek(newValue.toString())
        } else {
            val millisOfDay = newValue as Int
            if (preference == getMorningPreference()) {
                if (millisOfDay >= getAfternoonPreference().millisOfDay) {
                    mustComeBefore(R.string.date_shortcut_morning, R.string.date_shortcut_afternoon)
                    return false
                }
            } else if (preference == getAfternoonPreference()) {
                if (millisOfDay <= getMorningPreference().millisOfDay) {
                    mustComeAfter(R.string.date_shortcut_afternoon, R.string.date_shortcut_morning)
                    return false
                } else if (millisOfDay >= getEveningPreference().millisOfDay) {
                    mustComeBefore(R.string.date_shortcut_afternoon, R.string.date_shortcut_evening)
                    return false
                }
            } else if (preference == getEveningPreference()) {
                if (millisOfDay <= getAfternoonPreference().millisOfDay) {
                    mustComeAfter(R.string.date_shortcut_evening, R.string.date_shortcut_afternoon)
                    return false
                } else if (millisOfDay >= getNightPreference().millisOfDay) {
                    mustComeBefore(R.string.date_shortcut_evening, R.string.date_shortcut_night)
                    return false
                }
            } else if (preference == getNightPreference()) {
                if (millisOfDay <= getEveningPreference().millisOfDay) {
                    mustComeAfter(R.string.date_shortcut_night, R.string.date_shortcut_evening)
                    return false
                }
            }
        }
        return true
    }

    private fun mustComeBefore(settingResId: Int, relativeResId: Int) {
        invalidSetting(R.string.date_shortcut_must_come_before, settingResId, relativeResId)
    }

    private fun mustComeAfter(settingResId: Int, relativeResId: Int) {
        invalidSetting(R.string.date_shortcut_must_come_after, settingResId, relativeResId)
    }

    private fun invalidSetting(errorResId: Int, settingResId: Int, relativeResId: Int) =
        context?.toast(errorResId, getString(settingResId), getString(relativeResId))

    private fun updateStartOfWeek(value: String) {
        val preference = getStartOfWeekPreference()
        val index = preference.findIndexOfValue(value)
        val summary: String? = getWeekdayEntries()[index]
        preference.summary = summary
    }

    private fun getStartOfWeekPreference(): ListPreference =
            findPreference(R.string.p_start_of_week) as ListPreference

    private fun getWeekdayDisplayName(dayOfWeek: DayOfWeek): String =
            dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    private fun getMorningPreference(): TimePreference =
            getTimePreference(R.string.p_date_shortcut_morning)

    private fun getAfternoonPreference(): TimePreference =
            getTimePreference(R.string.p_date_shortcut_afternoon)

    private fun getEveningPreference(): TimePreference =
            getTimePreference(R.string.p_date_shortcut_evening)

    private fun getNightPreference(): TimePreference =
            getTimePreference(R.string.p_date_shortcut_night)

    private fun getTimePreference(resId: Int): TimePreference =
            findPreference(resId) as TimePreference

    private fun getWeekdayEntries(): Array<String?> = arrayOf(
        getString(R.string.use_locale_default),
        getWeekdayDisplayName(DayOfWeek.SUNDAY),
        getWeekdayDisplayName(DayOfWeek.MONDAY),
        getWeekdayDisplayName(DayOfWeek.TUESDAY),
        getWeekdayDisplayName(DayOfWeek.WEDNESDAY),
        getWeekdayDisplayName(DayOfWeek.THURSDAY),
        getWeekdayDisplayName(DayOfWeek.FRIDAY),
        getWeekdayDisplayName(DayOfWeek.SATURDAY)
    )
}