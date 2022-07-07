package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.compose.DisabledText
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.extensions.Context.toast
import org.tasks.preferences.PermissionChecker
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CalendarControlSet : TaskEditControlComposeFragment() {
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

    @Composable
    override fun Body() {
        val eventUri = viewModel.eventUri.collectAsStateLifecycleAware().value
        val selectedCalendar =
            viewModel.selectedCalendar.collectAsStateLifecycleAware().value?.let {
                calendarProvider.getCalendar(it)?.name
            }
        if (eventUri?.isNotBlank() == true) {
            Row {
                Text(
                    text = stringResource(id = R.string.gcal_TEA_showCalendar_label),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 20.dp)
                )
                IconButton(
                    onClick = { clear() },
                    Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.delete),
                        modifier = Modifier.alpha(ContentAlpha.medium),
                    )
                }
            }
        } else if (selectedCalendar?.isNotBlank() == true) {
            Text(
                text = selectedCalendar,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            DisabledText(
                text = stringResource(id = R.string.dont_add_to_calendar),
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }

    override val icon = R.drawable.ic_outline_event_24px

    override fun controlId() = TAG

    override val isClickable = true

    private fun clear() {
        viewModel.selectedCalendar.value = null
        viewModel.eventUri.value = null
    }

    override fun onRowClick() {
        if (viewModel.eventUri.value.isNullOrBlank()) {
            CalendarPicker.newCalendarPicker(this, REQUEST_CODE_PICK_CALENDAR, calendarName)
                    .show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
        } else {
            openCalendarEvent()
        }
    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_CALENDAR) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.selectedCalendar.value = data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val calendarName: String?
        get() = viewModel.selectedCalendar.value?.let { calendarProvider.getCalendar(it)?.name }

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
    }
}