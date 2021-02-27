package org.tasks.injection

import android.content.Context
import com.todoroo.astrid.dao.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tasks.analytics.Firebase
import org.tasks.billing.BillingClient
import org.tasks.billing.BillingClientImpl
import org.tasks.billing.Inventory
import org.tasks.data.*
import org.tasks.locale.Locale
import org.tasks.notifications.NotificationDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {

    @Provides
    fun getLocale(@ApplicationContext context: Context): Locale = Locale.getInstance(context)

    @Provides
    @Singleton
    fun getJavaLocale(locale: Locale): java.util.Locale = locale.locale

    @Provides
    @Singleton
    fun getNotificationDao(db: Database): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun getTagDataDao(db: Database): TagDataDao = db.tagDataDao

    @Provides
    @Singleton
    fun getUserActivityDao(db: Database): UserActivityDao = db.userActivityDao

    @Provides
    @Singleton
    fun getTaskAttachmentDao(db: Database): TaskAttachmentDao = db.taskAttachmentDao

    @Provides
    @Singleton
    fun getTaskListMetadataDao(db: Database): TaskListMetadataDao = db.taskListMetadataDao

    @Provides
    @Singleton
    fun getGoogleTaskDao(db: Database): GoogleTaskDao = db.googleTaskDao

    @Provides
    @Singleton
    fun getAlarmDao(db: Database): AlarmDao = db.alarmDao

    @Provides
    @Singleton
    fun getGeofenceDao(db: Database): LocationDao = db.locationDao

    @Provides
    @Singleton
    fun getTagDao(db: Database): TagDao = db.tagDao

    @Provides
    @Singleton
    fun getFilterDao(db: Database): FilterDao = db.filterDao

    @Provides
    @Singleton
    fun getGoogleTaskListDao(db: Database): GoogleTaskListDao = db.googleTaskListDao

    @Provides
    @Singleton
    fun getCaldavDao(db: Database): CaldavDao = db.caldavDao

    @Provides
    @Singleton
    fun getTaskDao(db: Database): TaskDao = db.taskDao

    @Provides
    @Singleton
    fun getDeletionDao(db: Database): DeletionDao = db.deletionDao

    @Provides
    @Singleton
    fun getContentProviderDao(db: Database): ContentProviderDao = db.contentProviderDao

    @Provides
    @Singleton
    fun getUpgraderDao(db: Database) = db.upgraderDao

    @Provides
    @Singleton
    fun getPrincipalDao(db: Database) = db.principalDao

    @Provides
    fun getBillingClient(@ApplicationContext context: Context, inventory: Inventory, firebase: Firebase): BillingClient
            = BillingClientImpl(context, inventory, firebase)
}