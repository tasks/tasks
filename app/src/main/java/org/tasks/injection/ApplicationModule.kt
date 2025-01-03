package org.tasks.injection

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.tasks.analytics.Firebase
import org.tasks.billing.BillingClient
import org.tasks.billing.BillingClientImpl
import org.tasks.billing.Inventory
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.Astrid2ContentProviderDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.db.Database
import org.tasks.filters.PreferenceDrawerConfiguration
import org.tasks.jobs.WorkManager
import org.tasks.kmp.createDataStore
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.filters.FilterProvider
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.Preferences
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {
    @Provides
    fun getLocale(): Locale {
        return AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .split(",")
            .firstOrNull { it.isNotBlank() }
            ?.let { Locale.forLanguageTag(it) }
            ?: Locale.getDefault()
    }

    @Provides
    @Singleton
    fun getNotificationDao(db: Database): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun getTagDataDao(db: Database): TagDataDao = db.tagDataDao()

    @Provides
    @Singleton
    fun getUserActivityDao(db: Database): UserActivityDao = db.userActivityDao()

    @Provides
    @Singleton
    fun getTaskAttachmentDao(db: Database): TaskAttachmentDao = db.taskAttachmentDao()

    @Provides
    @Singleton
    fun getTaskListMetadataDao(db: Database): TaskListMetadataDao = db.taskListMetadataDao()

    @Provides
    @Singleton
    fun getGoogleTaskDao(db: Database): GoogleTaskDao = db.googleTaskDao()

    @Provides
    @Singleton
    fun getAlarmDao(db: Database): AlarmDao = db.alarmDao()

    @Provides
    @Singleton
    fun getGeofenceDao(db: Database): LocationDao = db.locationDao()

    @Provides
    @Singleton
    fun getTagDao(db: Database): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun getFilterDao(db: Database): FilterDao = db.filterDao()

    @Provides
    @Singleton
    fun getCaldavDao(db: Database): CaldavDao = db.caldavDao()

    @Provides
    @Singleton
    fun getTaskDao(db: Database): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun getDeletionDao(db: Database): DeletionDao = db.deletionDao()

    @Provides
    @Singleton
    fun getContentProviderDao(db: Database): Astrid2ContentProviderDao = db.contentProviderDao()

    @Provides
    @Singleton
    fun getUpgraderDao(db: Database) = db.upgraderDao()

    @Provides
    @Singleton
    fun getPrincipalDao(db: Database) = db.principalDao()

    @Provides
    fun getBillingClient(
        @ApplicationContext context: Context,
        inventory: Inventory,
        firebase: Firebase,
        workManager: WorkManager,
    ): BillingClient = BillingClientImpl(context, inventory, firebase, workManager)

    @Singleton
    @ApplicationScope
    @Provides
    fun providesCoroutineScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Provides
    fun providesNotificationManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Singleton
    @Provides
    fun providesTasksPreferences(@ApplicationContext context: Context) =
        TasksPreferences(createDataStore(context))

    @Provides
    fun providesDrawerConfiguration(preferences: Preferences): DrawerConfiguration =
        PreferenceDrawerConfiguration(preferences)

    @Provides
    fun providesFilterProvider(
        filterDao: FilterDao,
        tagDataDao: TagDataDao,
        caldavDao: CaldavDao,
        drawerConfiguration: DrawerConfiguration,
        locationDao: LocationDao,
        taskDao: TaskDao,
        tasksPreferences: TasksPreferences,
    ) = FilterProvider(
        filterDao = filterDao,
        tagDataDao = tagDataDao,
        caldavDao = caldavDao,
        configuration = drawerConfiguration,
        locationDao = locationDao,
        taskDao = taskDao,
        tasksPreferences = tasksPreferences,
    )
}