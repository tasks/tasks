package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.view.View
import android.widget.TextView
import android.widget.Toast.LENGTH_SHORT
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.astrid.gcal.GCalHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarEventProvider
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.toast
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeBase
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CalendarControlSet : TaskEditControlFragment() {
    @BindView(R.id.clear)
    lateinit var cancelButton: View

    @BindView(R.id.calendar_display_which)
    lateinit var calendar: TextView

    @Inject lateinit var activity: Activity
    @Inject lateinit var gcalHelper: GCalHelper
    @Inject lateinit var calendarProvider: CalendarProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var themeBase: ThemeBase
    @Inject lateinit var calendarEventProvider: CalendarEventProvider
    
    override fun onResume() {
        super.onResume()

        val canAccessCalendars = permissionChecker.canAccessCalendars()
        viewModel.eventUri?.let {
            if (canAccessCalendars && !calendarEntryExists(it)) {
                viewModel.eventUri = null
            }
        }
        if (!canAccessCalendars) {
            viewModel.selectedCalendar = null
        }

        refreshDisplayView()
    }

    override val layout = R.layout.control_set_gcal_display

    override val icon = R.drawable.ic_outline_event_24px

    override fun controlId() = TAG

    override val isClickable = true

    @OnClick(R.id.clear)
    fun clearCalendar() {
        if (viewModel.eventUri.isNullOrBlank()) {
            clear()
        } else {
            dialogBuilder
                    .newDialog(R.string.delete_calendar_event_confirmation)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        if (permissionRequestor.requestCalendarPermissions(REQUEST_CODE_CLEAR_EVENT)) {
                            clear()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    private fun clear() {
        viewModel.selectedCalendar = null
        viewModel.eventUri = null
        refreshDisplayView()
    }

    override fun onRowClick() {
        if (viewModel.eventUri.isNullOrBlank()) {
            CalendarPicker.newCalendarPicker(this, REQUEST_CODE_PICK_CALENDAR, calendarName)
                    .show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
        } else if (permissionRequestor.requestCalendarPermissions(REQUEST_CODE_OPEN_EVENT)) {
            openCalendarEvent()
        }
    }

    private fun openCalendarEvent() {
        val cr = activity.contentResolver
        val uri = Uri.parse(viewModel.eventUri)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            cr.query(
                    uri, arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND),
                    null,
                    null,
                    null).use { cursor ->
                if (cursor!!.count == 0) {
                    activity.toast(R.string.calendar_event_not_found, duration = LENGTH_SHORT)
                    viewModel.eventUri = null
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
            activity.toast(R.string.gcal_TEA_error)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_CALENDAR) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.selectedCalendar = data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
                refreshDisplayView()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val calendarName: String?
        get() = viewModel.selectedCalendar?.let { calendarProvider.getCalendar(it)?.name }

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

    private fun refreshDisplayView() = when {
        viewModel.eventUri?.isNotBlank() == true -> {
            calendar.setText(R.string.gcal_TEA_showCalendar_label)
            cancelButton.visibility = View.VISIBLE
        }
        !viewModel.selectedCalendar.isNullOrBlank() -> {
            calendar.text = calendarName
            cancelButton.visibility = View.GONE
        }
        else -> {
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

    companion object {
        const val TAG = R.string.TEA_ctrl_gcal
        private const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"
        private const val REQUEST_CODE_PICK_CALENDAR = 70
        private const val REQUEST_CODE_OPEN_EVENT = 71
        private const val REQUEST_CODE_CLEAR_EVENT = 72
    }
}