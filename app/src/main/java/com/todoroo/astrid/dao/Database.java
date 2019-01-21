/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.dao;

import androidx.room.RoomDatabase;
import com.todoroo.astrid.data.Task;
import org.tasks.data.Alarm;
import org.tasks.data.AlarmDao;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.DeletionDao;
import org.tasks.data.Filter;
import org.tasks.data.FilterDao;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.notifications.Notification;
import org.tasks.notifications.NotificationDao;

@androidx.room.Database(
    entities = {
      Notification.class,
      TagData.class,
      UserActivity.class,
      TaskAttachment.class,
      TaskListMetadata.class,
      Task.class,
      Alarm.class,
      Location.class,
      Tag.class,
      GoogleTask.class,
      Filter.class,
      GoogleTaskList.class,
      CaldavCalendar.class,
      CaldavTask.class,
      CaldavAccount.class,
      GoogleTaskAccount.class
    },
    version = 60)
public abstract class Database extends RoomDatabase {

  public static final String NAME = "database";
  private Runnable onDatabaseUpdated;

  public abstract NotificationDao notificationDao();

  public abstract TagDataDao getTagDataDao();

  public abstract UserActivityDao getUserActivityDao();

  public abstract TaskAttachmentDao getTaskAttachmentDao();

  public abstract TaskListMetadataDao getTaskListMetadataDao();

  public abstract AlarmDao getAlarmDao();

  public abstract LocationDao getLocationDao();

  public abstract TagDao getTagDao();

  public abstract GoogleTaskDao getGoogleTaskDao();

  public abstract FilterDao getFilterDao();

  public abstract GoogleTaskListDao getGoogleTaskListDao();

  public abstract TaskDao getTaskDao();

  public abstract CaldavDao getCaldavDao();

  public abstract DeletionDao getDeletionDao();

  // --- implementation

  public String getName() {
    return NAME;
  }

  public Database init(Runnable onDatabaseUpdated) {
    this.onDatabaseUpdated = onDatabaseUpdated;
    return this;
  }

  public void onDatabaseUpdated() {
    if (onDatabaseUpdated != null) {
      onDatabaseUpdated.run();
    }
  }

  /** @return human-readable database name for debugging */
  @Override
  public String toString() {
    return "DB:" + getName();
  }
}
