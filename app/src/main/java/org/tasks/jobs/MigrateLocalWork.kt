package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.CaldavDao
import org.tasks.injection.BaseWorker
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters

class MigrateLocalWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val clientProvider: CaldavClientProvider,
        private val caldavDao: CaldavDao,
        private val preferences: Preferences,
        private val syncAdapters: SyncAdapters
) : BaseWorker(context, workerParams, firebase) {
    override suspend fun run(): Result {
        val uuid = inputData.getString(EXTRA_ACCOUNT) ?: return Result.failure()
        val caldavAccount = caldavDao.getAccountByUuid(uuid) ?: return Result.failure()
        val caldavClient = clientProvider.forAccount(caldavAccount)
        caldavDao.getCalendarsByAccount(CaldavDao.LOCAL).forEach {
            it.url = caldavClient.makeCollection(it.name!!, it.color)
            it.account = caldavAccount.uuid
            caldavDao.update(it)
        }
        preferences.setBoolean(R.string.p_lists_enabled, false)
        syncAdapters.sync()
        return Result.success()
    }

    override fun destroy() {
        clientProvider.dispose()
    }

    companion object {
        const val EXTRA_ACCOUNT = "extra_account"
    }
}