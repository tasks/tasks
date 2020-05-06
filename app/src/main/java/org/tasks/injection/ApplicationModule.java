package org.tasks.injection;

import android.content.Context;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import dagger.Module;
import dagger.Provides;
import org.tasks.analytics.Firebase;
import org.tasks.billing.BillingClient;
import org.tasks.billing.BillingClientImpl;
import org.tasks.billing.Inventory;
import org.tasks.data.AlarmDao;
import org.tasks.data.CaldavDao;
import org.tasks.data.DeletionDao;
import org.tasks.data.FilterDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDao;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.data.UserActivityDao;
import org.tasks.jobs.WorkManager;
import org.tasks.locale.Locale;
import org.tasks.location.Geocoder;
import org.tasks.location.MapboxGeocoder;
import org.tasks.notifications.NotificationDao;

@Module
public class ApplicationModule {

  private final Context context;

  public ApplicationModule(Context context) {
    this.context = context;
  }

  @Provides
  public Locale getLocale() {
    return Locale.getInstance(context);
  }

  @Provides
  @ForApplication
  public Context getApplicationContext() {
    return context;
  }

  @Provides
  @ApplicationScope
  NotificationDao getNotificationDao(Database database) {
    return database.notificationDao();
  }

  @Provides
  @ApplicationScope
  TagDataDao getTagDataDao(Database database) {
    return database.getTagDataDao();
  }

  @Provides
  @ApplicationScope
  UserActivityDao getUserActivityDao(Database database) {
    return database.getUserActivityDao();
  }

  @Provides
  @ApplicationScope
  TaskAttachmentDao getTaskAttachmentDao(Database database) {
    return database.getTaskAttachmentDao();
  }

  @Provides
  @ApplicationScope
  TaskListMetadataDao getTaskListMetadataDao(Database database) {
    return database.getTaskListMetadataDao();
  }

  @Provides
  @ApplicationScope
  GoogleTaskDao getGoogleTaskDao(Database database) {
    return database.getGoogleTaskDao();
  }

  @Provides
  @ApplicationScope
  AlarmDao getAlarmDao(Database database) {
    return database.getAlarmDao();
  }

  @Provides
  @ApplicationScope
  LocationDao getGeofenceDao(Database database) {
    return database.getLocationDao();
  }

  @Provides
  @ApplicationScope
  TagDao getTagDao(Database database) {
    return database.getTagDao();
  }

  @Provides
  @ApplicationScope
  FilterDao getFilterDao(Database database) {
    return database.getFilterDao();
  }

  @Provides
  @ApplicationScope
  GoogleTaskListDao getGoogleTaskListDao(Database database) {
    return database.getGoogleTaskListDao();
  }

  @Provides
  @ApplicationScope
  CaldavDao getCaldavDao(Database database) {
    return database.getCaldavDao();
  }

  @Provides
  @ApplicationScope
  TaskDao getTaskDao(Database database, WorkManager workManager) {
    TaskDao taskDao = database.getTaskDao();
    taskDao.initialize(workManager);
    return taskDao;
  }

  @Provides
  @ApplicationScope
  DeletionDao getDeletionDao(Database database) {
    return database.getDeletionDao();
  }

  @Provides
  BillingClient getBillingClient(Inventory inventory, Firebase firebase) {
    return new BillingClientImpl(context, inventory, firebase);
  }

  @Provides
  Geocoder getGeocoder(@ForApplication Context context) {
    return new MapboxGeocoder(context);
  }
}
