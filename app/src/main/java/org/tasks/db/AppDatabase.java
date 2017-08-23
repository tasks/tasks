package org.tasks.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import org.tasks.notifications.Notification;
import org.tasks.notifications.NotificationDao;

@Database(entities = {Notification.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NotificationDao notificationDao();
}
