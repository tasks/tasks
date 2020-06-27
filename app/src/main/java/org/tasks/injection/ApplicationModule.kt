package org.tasks.injection

import android.content.Context
import com.todoroo.astrid.dao.Database
import com.todoroo.astrid.dao.TaskDaoBlocking
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.analytics.Firebase
import org.tasks.billing.BillingClient
import org.tasks.billing.BillingClientImpl
import org.tasks.billing.Inventory
import org.tasks.data.*
import org.tasks.jobs.WorkManager
import org.tasks.locale.Locale
import org.tasks.location.Geocoder
import org.tasks.location.MapboxGeocoder
import org.tasks.notifications.NotificationDaoBlocking
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
class ApplicationModule {

    @Provides
    fun getLocale(@ApplicationContext context: Context): Locale = Locale.getInstance(context)

    @Provides
    @Singleton
    fun getJavaLocale(locale: Locale): java.util.Locale = locale.locale

    @Provides
    @Singleton
    fun getNotificationDao(db: Database): NotificationDaoBlocking = db.notificationDao()

    @Provides
    @Singleton
    fun getTagDataDao(db: Database): TagDataDaoBlocking = db.tagDataDao

    @Provides
    @Singleton
    fun getUserActivityDao(db: Database): UserActivityDaoBlocking = db.userActivityDao

    @Provides
    @Singleton
    fun getTaskAttachmentDao(db: Database): TaskAttachmentDaoBlocking = db.taskAttachmentDao

    @Provides
    @Singleton
    fun getTaskListMetadataDao(db: Database): TaskListMetadataDaoBlocking = db.taskListMetadataDao

    @Provides
    @Singleton
    fun getGoogleTaskDao(db: Database): GoogleTaskDaoBlocking = db.googleTaskDao

    @Provides
    @Singleton
    fun getAlarmDao(db: Database): AlarmDaoBlocking = db.alarmDao

    @Provides
    @Singleton
    fun getGeofenceDao(db: Database): LocationDaoBlocking = db.locationDao

    @Provides
    @Singleton
    fun getTagDao(db: Database): TagDaoBlocking = db.tagDao

    @Provides
    @Singleton
    fun getFilterDao(db: Database): FilterDaoBlocking = db.filterDao

    @Provides
    @Singleton
    fun getGoogleTaskListDao(db: Database): GoogleTaskListDaoBlocking = db.googleTaskListDao

    @Provides
    @Singleton
    fun getCaldavDao(db: Database): CaldavDaoBlocking = db.caldavDao

    @Provides
    @Singleton
    fun getTaskDao(db: Database, workManager: WorkManager): TaskDaoBlocking {
        val taskDao = db.taskDao
        taskDao.initialize(workManager)
        return taskDao
    }

    @Provides
    @Singleton
    fun getDeletionDao(db: Database): DeletionDaoBlocking = db.deletionDao

    @Provides
    @Singleton
    fun getContentProviderDao(db: Database): ContentProviderDaoBlocking = db.contentProviderDao

    @Provides
    fun getBillingClient(@ApplicationContext context: Context, inventory: Inventory, firebase: Firebase): BillingClient
            = BillingClientImpl(context, inventory, firebase)

    @Provides
    fun getGeocoder(@ApplicationContext context: Context): Geocoder = MapboxGeocoder(context)
}