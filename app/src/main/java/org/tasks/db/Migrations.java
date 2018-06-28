package org.tasks.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import timber.log.Timber;

public class Migrations {

  private static final Migration MIGRATION_35_36 =
      new Migration(35, 36) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL("ALTER TABLE `tagdata` ADD COLUMN `color` INTEGER DEFAULT -1");
        }
      };

  private static final Migration MIGRATION_36_37 =
      new Migration(36, 37) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL("ALTER TABLE `store` ADD COLUMN `deleted` INTEGER DEFAULT 0");
        }
      };

  private static final Migration MIGRATION_37_38 =
      new Migration(37, 38) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          try {
            database.execSQL("ALTER TABLE `store` ADD COLUMN `value4` TEXT DEFAULT -1");
          } catch (SQLiteException e) {
            Timber.w(e);
          }
        }
      };

  private static final Migration MIGRATION_38_39 =
      new Migration(38, 39) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `notification` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `type` INTEGER NOT NULL)");
          database.execSQL(
              "CREATE UNIQUE INDEX `index_notification_task` ON `notification` (`task`)");
        }
      };

  private static final Migration MIGRATION_46_47 =
      new Migration(46, 47) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `alarms` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `time` INTEGER NOT NULL)");
          database.execSQL(
              "INSERT INTO `alarms` (`task`, `time`) SELECT `task`, `value` FROM `metadata` WHERE `key` = 'alarm' AND `deleted` = 0");
          database.execSQL("DELETE FROM `metadata` WHERE `key` = 'alarm'");
        }
      };

  private static final Migration MIGRATION_47_48 =
      new Migration(47, 48) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `locations` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `radius` INTEGER NOT NULL)");
          database.execSQL(
              "INSERT INTO `locations` (`task`, `name`, `latitude`, `longitude`, `radius`) "
                  + "SELECT `task`, `value`, `value2`, `value3`, `value4` FROM `metadata` WHERE `key` = 'geofence' AND `deleted` = 0");
          database.execSQL("DELETE FROM `metadata` WHERE `key` = 'geofence'");
        }
      };

  private static final Migration MIGRATION_48_49 =
      new Migration(48, 49) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `tags` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `tag_uid` TEXT, `task_uid` TEXT)");
          database.execSQL(
              "INSERT INTO `tags` (`task`, `name`, `tag_uid`, `task_uid`) "
                  + "SELECT `task`, `value`, `value2`, `value3` FROM `metadata` WHERE `key` = 'tags-tag' AND `deleted` = 0");
          database.execSQL("DELETE FROM `metadata` WHERE `key` = 'tags-tag'");
        }
      };

  private static final Migration MIGRATION_49_50 =
      new Migration(49, 50) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `google_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `remote_id` TEXT, `list_id` TEXT, `parent` INTEGER NOT NULL, `indent` INTEGER NOT NULL, `order` INTEGER NOT NULL, `remote_order` INTEGER NOT NULL, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL)");
          database.execSQL(
              "INSERT INTO `google_tasks` (`task`, `remote_id`, `list_id`, `parent`, `indent`, `order`, `remote_order`, `last_sync`, `deleted`) "
                  + "SELECT `task`, `value`, `value2`, IFNULL(`value3`, 0), IFNULL(`value4`, 0), IFNULL(`value5`, 0), IFNULL(`value6`, 0), IFNULL(`value7`, 0), IFNULL(`deleted`, 0) FROM `metadata` WHERE `key` = 'gtasks'");
          database.execSQL("DROP TABLE IF EXISTS `metadata`");
        }
      };

  private static final Migration MIGRATION_50_51 =
      new Migration(50, 51) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `filters` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `sql` TEXT, `values` TEXT, `criterion` TEXT)");
          database.execSQL(
              "INSERT INTO `filters` (`title`, `sql`, `values`, `criterion`) "
                  + "SELECT `item`, `value`, `value2`, `value3` FROM `store` WHERE `type` = 'filter' AND `deleted` = 0");
          database.execSQL("DELETE FROM `store` WHERE `type` = 'filter'");
        }
      };

  private static final Migration MIGRATION_51_52 =
      new Migration(51, 52) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `google_task_lists` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remote_id` TEXT, `title` TEXT, `remote_order` INTEGER NOT NULL, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `color` INTEGER)");
          database.execSQL(
              "INSERT INTO `google_task_lists` (`remote_id`, `title`, `remote_order`, `last_sync`, `color`, `deleted`) "
                  + "SELECT `item`, `value`, `value2`, `value3`, `value4`, `deleted` FROM `store` WHERE `type` = 'gtasks-list'");
          database.execSQL("DROP TABLE IF EXISTS `store`");
        }
      };

  private static final Migration MIGRATION_52_53 =
      new Migration(52, 53) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL("ALTER TABLE `tagdata` RENAME TO `tagdata-temp`");
          database.execSQL(
              "CREATE TABLE `tagdata` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `name` TEXT, `color` INTEGER, `tagOrdering` TEXT)");
          database.execSQL(
              "INSERT INTO `tagdata` (`remoteId`, `name`, `color`, `tagOrdering`) "
                  + "SELECT `remoteId`, `name`, `color`, `tagOrdering` FROM `tagdata-temp`");
          database.execSQL("DROP TABLE `tagdata-temp`");

          database.execSQL("ALTER TABLE `userActivity` RENAME TO `userActivity-temp`");
          database.execSQL(
              "CREATE TABLE `userActivity` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `message` TEXT, `picture` TEXT, `target_id` TEXT, `created_at` INTEGER)");
          database.execSQL(
              "INSERT INTO `userActivity` (`remoteId`, `message`, `picture`, `target_id`, `created_at`) "
                  + "SELECT `remoteId`, `message`, `picture`, `target_id`, `created_at` FROM `userActivity-temp`");
          database.execSQL("DROP TABLE `userActivity-temp`");

          database.execSQL("ALTER TABLE `task_attachments` RENAME TO `task_attachments-temp`");
          database.execSQL(
              "CREATE TABLE `task_attachments` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `task_id` TEXT, `name` TEXT, `path` TEXT, `content_type` TEXT)");
          database.execSQL(
              "INSERT INTO `task_attachments` (`remoteId`, `task_id`, `name`, `path`, `content_type`) "
                  + "SELECT `remoteId`, `task_id`, `name`, `path`, `content_type` FROM `task_attachments-temp`");
          database.execSQL("DROP TABLE `task_attachments-temp`");
        }
      };

  private static final Migration MIGRATION_53_54 =
      new Migration(53, 54) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          // need to drop columns that were removed in the past
          database.execSQL("ALTER TABLE `task_list_metadata` RENAME TO `task_list_metadata-temp`");
          database.execSQL(
              "CREATE TABLE `task_list_metadata` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `tag_uuid` TEXT, `filter` TEXT, `task_ids` TEXT)");
          database.execSQL(
              "INSERT INTO `task_list_metadata` (`remoteId`, `tag_uuid`, `filter`, `task_ids`) "
                  + "SELECT `remoteId`, `tag_uuid`, `filter`, `task_ids` FROM `task_list_metadata-temp`");
          database.execSQL("DROP TABLE `task_list_metadata-temp`");

          database.execSQL("ALTER TABLE `tasks` RENAME TO `tasks-temp`");
          database.execSQL(
              "CREATE TABLE `tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `title` TEXT, `importance` INTEGER, `dueDate` INTEGER, `hideUntil` INTEGER, `created` INTEGER, `modified` INTEGER, `completed` INTEGER, `deleted` INTEGER, `notes` TEXT, `estimatedSeconds` INTEGER, `elapsedSeconds` INTEGER, `timerStart` INTEGER, `notificationFlags` INTEGER, `notifications` INTEGER, `lastNotified` INTEGER, `snoozeTime` INTEGER, `recurrence` TEXT, `repeatUntil` INTEGER, `calendarUri` TEXT, `remoteId` TEXT)");
          database.execSQL("DROP INDEX `t_rid`");
          database.execSQL("CREATE UNIQUE INDEX `t_rid` ON `tasks` (`remoteId`)");
          database.execSQL(
              "INSERT INTO `tasks` (`_id`, `title`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `notes`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `notifications`, `lastNotified`, `snoozeTime`, `recurrence`, `repeatUntil`, `calendarUri`, `remoteId`) "
                  + "SELECT `_id`, `title`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `notes`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `notifications`, `lastNotified`, `snoozeTime`, `recurrence`, `repeatUntil`, `calendarUri`, `remoteId` FROM `tasks-temp`");
          database.execSQL("DROP TABLE `tasks-temp`");
        }
      };

  private static final Migration MIGRATION_54_58 =
      new Migration(54, 58) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `caldav_account` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT, `name` TEXT, `url` TEXT, `username` TEXT, `password` TEXT)");
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `caldav_calendar` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account` TEXT, `uuid` TEXT, `name` TEXT, `color` INTEGER NOT NULL, `ctag` TEXT, `url` TEXT)");
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `caldav_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `calendar` TEXT, `object` TEXT, `remote_id` TEXT, `etag` TEXT, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `vtodo` TEXT)");
        }
      };

  private static final Migration MIGRATION_58_59 =
      new Migration(58, 59) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `google_task_accounts` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account` TEXT, `error` TEXT)");
          database.execSQL("ALTER TABLE `google_task_lists` ADD COLUMN `account` TEXT");
          database.execSQL("ALTER TABLE `caldav_account` ADD COLUMN `error` TEXT");
        }
      };

  public static final Migration[] MIGRATIONS =
      new Migration[] {
        MIGRATION_35_36,
        MIGRATION_36_37,
        MIGRATION_37_38,
        MIGRATION_38_39,
        NOOP(39, 46),
        MIGRATION_46_47,
        MIGRATION_47_48,
        MIGRATION_48_49,
        MIGRATION_49_50,
        MIGRATION_50_51,
        MIGRATION_51_52,
        MIGRATION_52_53,
        MIGRATION_53_54,
        MIGRATION_54_58,
        MIGRATION_58_59
      };

  private static Migration NOOP(int from, int to) {
    return new Migration(from, to) {
      @Override
      public void migrate(@NonNull SupportSQLiteDatabase database) {}
    };
  }
}
