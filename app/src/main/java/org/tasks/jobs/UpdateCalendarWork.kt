package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.todoroo.astrid.gcal.GCalHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.dao.TaskDao
import org.tasks.injection.BaseWorker
import org.tasks.preferences.PermissionChecker

@HiltWorker
class UpdateCalendarWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val taskDao: TaskDao,
    private val gCalHelper: GCalHelper,
    private val calendarEventProvider: CalendarEventProvider,
    private val permissionChecker: PermissionChecker
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        if (!permissionChecker.canAccessCalendars()) {
            return Result.failure()
        }
        val taskId = inputData.getLong(EXTRA_ID, -1)
        val task = taskDao.fetch(taskId) ?: return Result.failure()
        if (task.hasDueDate()) {
            gCalHelper.updateEvent(task)
        } else {
            calendarEventProvider.deleteEvent(task)
        }
        return Result.success()
    }

    companion object {
        const val EXTRA_ID = "extra_id"
    }
}