package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import org.tasks.PermissionUtil
import org.tasks.R
import org.tasks.activities.CalendarSelectionActivity
import org.tasks.activities.RemoteListPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionRequestor
import org.tasks.preferences.Preferences
import javax.inject.Inject

private const val FRAG_TAG_REMOTE_LIST_SELECTION = "frag_tag_remote_list_selection"
private const val REQUEST_REMOTE_LIST = 10010
private const val REQUEST_CALENDAR_SELECTION = 10011

class TaskDefaults : InjectingPreferenceFragment() {

    @Inject lateinit var permissionRequester: FragmentPermissionRequestor
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var calendarProvider: CalendarProvider

    private lateinit var defaultCalendarPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_task_defaults, rootKey)

        defaultCalendarPref = findPreference(R.string.gcal_p_default)
        defaultCalendarPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (permissionRequester.requestCalendarPermissions()) {
                startCalendarSelectionActivity()
            }
            false
        }
        val defaultCalendarName: String? = getDefaultCalendarName()
        defaultCalendarPref.summary = defaultCalendarName
            ?: getString(R.string.dont_add_to_calendar)

        findPreference(R.string.p_default_remote_list)
            .setOnPreferenceClickListener {
                RemoteListPicker.newRemoteListSupportPicker(
                    defaultFilterProvider.defaultRemoteList,
                    this,
                    REQUEST_REMOTE_LIST
                )
                    .show(parentFragmentManager, FRAG_TAG_REMOTE_LIST_SELECTION)
                false
            }
        updateRemoteListSummary()

        requires(device.supportsGeofences(), R.string.p_default_location_reminder_key)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (PermissionUtil.verifyPermissions(grantResults)) {
                startCalendarSelectionActivity()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_REMOTE_LIST) {
            val list: Filter? = data!!.getParcelableExtra(RemoteListPicker.EXTRA_SELECTED_FILTER)
            if (list == null) {
                preferences.setString(R.string.p_default_remote_list, null)
            } else if (list is GtasksFilter || list is CaldavFilter) {
                defaultFilterProvider.defaultRemoteList = list
            } else {
                throw RuntimeException("Unhandled filter type")
            }
            updateRemoteListSummary()
        } else if (requestCode == REQUEST_CALENDAR_SELECTION) {
            if (resultCode == RESULT_OK) {
                preferences.setString(
                    R.string.gcal_p_default,
                    data!!.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_ID)
                )
                defaultCalendarPref.summary =
                    data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_NAME)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getDefaultCalendarName(): String? {
        val calendarId = preferences.getStringValue(R.string.gcal_p_default)
        return calendarProvider.getCalendar(calendarId)?.name
    }

    private fun startCalendarSelectionActivity() {
        val intent = Intent(context, CalendarSelectionActivity::class.java)
        intent.putExtra(CalendarSelectionActivity.EXTRA_CALENDAR_NAME, getDefaultCalendarName())
        startActivityForResult(intent, REQUEST_CALENDAR_SELECTION)
    }

    private fun updateRemoteListSummary() {
        val defaultFilter = defaultFilterProvider.defaultRemoteList
        findPreference(R.string.p_default_remote_list).summary =
            if (defaultFilter == null) getString(R.string.dont_sync)
            else defaultFilter.listingTitle
    }

    override fun inject(component: FragmentComponent) {
        component.inject(this);
    }
}