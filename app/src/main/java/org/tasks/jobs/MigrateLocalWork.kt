package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.data.dao.CaldavDao
import org.tasks.injection.BaseWorker
import org.tasks.service.TaskMigrator

@HiltWorker
class MigrateLocalWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val caldavDao: CaldavDao,
    private val taskMigrator: TaskMigrator,
) : BaseWorker(context, workerParams, firebase) {
    override suspend fun run(): Result {
        val localUuid = inputData.getString(EXTRA_LOCAL_ACCOUNT) ?: return Result.failure()
        val tasksUuid = inputData.getString(EXTRA_ACCOUNT) ?: return Result.failure()
        val localAccount = caldavDao.getAccountByUuid(localUuid) ?: return Result.failure()
        val tasksAccount = caldavDao.getAccountByUuid(tasksUuid) ?: return Result.failure()
        taskMigrator.migrateLocalTasks(localAccount, tasksAccount)
        return Result.success()
    }

    companion object {
        const val EXTRA_LOCAL_ACCOUNT = "extra_local_account"
        const val EXTRA_ACCOUNT = "extra_account"
    }
}