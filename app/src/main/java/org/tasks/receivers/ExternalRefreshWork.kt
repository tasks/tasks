package org.tasks.receivers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.todoroo.astrid.provider.Astrid2TaskProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.count
import org.tasks.data.dao.TaskDao
import org.tasks.injection.BaseWorker
import org.tasks.pebble.PebbleRefresher
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.provider.TasksContentProvider
import org.tasks.wear.WearRefresher
import timber.log.Timber

@HiltWorker
class ExternalRefreshWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val wearRefresher: WearRefresher,
    private val pebbleRefresher: PebbleRefresher,
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        if (preferences.getBoolean(R.string.p_badges_enabled, true)) {
            val badgeFilter = defaultFilterProvider.getBadgeFilter()
            ShortcutBadger.applyCount(context, taskDao.count(badgeFilter))
        }
        try {
            val cr = context.contentResolver
            cr.notifyChange(TasksContentProvider.CONTENT_URI, null)
            cr.notifyChange(Astrid2TaskProvider.CONTENT_URI, null)
        } catch (e: Exception) {
            Timber.e(e)
        }
        wearRefresher.refresh()
        pebbleRefresher.refresh()
        return Result.success()
    }

    companion object {
        private const val TAG_EXTERNAL_REFRESH = "tag_external_refresh"

        fun enqueueWork(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                TAG_EXTERNAL_REFRESH,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(ExternalRefreshWork::class.java).build()
            )
        }
    }
}
