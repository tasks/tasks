package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.view.View
import android.view.ViewGroup
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.activity.TaskEditFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.compose.edit.CalendarRow
import org.tasks.extensions.Context.toast
import org.tasks.preferences.PermissionChecker
import org.tasks.themes.TasksTheme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CalendarControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var calendarProvider: CalendarProvider
    @Inject lateinit var permissionChecker: PermissionChecker

    override fun onResume() {
        super.onResume()

        val canAccessCalendars = permissionChecker.canAccessCalendars()
        viewModel.eventUri.value?.let {
            if (canAccessCalendars && !calendarEntryExists(it)) {
                viewModel.eventUri.value = null
            }
        }
        if (!canAccessCalendars) {
            viewModel.selectedCalendar.value = null
        }
    }

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                TasksTheme {
                    CalendarRow(
                        eventUri = viewModel.eventUri.collectAsStateWithLifecycle().value,
                        selectedCalendar = viewModel.selectedCalendar.collectAsStateWithLifecycle().value?.let {
                            calendarProvider.getCalendar(it)?.name
                        },
                        onClick = {
                            if (viewModel.eventUri.value.isNullOrBlank()) {
                                CalendarPicker
                                    .newCalendarPicker(
                                        requireParentFragment(),
                                        TaskEditFragment.REQUEST_CODE_PICK_CALENDAR,
                                        viewModel.selectedCalendar.value,
                                    )
                                    .show(
                                        requireParentFragment().parentFragmentManager,
                                        TaskEditFragment.FRAG_TAG_CALENDAR_PICKER
                                    )
                            } else {
                                openCalendarEvent()
                            }
                        },
                        clear = {
                            viewModel.selectedCalendar.value = null
                            viewModel.eventUri.value = null
                        }
                    )
                }
            }
        }

    override fun controlId() = TAG

    private fun openCalendarEvent() {
        val cr = activity.contentResolver
        val uri = Uri.parse(viewModel.eventUri.value)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            cr.query(
                    uri, arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND),
                    null,
                    null,
                    null).use { cursor ->
                if (cursor!!.count == 0) {
                    activity.toast(R.string.calendar_event_not_found, duration = LENGTH_SHORT)
                    viewModel.eventUri.value = null
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
        val TAG = R.string.TEA_ctrl_gcal
    }
}
