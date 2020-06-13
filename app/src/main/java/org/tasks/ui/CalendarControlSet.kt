package org.tasks.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.gcal.GCalHelper
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarEventProvider
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.dialogs.DialogBuilder
import org.tasks.injection.ActivityContext
import org.tasks.injection.FragmentComponent
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeBase
import timber.log.Timber
import javax.inject.Inject

class CalendarControlSet : TaskEditControlFragment() {
    @BindView(R.id.clear)
    lateinit var cancelButton: View

    @BindView(R.id.calendar_display_which)
    lateinit var calendar: TextView

    @Inject @ActivityContext lateinit var activity: Context
    @Inject lateinit var gcalHelper: GCalHelper
    @Inject lateinit var calendarProvider: CalendarProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var themeBase: ThemeBase
    @Inject lateinit var calendarEventProvider: CalendarEventProvider
    
    private var calendarId: String? = null
    private var eventUri: String? = null
    
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val canAccessCalendars = permissionChecker.canAccessCalendars()
        if (savedInstanceState != null) {
            eventUri = savedInstanceState.getString(EXTRA_URI)
            calendarId = savedInstanceState.getString(EXTRA_ID)
        } else if (task.isNew && canAccessCalendars) {
            calendarId = preferences.defaultCalendar
            if (!isNullOrEmpty(calendarId)) {
                try {
                    val defaultCalendar = calendarProvider.getCalendar(calendarId)
                    if (defaultCalendar == null) {
                        calendarId = null
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    firebase.reportException(e)
                    calendarId = null
                }
            }
        } else {
            eventUri = task.calendarURI
        }
        if (canAccessCalendars && !calendarEntryExists(eventUri)) {
            eventUri = null
        }
        refreshDisplayView()
        return view
    }

    override val layout: Int
        get() = R.layout.control_set_gcal_display

    override val icon: Int
        get() = R.drawable.ic_outline_event_24px

    override fun controlId() = TAG

    override fun hasChanges(original: Task): Boolean {
        if (!permissionChecker.canAccessCalendars()) {
            return false
        }
        if (!isNullOrEmpty(calendarId)) {
            return true
        }
        val originalUri = original.calendarURI
        return if (isNullOrEmpty(eventUri) && isNullOrEmpty(originalUri)) {
            false
        } else originalUri != eventUri
    }

    override fun apply(task: Task) {
        if (!permissionChecker.canAccessCalendars()) {
            return
        }
        if (!isNullOrEmpty(task.calendarURI)) {
            if (eventUri == null) {
                calendarEventProvider.deleteEvent(task)
            } else if (!calendarEntryExists(task.calendarURI)) {
                task.calendarURI = ""
            }
        }
        if (!task.hasDueDate()) {
            return
        }
        if (calendarEntryExists(task.calendarURI)) {
            val cr = activity.contentResolver
            try {
                val updateValues = ContentValues()

                // check if we need to update the item
                updateValues.put(CalendarContract.Events.TITLE, task.title)
                updateValues.put(CalendarContract.Events.DESCRIPTION, task.notes)
                gcalHelper.createStartAndEndDate(task, updateValues)
                cr.update(Uri.parse(task.calendarURI), updateValues, null, null)
            } catch (e: Exception) {
                Timber.e(e, "unable-to-update-calendar: %s", task.calendarURI)
            }
        } else if (!isNullOrEmpty(calendarId)) {
            try {
                val values = ContentValues()
                values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
                val uri = gcalHelper.createTaskEvent(task, values)
                if (uri != null) {
                    task.calendarURI = uri.toString()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_URI, eventUri)
        outState.putString(EXTRA_ID, calendarId)
    }

    @OnClick(R.id.clear)
    fun clearCalendar() {
        if (isNullOrEmpty(eventUri)) {
            clear()
        } else {
            dialogBuilder
                    .newDialog(R.string.delete_calendar_event_confirmation)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        if (permissionRequestor.requestCalendarPermissions(REQUEST_CODE_CLEAR_EVENT)) {
                            clear()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
    }

    private fun clear() {
        calendarId = null
        eventUri = null
        refreshDisplayView()
    }

    override fun onRowClick() {
        if (isNullOrEmpty(eventUri)) {
            CalendarPicker.newCalendarPicker(this, REQUEST_CODE_PICK_CALENDAR, calendarName)
                    .show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
        } else {
            if (permissionRequestor.requestCalendarPermissions(REQUEST_CODE_OPEN_EVENT)) {
                openCalendarEvent()
            }
        }
    }

    override val isClickable: Boolean
        get() = true

    private fun openCalendarEvent() {
        val cr = activity.contentResolver
        val uri = Uri.parse(eventUri)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            cr.query(
                    uri, arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND),
                    null,
                    null,
                    null).use { cursor ->
                if (cursor!!.count == 0) {
                    // event no longer exists
                    Toast.makeText(activity, R.string.calendar_event_not_found, Toast.LENGTH_SHORT).show()
                    eventUri = null
                    refreshDisplayView()
                } else {
                    cursor.moveToFirst()
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cursor.getLong(0))
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cursor.getLong(1))
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(activity, R.string.gcal_TEA_error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_CALENDAR) {
            if (resultCode == Activity.RESULT_OK) {
                calendarId = data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
                refreshDisplayView()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val calendarName: String?
        get() {
            if (calendarId == null) {
                return null
            }
            val calendar = calendarProvider.getCalendar(calendarId)
            return calendar?.name
        }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_OPEN_EVENT) {
            if (verifyPermissions(grantResults)) {
                openCalendarEvent()
            }
        } else if (requestCode == REQUEST_CODE_CLEAR_EVENT) {
            if (verifyPermissions(grantResults)) {
                clear()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun refreshDisplayView() {
        if (!isNullOrEmpty(eventUri)) {
            calendar.setText(R.string.gcal_TEA_showCalendar_label)
            cancelButton.visibility = View.VISIBLE
        } else if (calendarId != null) {
            calendar.text = calendarName
            cancelButton.visibility = View.GONE
        } else {
            calendar.text = null
            cancelButton.visibility = View.GONE
        }
    }

    private fun calendarEntryExists(eventUri: String?): Boolean {
        if (isNullOrEmpty(eventUri)) {
            return false
        }
        try {
            val uri = Uri.parse(eventUri)
            val contentResolver = activity.contentResolver
            contentResolver.query(
                    uri, arrayOf(CalendarContract.Events.DTSTART), null, null, null).use { cursor ->
                if (cursor!!.count != 0) {
                    return true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "%s: %s", eventUri, e.message)
        }
        return false
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    companion object {
        const val TAG = R.string.TEA_ctrl_gcal
        private const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"
        private const val REQUEST_CODE_PICK_CALENDAR = 70
        private const val REQUEST_CODE_OPEN_EVENT = 71
        private const val REQUEST_CODE_CLEAR_EVENT = 72
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_ID = "extra_id"
    }
}