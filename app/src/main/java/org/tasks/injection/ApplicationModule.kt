package org.tasks.injection

import android.content.Context
import com.todoroo.astrid.dao.Database
import com.todoroo.astrid.dao.TaskDao
import dagger.Module
import dagger.Provides
import org.tasks.analytics.Firebase
import org.tasks.billing.BillingClient
import org.tasks.billing.BillingClientImpl
import org.tasks.billing.Inventory
import org.tasks.data.*
import org.tasks.jobs.WorkManager
import org.tasks.locale.Locale
import org.tasks.location.Geocoder
import org.tasks.location.MapboxGeocoder
import org.tasks.notifications.NotificationDao

@Module
class ApplicationModule(@get:Provides @get:ApplicationContext val context: Context) {

    @get:Provides
    val locale: Locale
        get() = Locale.getInstance(context)

    @Provides
    @ApplicationScope
    fun getJavaLocale(locale: Locale): java.util.Locale = locale.locale

    @Provides
    @ApplicationScope
    fun getNotificationDao(db: Database): NotificationDao = db.notificationDao()

    @Provides
    @ApplicationScope
    fun getTagDataDao(db: Database): TagDataDao = db.tagDataDao

    @Provides
    @ApplicationScope
    fun getUserActivityDao(db: Database): UserActivityDao = db.userActivityDao

    @Provides
    @ApplicationScope
    fun getTaskAttachmentDao(db: Database): TaskAttachmentDao = db.taskAttachmentDao

    @Provides
    @ApplicationScope
    fun getTaskListMetadataDao(db: Database): TaskListMetadataDao = db.taskListMetadataDao

    @Provides
    @ApplicationScope
    fun getGoogleTaskDao(db: Database): GoogleTaskDao = db.googleTaskDao

    @Provides
    @ApplicationScope
    fun getAlarmDao(db: Database): AlarmDao = db.alarmDao

    @Provides
    @ApplicationScope
    fun getGeofenceDao(db: Database): LocationDao = db.locationDao

    @Provides
    @ApplicationScope
    fun getTagDao(db: Database): TagDao = db.tagDao

    @Provides
    @ApplicationScope
    fun getFilterDao(db: Database): FilterDao = db.filterDao

    @Provides
    @ApplicationScope
    fun getGoogleTaskListDao(db: Database): GoogleTaskListDao = db.googleTaskListDao

    @Provides
    @ApplicationScope
    fun getCaldavDao(db: Database): CaldavDao = db.caldavDao

    @Provides
    @ApplicationScope
    fun getTaskDao(db: Database, workManager: WorkManager): TaskDao {
        val taskDao = db.taskDao
        taskDao.initialize(workManager)
        return taskDao
    }

    @Provides
    @ApplicationScope
    fun getDeletionDao(db: Database): DeletionDao = db.deletionDao

    @Provides
    fun getBillingClient(inventory: Inventory, firebase: Firebase): BillingClient
            = BillingClientImpl(context, inventory, firebase)

    @get:Provides
    val geocoder: Geocoder
        get() = MapboxGeocoder(context)
}