package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import org.tasks.R
import org.tasks.activities.ListPicker
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarPicker.newCalendarPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import javax.inject.Inject

private const val FRAG_TAG_DEFAULT_LIST_SELECTION = "frag_tag_default_list_selection"
private const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"
private const val REQUEST_DEFAULT_LIST = 10010
private const val REQUEST_CALENDAR_SELECTION = 10011

class TaskDefaults : InjectingPreferenceFragment() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var calendarProvider: CalendarProvider

    private lateinit var defaultCalendarPref: Preference

    override fun getPreferenceXml() = R.xml.preferences_task_defaults

    override fun setupPreferences(savedInstanceState: Bundle?) {
        defaultCalendarPref = findPreference(R.string.gcal_p_default)
        defaultCalendarPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            newCalendarPicker(this, REQUEST_CALENDAR_SELECTION, getDefaultCalendarName())
                .show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
            false
        }
        val defaultCalendarName: String? = getDefaultCalendarName()
        defaultCalendarPref.summary = defaultCalendarName
            ?: getString(R.string.dont_add_to_calendar)

        findPreference(R.string.p_default_list)
            .setOnPreferenceClickListener {
                ListPicker.newListPicker(
                        defaultFilterProvider.defaultList,
                        this,
                        REQUEST_DEFAULT_LIST)
                    .show(parentFragmentManager, FRAG_TAG_DEFAULT_LIST_SELECTION)
                false
            }
        updateRemoteListSummary()

        requires(device.supportsGeofences(), R.string.p_default_location_reminder_key, R.string.p_default_location_radius)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DEFAULT_LIST) {
            val list: Filter? = data!!.getParcelableExtra(ListPicker.EXTRA_SELECTED_FILTER)
            if (list is GtasksFilter || list is CaldavFilter) {
                defaultFilterProvider.defaultList = list
            } else {
                throw RuntimeException("Unhandled filter type")
            }
            updateRemoteListSummary()
        } else if (requestCode == REQUEST_CALENDAR_SELECTION) {
            if (resultCode == RESULT_OK) {
                preferences.setString(
                    R.string.gcal_p_default,
                    data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
                )
                defaultCalendarPref.summary =
                    data.getStringExtra(CalendarPicker.EXTRA_CALENDAR_NAME)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getDefaultCalendarName(): String? {
        val calendarId = preferences.getStringValue(R.string.gcal_p_default)
        return calendarProvider.getCalendar(calendarId)?.name
    }

    private fun updateRemoteListSummary() {
        val defaultFilter = defaultFilterProvider.defaultList
        findPreference(R.string.p_default_list).summary = defaultFilter.listingTitle
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}