package org.tasks.jobs

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.repeats.RepeatTaskHelper
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavDao
import org.tasks.data.TaskDao
import org.tasks.injection.BaseWorker
import timber.log.Timber

class AfterSaveWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val repeatTaskHelper: RepeatTaskHelper,
        private val taskDao: TaskDao,
        private val caldavDao: CaldavDao
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val taskId = inputData.getLong(EXTRA_ID, -1)
        val task = taskDao.fetch(taskId)
        if (task == null) {
            Timber.e("Missing saved task")
            return Result.failure()
        }

        updateCalendarTitle(task)
        val account = caldavDao.getAccountForTask(taskId)
        if (account == null || !account.isSuppressRepeatingTasks) {
            repeatTaskHelper.handleRepeat(task)
        }
        return Result.success()
    }

    private fun updateCalendarTitle(task: Task) {
        val calendarUri = task.calendarURI
        if (!isNullOrEmpty(calendarUri)) {
            try {
                // change title of calendar event
                val cr = context.contentResolver
                val values = ContentValues()
                values.put(
                        CalendarContract.Events.TITLE,
                        context.getString(R.string.gcal_completed_title, task.title))
                cr.update(Uri.parse(calendarUri), values, null, null)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    companion object {
        const val EXTRA_ID = "extra_id"
    }
}