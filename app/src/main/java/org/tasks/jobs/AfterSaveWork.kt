package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.repeats.RepeatTaskHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavDao
import org.tasks.data.TaskDao
import org.tasks.injection.BaseWorker

@HiltWorker
class AfterSaveWork @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val repeatTaskHelper: RepeatTaskHelper,
        private val taskDao: TaskDao,
        private val caldavDao: CaldavDao,
        private val gCalHelper: GCalHelper
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val taskId = inputData.getLong(EXTRA_ID, -1)
        val task = taskDao.fetch(taskId) ?: return Result.failure()

        gCalHelper.updateEvent(task)

        if (caldavDao.getAccountForTask(taskId)?.isSuppressRepeatingTasks != true) {
            repeatTaskHelper.handleRepeat(task)
        }
        return Result.success()
    }

    companion object {
        const val EXTRA_ID = "extra_id"
    }
}