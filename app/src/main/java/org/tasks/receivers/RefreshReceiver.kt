package org.tasks.receivers

import android.content.Context
import android.content.Intent
import com.todoroo.astrid.provider.Astrid2TaskProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.dao.TaskDao
import org.tasks.data.count
import org.tasks.injection.InjectingJobIntentService
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.provider.TasksContentProvider
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RefreshReceiver : InjectingJobIntentService() {
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences

    override suspend fun doWork(intent: Intent) {
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
    }
}