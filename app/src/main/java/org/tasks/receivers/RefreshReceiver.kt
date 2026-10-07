package org.tasks.receivers

import android.content.Context
import android.content.Intent
import com.todoroo.astrid.provider.Astrid2TaskProvider
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.count
import org.tasks.data.dao.TaskDao
import org.tasks.injection.InjectingJobIntentService
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.provider.TasksContentProvider
import org.tasks.pebble.PebbleRefresher
import org.tasks.wear.WearRefresher
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RefreshReceiver : InjectingJobIntentService() {
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var defaultFilterProvider: Lazy<DefaultFilterProvider>
    @Inject lateinit var taskDao: Lazy<TaskDao>
    @Inject lateinit var preferences: Lazy<Preferences>
    @Inject lateinit var wearRefresher: Lazy<WearRefresher>
    @Inject lateinit var pebbleRefresher: Lazy<PebbleRefresher>

    override suspend fun doWork(intent: Intent) {
        if (preferences.get().getBoolean(R.string.p_badges_enabled, true)) {
            val badgeFilter = defaultFilterProvider.get().getBadgeFilter()
            ShortcutBadger.applyCount(context, taskDao.get().count(badgeFilter))
        }
        try {
            val cr = context.contentResolver
            cr.notifyChange(TasksContentProvider.CONTENT_URI, null)
            cr.notifyChange(Astrid2TaskProvider.CONTENT_URI, null)
        } catch (e: Exception) {
            Timber.e(e)
        }
        wearRefresher.get().refresh()
        pebbleRefresher.get().refresh()
    }
}