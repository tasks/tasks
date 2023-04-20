package org.tasks.preferences.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.todoroo.astrid.dao.Database
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.PermissionUtil
import org.tasks.R
import org.tasks.caldav.VtodoCache
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.TaskDao
import org.tasks.etebase.EtebaseLocalCache
import org.tasks.extensions.Context.toast
import org.tasks.files.FileHelper
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissionRequestor
import org.tasks.preferences.Preferences
import org.tasks.scheduling.CalendarNotificationIntentService
import javax.inject.Inject

private const val REQUEST_CODE_FILES_DIR = 10000

@AndroidEntryPoint
class Advanced : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var database: Database
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var calendarEventProvider: CalendarEventProvider
    @Inject lateinit var permissionRequester: FragmentPermissionRequestor
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var vtodoCache: VtodoCache

    private lateinit var calendarReminderPreference: SwitchPreferenceCompat

    override fun getPreferenceXml() = R.xml.preferences_advanced

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.EPr_manage_delete_completed_gcal)
            .setOnPreferenceClickListener {
                deleteCompletedEvents()
                false
            }

        findPreference(R.string.EPr_manage_delete_all_gcal)
            .setOnPreferenceClickListener {
                deleteAllCalendarEvents()
                false
            }

        findPreference(R.string.EPr_reset_preferences)
            .setOnPreferenceClickListener {
                resetPreferences()
                false
            }

        findPreference(R.string.EPr_delete_task_data)
            .setOnPreferenceClickListener {
                deleteTaskData()
                false
            }

        findPreference(R.string.p_attachment_dir)
            .setOnPreferenceClickListener {
                FileHelper.newDirectoryPicker(
                    this,
                    REQUEST_CODE_FILES_DIR,
                    preferences.attachmentsDirectory
                )
                false
            }
        updateAttachmentDirectory()

        calendarReminderPreference =
            findPreference(R.string.p_calendar_reminders) as SwitchPreferenceCompat
        initializeCalendarReminderPreference()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_FILES_DIR) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.data!!
                requireContext().contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                preferences.setUri(R.string.p_attachment_dir, uri)
                updateAttachmentDirectory()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (PermissionUtil.verifyPermissions(grantResults)) {
                calendarReminderPreference.isChecked = true
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun initializeCalendarReminderPreference() {
        calendarReminderPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == null) {
                    false
                } else if (!(newValue as Boolean)) {
                    true
                } else if (permissionRequester.requestCalendarPermissions()) {
                    CalendarNotificationIntentService.enqueueWork(context)
                    true
                } else {
                    false
                }
            }
        calendarReminderPreference.isChecked =
            calendarReminderPreference.isChecked && permissionChecker.canAccessCalendars()
    }

    private fun updateAttachmentDirectory() {
        findPreference(R.string.p_attachment_dir).summary =
            FileHelper.uri2String(preferences.attachmentsDirectory)
    }

    private fun deleteCompletedEvents() {
        dialogBuilder
            .newDialog()
            .setMessage(R.string.EPr_manage_delete_completed_gcal_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                performAction(R.string.EPr_manage_delete_gcal_status) {
                    calendarEventProvider.deleteEvents(taskDao.getCompletedCalendarEvents())
                    taskDao.clearCompletedCalendarEvents()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteAllCalendarEvents() {
        dialogBuilder
            .newDialog()
            .setMessage(R.string.EPr_manage_delete_all_gcal_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                performAction(
                    R.string.EPr_manage_delete_gcal_status) {
                        calendarEventProvider.deleteEvents(taskDao.getAllCalendarEvents())
                        taskDao.clearAllCalendarEvents()
                    }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performAction(message: Int, callable: suspend () -> Int) = lifecycleScope.launch {
        context?.toast(message, callable())
    }

    private fun resetPreferences() {
        dialogBuilder
            .newDialog()
            .setMessage(R.string.EPr_reset_preferences_warning)
            .setPositiveButton(R.string.EPr_reset_preferences) { _, _ ->
                preferences.reset()
                restart()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteTaskData() {
        dialogBuilder
            .newDialog()
            .setMessage(R.string.EPr_delete_task_data_warning)
            .setPositiveButton(R.string.EPr_delete_task_data) { _, _ ->
                val context = requireContext()
                context.deleteDatabase(database.name)
                vtodoCache.clear()
                EtebaseLocalCache.clear(context)
                restart()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
