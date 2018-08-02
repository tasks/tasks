package org.tasks.injection;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;

import android.arch.persistence.room.Room;
import android.content.Context;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import dagger.Module;
import dagger.Provides;
import org.tasks.analytics.Tracker;
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
import org.tasks.db.Migrations;
import org.tasks.locale.Locale;
import org.tasks.notifications.NotificationDao;
import org.tasks.security.Encryption;
import org.tasks.security.KeyStoreEncryption;
import org.tasks.security.NoEncryption;

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
  public Database getAppDatabase(Tracker tracker) {
    return Room.databaseBuilder(context, Database.class, Database.NAME)
        .allowMainThreadQueries() // TODO: remove me
        .addMigrations(Migrations.MIGRATIONS)
        .build()
        .init(tracker, () -> Astrid2TaskProvider.notifyDatabaseModification(context));
  }

  @Provides
  @ApplicationScope
  public NotificationDao getNotificationDao(Database database) {
    return database.notificationDao();
  }

  @Provides
  @ApplicationScope
  public TagDataDao getTagDataDao(Database database) {
    return database.getTagDataDao();
  }

  @Provides
  @ApplicationScope
  public UserActivityDao getUserActivityDao(Database database) {
    return database.getUserActivityDao();
  }

  @Provides
  @ApplicationScope
  public TaskAttachmentDao getTaskAttachmentDao(Database database) {
    return database.getTaskAttachmentDao();
  }

  @Provides
  @ApplicationScope
  public TaskListMetadataDao getTaskListMetadataDao(Database database) {
    return database.getTaskListMetadataDao();
  }

  @Provides
  @ApplicationScope
  public GoogleTaskDao getGoogleTaskDao(Database database) {
    return database.getGoogleTaskDao();
  }

  @Provides
  @ApplicationScope
  public AlarmDao getAlarmDao(Database database) {
    return database.getAlarmDao();
  }

  @Provides
  @ApplicationScope
  public LocationDao getGeofenceDao(Database database) {
    return database.getLocationDao();
  }

  @Provides
  @ApplicationScope
  public TagDao getTagDao(Database database) {
    return database.getTagDao();
  }

  @Provides
  @ApplicationScope
  public FilterDao getFilterDao(Database database) {
    return database.getFilterDao();
  }

  @Provides
  @ApplicationScope
  public GoogleTaskListDao getGoogleTaskListDao(Database database) {
    return database.getGoogleTaskListDao();
  }

  @Provides
  @ApplicationScope
  public CaldavDao getCaldavDao(Database database) {
    return database.getCaldavDao();
  }

  @Provides
  @ApplicationScope
  public TaskDao getTaskDao(Database database) {
    TaskDao taskDao = database.getTaskDao();
    taskDao.initialize(context);
    return taskDao;
  }

  @Provides
  @ApplicationScope
  public DeletionDao getDeletionDao(Database database) {
    return database.getDeletionDao();
  }

  @Provides
  @ApplicationScope
  public Encryption getEncryption() {
    return atLeastMarshmallow() ? new KeyStoreEncryption() : new NoEncryption();
  }
}
