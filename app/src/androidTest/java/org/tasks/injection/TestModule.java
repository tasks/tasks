package org.tasks.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;

import org.tasks.db.Migrations;
import org.tasks.notifications.NotificationDao;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissivePermissionChecker;

import dagger.Module;
import dagger.Provides;

@Module
public class TestModule {
    private Context context;

    public TestModule(Context context) {
        this.context = context;
    }

    @Provides
    @ApplicationScope
    public Database getDatabase() {
        return Room.inMemoryDatabaseBuilder(context, Database.class)
                .fallbackToDestructiveMigration()
                .addCallback(Migrations.ON_CREATE)
                .build();
    }

    @Provides
    public NotificationDao getNotificationDao(Database appDatabase) {
        return appDatabase.notificationDao();
    }

    @Provides
    public TagDataDao getTagDataDao(Database database) {
        return database.getTagDataDao();
    }

    @Provides
    public UserActivityDao getUserActivityDao(Database database) {
        return database.getUserActivityDao();
    }

    @Provides
    public TaskListMetadataDao getTaskListMetadataDao(Database database) {
        return database.getTaskListMetadataDao();
    }

    @Provides
    public StoreObjectDao getStoreObjectDao(Database database) {
        return database.getStoreObjectDao();
    }

    @ApplicationScope
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }

    @Provides
    public PermissionChecker getPermissionChecker() {
        return new PermissivePermissionChecker(context);
    }
}
