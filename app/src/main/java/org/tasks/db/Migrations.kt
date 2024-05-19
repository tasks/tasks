package org.tasks.db

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import org.tasks.R
import org.tasks.caldav.FileStorage
import org.tasks.data.NO_ORDER
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.NOTIFY_AFTER_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_START
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit.HOURS

object Migrations {

    private val MIGRATION_35_36: Migration = object : Migration(35, 36) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tagdata` ADD COLUMN `color` INTEGER DEFAULT -1")
        }
    }

    private val MIGRATION_36_37: Migration = object : Migration(36, 37) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `store` ADD COLUMN `deleted` INTEGER DEFAULT 0")
        }
    }

    private val MIGRATION_37_38: Migration = object : Migration(37, 38) {
        override fun migrate(connection: SQLiteConnection) {
            try {
                connection.execSQL("ALTER TABLE `store` ADD COLUMN `value4` TEXT DEFAULT -1")
            } catch (e: SQLiteException) {
                Timber.w(e)
            }
        }
    }

    private val MIGRATION_38_39: Migration = object : Migration(38, 39) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notification` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `type` INTEGER NOT NULL)")
            connection.execSQL(
                    "CREATE UNIQUE INDEX `index_notification_task` ON `notification` (`task`)")
        }
    }

    private val MIGRATION_46_47: Migration = object : Migration(46, 47) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alarms` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `time` INTEGER NOT NULL)")
            connection.execSQL(
                    "INSERT INTO `alarms` (`task`, `time`) SELECT `task`, `value` FROM `metadata` WHERE `key` = 'alarm' AND `deleted` = 0")
            connection.execSQL("DELETE FROM `metadata` WHERE `key` = 'alarm'")
        }
    }

    private val MIGRATION_47_48: Migration = object : Migration(47, 48) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `locations` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `radius` INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO `locations` (`task`, `name`, `latitude`, `longitude`, `radius`) "
                    + "SELECT `task`, `value`, `value2`, `value3`, `value4` FROM `metadata` WHERE `key` = 'geofence' AND `deleted` = 0")
            connection.execSQL("DELETE FROM `metadata` WHERE `key` = 'geofence'")
        }
    }

    private val MIGRATION_48_49: Migration = object : Migration(48, 49) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tags` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `tag_uid` TEXT, `task_uid` TEXT)")
            connection.execSQL("INSERT INTO `tags` (`task`, `name`, `tag_uid`, `task_uid`) "
                    + "SELECT `task`, `value`, `value2`, `value3` FROM `metadata` WHERE `key` = 'tags-tag' AND `deleted` = 0")
            connection.execSQL("DELETE FROM `metadata` WHERE `key` = 'tags-tag'")
        }
    }

    private val MIGRATION_49_50: Migration = object : Migration(49, 50) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `google_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `remote_id` TEXT, `list_id` TEXT, `parent` INTEGER NOT NULL, `indent` INTEGER NOT NULL, `order` INTEGER NOT NULL, `remote_order` INTEGER NOT NULL, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO `google_tasks` (`task`, `remote_id`, `list_id`, `parent`, `indent`, `order`, `remote_order`, `last_sync`, `deleted`) "
                    + "SELECT `task`, `value`, `value2`, IFNULL(`value3`, 0), IFNULL(`value4`, 0), IFNULL(`value5`, 0), IFNULL(`value6`, 0), IFNULL(`value7`, 0), IFNULL(`deleted`, 0) FROM `metadata` WHERE `key` = 'gtasks'")
            connection.execSQL("DROP TABLE IF EXISTS `metadata`")
        }
    }

    private val MIGRATION_50_51: Migration = object : Migration(50, 51) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `filters` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `sql` TEXT, `values` TEXT, `criterion` TEXT)")
            connection.execSQL("INSERT INTO `filters` (`title`, `sql`, `values`, `criterion`) "
                    + "SELECT `item`, `value`, `value2`, `value3` FROM `store` WHERE `type` = 'filter' AND `deleted` = 0")
            connection.execSQL("DELETE FROM `store` WHERE `type` = 'filter'")
        }
    }
    private val MIGRATION_51_52: Migration = object : Migration(51, 52) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `google_task_lists` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remote_id` TEXT, `title` TEXT, `remote_order` INTEGER NOT NULL, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `color` INTEGER)")
            connection.execSQL("INSERT INTO `google_task_lists` (`remote_id`, `title`, `remote_order`, `last_sync`, `color`, `deleted`) "
                    + "SELECT `item`, `value`, `value2`, `value3`, `value4`, `deleted` FROM `store` WHERE `type` = 'gtasks-list'")
            connection.execSQL("DROP TABLE IF EXISTS `store`")
        }
    }

    private val MIGRATION_52_53: Migration = object : Migration(52, 53) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tagdata` RENAME TO `tagdata-temp`")
            connection.execSQL(
                    "CREATE TABLE `tagdata` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `name` TEXT, `color` INTEGER, `tagOrdering` TEXT)")
            connection.execSQL("INSERT INTO `tagdata` (`remoteId`, `name`, `color`, `tagOrdering`) "
                    + "SELECT `remoteId`, `name`, `color`, `tagOrdering` FROM `tagdata-temp`")
            connection.execSQL("DROP TABLE `tagdata-temp`")
            connection.execSQL("ALTER TABLE `userActivity` RENAME TO `userActivity-temp`")
            connection.execSQL(
                    "CREATE TABLE `userActivity` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `message` TEXT, `picture` TEXT, `target_id` TEXT, `created_at` INTEGER)")
            connection.execSQL("INSERT INTO `userActivity` (`remoteId`, `message`, `picture`, `target_id`, `created_at`) "
                    + "SELECT `remoteId`, `message`, `picture`, `target_id`, `created_at` FROM `userActivity-temp`")
            connection.execSQL("DROP TABLE `userActivity-temp`")
            connection.execSQL("ALTER TABLE `task_attachments` RENAME TO `task_attachments-temp`")
            connection.execSQL(
                    "CREATE TABLE `task_attachments` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `task_id` TEXT, `name` TEXT, `path` TEXT, `content_type` TEXT)")
            connection.execSQL("INSERT INTO `task_attachments` (`remoteId`, `task_id`, `name`, `path`, `content_type`) "
                    + "SELECT `remoteId`, `task_id`, `name`, `path`, `content_type` FROM `task_attachments-temp`")
            connection.execSQL("DROP TABLE `task_attachments-temp`")
        }
    }

    private val MIGRATION_53_54: Migration = object : Migration(53, 54) {
        override fun migrate(connection: SQLiteConnection) {
            // need to drop columns that were removed in the past
            connection.execSQL("ALTER TABLE `task_list_metadata` RENAME TO `task_list_metadata-temp`")
            connection.execSQL(
                    "CREATE TABLE `task_list_metadata` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `remoteId` TEXT, `tag_uuid` TEXT, `filter` TEXT, `task_ids` TEXT)")
            connection.execSQL("INSERT INTO `task_list_metadata` (`remoteId`, `tag_uuid`, `filter`, `task_ids`) "
                    + "SELECT `remoteId`, `tag_uuid`, `filter`, `task_ids` FROM `task_list_metadata-temp`")
            connection.execSQL("DROP TABLE `task_list_metadata-temp`")
            connection.execSQL("ALTER TABLE `tasks` RENAME TO `tasks-temp`")
            connection.execSQL(
                    "CREATE TABLE `tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `title` TEXT, `importance` INTEGER, `dueDate` INTEGER, `hideUntil` INTEGER, `created` INTEGER, `modified` INTEGER, `completed` INTEGER, `deleted` INTEGER, `notes` TEXT, `estimatedSeconds` INTEGER, `elapsedSeconds` INTEGER, `timerStart` INTEGER, `notificationFlags` INTEGER, `notifications` INTEGER, `lastNotified` INTEGER, `snoozeTime` INTEGER, `recurrence` TEXT, `repeatUntil` INTEGER, `calendarUri` TEXT, `remoteId` TEXT)")
            connection.execSQL("DROP INDEX `t_rid`")
            connection.execSQL("CREATE UNIQUE INDEX `t_rid` ON `tasks` (`remoteId`)")
            connection.execSQL("INSERT INTO `tasks` (`_id`, `title`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `notes`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `notifications`, `lastNotified`, `snoozeTime`, `recurrence`, `repeatUntil`, `calendarUri`, `remoteId`) "
                    + "SELECT `_id`, `title`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `notes`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `notifications`, `lastNotified`, `snoozeTime`, `recurrence`, `repeatUntil`, `calendarUri`, `remoteId` FROM `tasks-temp`")
            connection.execSQL("DROP TABLE `tasks-temp`")
        }
    }

    private val MIGRATION_54_58: Migration = object : Migration(54, 58) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_account` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT, `name` TEXT, `url` TEXT, `username` TEXT, `password` TEXT)")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_calendar` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account` TEXT, `uuid` TEXT, `name` TEXT, `color` INTEGER NOT NULL, `ctag` TEXT, `url` TEXT)")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `calendar` TEXT, `object` TEXT, `remote_id` TEXT, `etag` TEXT, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `vtodo` TEXT)")
        }
    }

    private val MIGRATION_58_59: Migration = object : Migration(58, 59) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `google_task_accounts` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account` TEXT, `error` TEXT)")
            connection.execSQL("ALTER TABLE `google_task_lists` ADD COLUMN `account` TEXT")
            connection.execSQL("ALTER TABLE `caldav_account` ADD COLUMN `error` TEXT")
        }
    }

    private val MIGRATION_59_60: Migration = object : Migration(59, 60) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `locations` ADD COLUMN `address` TEXT")
            connection.execSQL("ALTER TABLE `locations` ADD COLUMN `phone` TEXT")
            connection.execSQL("ALTER TABLE `locations` ADD COLUMN `url` TEXT")
            connection.execSQL(
                    "ALTER TABLE `locations` ADD COLUMN `arrival` INTEGER DEFAULT 1 NOT NULL")
            connection.execSQL(
                    "ALTER TABLE `locations` ADD COLUMN `departure` INTEGER DEFAULT 0 NOT NULL")
            connection.execSQL("ALTER TABLE `notification` ADD COLUMN `location` INTEGER")
        }
    }

    private val MIGRATION_60_61: Migration = object : Migration(60, 61) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `places` (`place_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uid` TEXT, `name` TEXT, `address` TEXT, `phone` TEXT, `url` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL)")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `geofences` (`geofence_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `place` TEXT, `radius` INTEGER NOT NULL, `arrival` INTEGER NOT NULL, `departure` INTEGER NOT NULL)")
            connection.execSQL(
                    "INSERT INTO `places` (`place_id`, `uid`, `name`, `address`, `phone`, `url`, `latitude`, `longitude`) SELECT `_id`, hex(randomblob(16)), `name`, `address`, `phone`, `url`, `latitude`, `longitude` FROM `locations`")
            connection.execSQL(
                    "INSERT INTO `geofences` (`geofence_id`, `task`, `place`, `radius`, `arrival`, `departure`) SELECT `_id`, `task`, `uid`, `radius`, `arrival`, `departure` FROM `locations` INNER JOIN `places` ON `_id` = `place_id`")
            connection.execSQL("DROP TABLE `locations`")
        }
    }

    private val MIGRATION_61_62: Migration = object : Migration(61, 62) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `google_task_accounts` ADD COLUMN `etag` TEXT")
        }
    }

    private val MIGRATION_62_63: Migration = object : Migration(62, 63) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `google_tasks` RENAME TO `gt-temp`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `google_tasks` (`gt_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gt_task` INTEGER NOT NULL, `gt_remote_id` TEXT, `gt_list_id` TEXT, `gt_parent` INTEGER NOT NULL, `gt_remote_parent` TEXT, `gt_moved` INTEGER NOT NULL, `gt_order` INTEGER NOT NULL, `gt_remote_order` INTEGER NOT NULL, `gt_last_sync` INTEGER NOT NULL, `gt_deleted` INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO `google_tasks` (`gt_id`, `gt_task`, `gt_remote_id`, `gt_list_id`, `gt_parent`, `gt_remote_parent`, `gt_moved`, `gt_order`, `gt_remote_order`, `gt_last_sync`, `gt_deleted`) "
                    + "SELECT `_id`, `task`, `remote_id`, `list_id`, `parent`, '', 0, `order`, `remote_order`, `last_sync`, `deleted` FROM `gt-temp`")
            connection.execSQL("DROP TABLE `gt-temp`")
            connection.execSQL("UPDATE `google_task_lists` SET `last_sync` = 0")
        }
    }

    private val MIGRATION_63_64: Migration = object : Migration(63, 64) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `caldav_tasks` RENAME TO `caldav-temp`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_tasks` (`cd_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cd_task` INTEGER NOT NULL, `cd_calendar` TEXT, `cd_object` TEXT, `cd_remote_id` TEXT, `cd_etag` TEXT, `cd_last_sync` INTEGER NOT NULL, `cd_deleted` INTEGER NOT NULL, `cd_vtodo` TEXT)")
            connection.execSQL("INSERT INTO `caldav_tasks` (`cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_vtodo`)"
                    + "SELECT `_id`, `task`, `calendar`, `object`, `remote_id`, `etag`, `last_sync`, `deleted`, `vtodo` FROM `caldav-temp`")
            connection.execSQL("DROP TABLE `caldav-temp`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_accounts` (`cda_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cda_uuid` TEXT, `cda_name` TEXT, `cda_url` TEXT, `cda_username` TEXT, `cda_password` TEXT, `cda_error` TEXT)")
            connection.execSQL("INSERT INTO `caldav_accounts` (`cda_id`, `cda_uuid`, `cda_name`, `cda_url`, `cda_username`, `cda_password`, `cda_error`) "
                    + "SELECT `_id`, `uuid`, `name`, `url`, `username`, `password`, `error` FROM `caldav_account`")
            connection.execSQL("DROP TABLE `caldav_account`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_lists` (`cdl_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cdl_account` TEXT, `cdl_uuid` TEXT, `cdl_name` TEXT, `cdl_color` INTEGER NOT NULL, `cdl_ctag` TEXT, `cdl_url` TEXT, `cdl_icon` INTEGER)")
            connection.execSQL("INSERT INTO `caldav_lists` (`cdl_id`, `cdl_account`, `cdl_uuid`, `cdl_name`, `cdl_color`, `cdl_ctag`, `cdl_url`) "
                    + "SELECT `_id`, `account`, `uuid`, `name`, `color`, `ctag`, `url` FROM caldav_calendar")
            connection.execSQL("DROP TABLE `caldav_calendar`")
            connection.execSQL("ALTER TABLE `google_task_accounts` RENAME TO `gta-temp`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `google_task_accounts` (`gta_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gta_account` TEXT, `gta_error` TEXT, `gta_etag` TEXT)")
            connection.execSQL("INSERT INTO `google_task_accounts` (`gta_id`, `gta_account`, `gta_error`, `gta_etag`) "
                    + "SELECT `_id`, `account`, `error`, `etag` FROM `gta-temp`")
            connection.execSQL("DROP TABLE `gta-temp`")
            connection.execSQL("ALTER TABLE `google_task_lists` RENAME TO `gtl-temp`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `google_task_lists` (`gtl_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gtl_account` TEXT, `gtl_remote_id` TEXT, `gtl_title` TEXT, `gtl_remote_order` INTEGER NOT NULL, `gtl_last_sync` INTEGER NOT NULL, `gtl_color` INTEGER, `gtl_icon` INTEGER)")
            connection.execSQL("INSERT INTO `google_task_lists` (`gtl_id`, `gtl_account`, `gtl_remote_id`, `gtl_title`, `gtl_remote_order`, `gtl_last_sync`, `gtl_color`) "
                    + "SELECT `_id`, `account`, `remote_id`, `title`, `remote_order`, `last_sync`, `color` FROM `gtl-temp`")
            connection.execSQL("DROP TABLE `gtl-temp`")
            connection.execSQL("ALTER TABLE `filters` ADD COLUMN `f_color` INTEGER")
            connection.execSQL("ALTER TABLE `filters` ADD COLUMN `f_icon` INTEGER")
            connection.execSQL("ALTER TABLE `tagdata` ADD COLUMN `td_icon` INTEGER")
        }
    }

    private val MIGRATION_64_65: Migration = object : Migration(64, 65) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "ALTER TABLE `caldav_tasks` ADD COLUMN `cd_parent` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `caldav_tasks` ADD COLUMN `cd_remote_parent` TEXT")
        }
    }

    private val MIGRATION_65_66: Migration = object : Migration(65, 66) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE UNIQUE INDEX `place_uid` ON `places` (`uid`)")
            connection.execSQL("CREATE INDEX `geo_task` ON `geofences` (`task`)")
            connection.execSQL("CREATE INDEX `tag_task` ON `tags` (`task`)")
            connection.execSQL("CREATE INDEX `gt_list_parent` ON `google_tasks` (`gt_list_id`, `gt_parent`)")
            connection.execSQL("CREATE INDEX `gt_task` ON `google_tasks` (`gt_task`)")
            connection.execSQL("CREATE INDEX `cd_calendar_parent` ON `caldav_tasks` (`cd_calendar`, `cd_parent`)")
            connection.execSQL("CREATE INDEX `cd_task` ON `caldav_tasks` (`cd_task`)")
        }
    }

    private val MIGRATION_66_67: Migration = object : Migration(66, 67) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "ALTER TABLE `caldav_accounts` ADD COLUMN `cda_repeat` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_67_68: Migration = object : Migration(67, 68) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE INDEX `active_and_visible` ON `tasks` (`completed`, `deleted`, `hideUntil`)")
        }
    }

    private val MIGRATION_68_69: Migration = object : Migration(68, 69) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "ALTER TABLE `tasks` ADD COLUMN `collapsed` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_69_70: Migration = object : Migration(69, 70) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tasks` ADD COLUMN `parent` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `tasks` ADD COLUMN `parent_uuid` TEXT")
            connection.execSQL(
                    "UPDATE `tasks` SET `parent` = IFNULL(("
                            + " SELECT p.cd_task FROM caldav_tasks"
                            + "  INNER JOIN caldav_tasks AS p ON p.cd_remote_id = caldav_tasks.cd_remote_parent"
                            + "  WHERE caldav_tasks.cd_task = tasks._id"
                            + "    AND caldav_tasks.cd_deleted = 0"
                            + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
                            + "    AND p.cd_deleted = 0), 0)")
            connection.execSQL("ALTER TABLE `caldav_tasks` RENAME TO `caldav_tasks-temp`")
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caldav_tasks` (`cd_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cd_task` INTEGER NOT NULL, `cd_calendar` TEXT, `cd_object` TEXT, `cd_remote_id` TEXT, `cd_etag` TEXT, `cd_last_sync` INTEGER NOT NULL, `cd_deleted` INTEGER NOT NULL, `cd_vtodo` TEXT, `cd_remote_parent` TEXT)")
            connection.execSQL("INSERT INTO `caldav_tasks` (`cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_vtodo`, `cd_remote_parent`) "
                    + "SELECT `cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_vtodo`, `cd_remote_parent` FROM `caldav_tasks-temp`")
            connection.execSQL("DROP TABLE `caldav_tasks-temp`")
            connection.execSQL("CREATE INDEX `cd_task` ON `caldav_tasks` (`cd_task`)")
        }
    }

    private val MIGRATION_70_71: Migration = object : Migration(70, 71) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `caldav_accounts` ADD COLUMN `cda_encryption_key` TEXT")
            connection.execSQL(
                    "ALTER TABLE `caldav_accounts` ADD COLUMN `cda_account_type` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_71_72: Migration = object : Migration(71, 72) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "ALTER TABLE `caldav_accounts` ADD COLUMN `cda_collapsed` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL(
                    "ALTER TABLE `google_task_accounts` ADD COLUMN `gta_collapsed` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_72_73: Migration = object : Migration(72, 73) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `places` ADD COLUMN `place_color` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `places` ADD COLUMN `place_icon` INTEGER NOT NULL DEFAULT -1")
        }
    }

    private val MIGRATION_73_74: Migration = object : Migration(73, 74) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tasks` RENAME TO `tasks-temp`")
            connection.execSQL("DROP INDEX `t_rid`")
            connection.execSQL("DROP INDEX `active_and_visible`")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `importance` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `hideUntil` INTEGER NOT NULL, `created` INTEGER NOT NULL, `modified` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `notes` TEXT, `estimatedSeconds` INTEGER NOT NULL, `elapsedSeconds` INTEGER NOT NULL, `timerStart` INTEGER NOT NULL, `notificationFlags` INTEGER NOT NULL, `notifications` INTEGER NOT NULL, `lastNotified` INTEGER NOT NULL, `snoozeTime` INTEGER NOT NULL, `recurrence` TEXT, `repeatUntil` INTEGER NOT NULL, `calendarUri` TEXT, `remoteId` TEXT, `collapsed` INTEGER NOT NULL, `parent` INTEGER NOT NULL, `parent_uuid` TEXT)")
            connection.execSQL("INSERT INTO `tasks` (`_id`, `title`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `notes`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `notifications`, `lastNotified`, `snoozeTime`, `recurrence`, `repeatUntil`, `calendarUri`, `remoteId`, `collapsed`, `parent`, `parent_uuid`) "
                    + "SELECT `_id`, `title`, IFNULL(`importance`, 3), IFNULL(`dueDate`, 0), IFNULL(`hideUntil`, 0), IFNULL(`created`, 0), IFNULL(`modified`, 0), IFNULL(`completed`, 0), IFNULL(`deleted`, 0), `notes`, IFNULL(`estimatedSeconds`, 0), IFNULL(`elapsedSeconds`, 0), IFNULL(`timerStart`, 0), IFNULL(`notificationFlags`, 0), IFNULL(`notifications`, 0), IFNULL(`lastNotified`, 0), IFNULL(`snoozeTime`, 0), `recurrence`, IFNULL(`repeatUntil`, 0), `calendarUri`, `remoteId`, `collapsed`, IFNULL(`parent`, 0), `parent_uuid` FROM `tasks-temp`")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `t_rid` ON `tasks` (`remoteId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `active_and_visible` ON `tasks` (`completed`, `deleted`, `hideUntil`)")
            connection.execSQL("DROP TABLE `tasks-temp`")
        }
    }

    private val MIGRATION_74_75: Migration = object : Migration(74, 75) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `caldav_tasks` ADD COLUMN `cd_order` INTEGER")
        }
    }

    private val MIGRATION_75_76: Migration = object : Migration(75, 76) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tagdata` ADD COLUMN `td_order` INTEGER NOT NULL DEFAULT $NO_ORDER")
            connection.execSQL("ALTER TABLE `caldav_lists` ADD COLUMN `cdl_order` INTEGER NOT NULL DEFAULT $NO_ORDER")
            connection.execSQL("ALTER TABLE `filters` ADD COLUMN `f_order` INTEGER NOT NULL DEFAULT $NO_ORDER")
            connection.execSQL("ALTER TABLE `places` ADD COLUMN `place_order` INTEGER NOT NULL DEFAULT $NO_ORDER")
        }
    }

    private val MIGRATION_76_77: Migration = object : Migration(76, 77) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "ALTER TABLE `caldav_lists` ADD COLUMN `cdl_access` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_77_78: Migration = object : Migration(77, 78) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `principals` (`principal_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `principal_list` INTEGER NOT NULL, `principal` TEXT, `display_name` TEXT, `invite` INTEGER NOT NULL, `access` INTEGER NOT NULL, FOREIGN KEY(`principal_list`) REFERENCES `caldav_lists`(`cdl_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_principals_principal_list_principal` ON `principals` (`principal_list`, `principal`)"
            )
        }
    }

    private val MIGRATION_78_79: Migration = object : Migration(78, 79) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `caldav_accounts` ADD COLUMN `cda_server_type` INTEGER NOT NULL DEFAULT $SERVER_UNKNOWN"
            )
        }
    }

    private val MIGRATION_79_80 = object : Migration(79, 80) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP TABLE `principals`")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `principals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account` INTEGER NOT NULL, `href` TEXT NOT NULL, `email` TEXT, `display_name` TEXT, FOREIGN KEY(`account`) REFERENCES `caldav_accounts`(`cda_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_principals_account_href` ON `principals` (`account`, `href`)"
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `principal_access` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `principal` INTEGER NOT NULL, `list` INTEGER NOT NULL, `invite` INTEGER NOT NULL, `access` INTEGER NOT NULL, FOREIGN KEY(`principal`) REFERENCES `principals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`list`) REFERENCES `caldav_lists`(`cdl_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_principal_access_list_principal` ON `principal_access` (`list`, `principal`)"
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_principal_access_principal` ON `principal_access` (`principal`)"
            )
        }
    }

    private val MIGRATION_80_81 = object : Migration(80, 81) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `alarms` ADD COLUMN `type` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `alarms` ADD COLUMN `repeat` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `alarms` ADD COLUMN `interval` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL(
                "INSERT INTO `alarms` (`task`, `time`, `type`) SELECT `_id`, 0, $TYPE_REL_START FROM `tasks` WHERE `hideUntil` > 0 AND `notificationFlags` & $NOTIFY_AT_START = $NOTIFY_AT_START"
            )
            connection.execSQL(
                "INSERT INTO `alarms` (`task`, `time`, `type`) SELECT `_id`, 0, $TYPE_REL_END FROM `tasks` WHERE `dueDate` > 0 AND `notificationFlags` & $NOTIFY_AT_DEADLINE = $NOTIFY_AT_DEADLINE"
            )
            connection.execSQL(
                "INSERT INTO `alarms` (`task`, `time`, `type`, `repeat`, `interval`) SELECT `_id`, ${HOURS.toMillis(24)}, $TYPE_REL_END, 6, ${HOURS.toMillis(24)} FROM `tasks` WHERE `dueDate` > 0 AND `notificationFlags` & $NOTIFY_AFTER_DEADLINE = $NOTIFY_AFTER_DEADLINE"
            )
            connection.execSQL(
                "INSERT INTO `alarms` (`task`, `time`, `type`) SELECT `_id`, `notifications`, $TYPE_RANDOM FROM `tasks` WHERE `notifications` > 0"
            )
            connection.execSQL(
                "INSERT INTO `alarms` (`task`, `time`, `type`) SELECT `_id`, `snoozeTime`, $TYPE_SNOOZE FROM `tasks` WHERE `snoozeTime` > 0"
            )
        }
    }

    @Suppress("FunctionName")
    private fun migration_81_82(fileStorage: FileStorage) = object : Migration(81, 82) {
        override fun migrate(connection: SQLiteConnection) {
            connection
                .prepare("SELECT `cdl_account`, `cd_calendar`, `cd_object`, `cd_vtodo` FROM `caldav_tasks` INNER JOIN `caldav_lists` ON `cdl_uuid` = `cd_calendar`")
                .use {
                    while (it.step()) {
                        val file = fileStorage.getFile(
                            it.getText(0),
                            it.getText(1),
                        )
                            ?.apply { mkdirs() }
                            ?: continue
                        if (it.isNull(2)) continue
                        val `object` = it.getText(2)
                        fileStorage.write(File(file, `object`), it.getText(3))
                    }
                }
            connection.execSQL("ALTER TABLE `caldav_tasks` RENAME TO `caldav_tasks-temp`")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `caldav_tasks` (`cd_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cd_task` INTEGER NOT NULL, `cd_calendar` TEXT, `cd_object` TEXT, `cd_remote_id` TEXT, `cd_etag` TEXT, `cd_last_sync` INTEGER NOT NULL, `cd_deleted` INTEGER NOT NULL, `cd_remote_parent` TEXT, `cd_order` INTEGER)"
            )
            connection.execSQL("DROP INDEX `cd_task`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `cd_task` ON `caldav_tasks` (`cd_task`)")
            connection.execSQL(
                "INSERT INTO `caldav_tasks` (`cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_remote_parent`, `cd_order`) SELECT `cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_remote_parent`, `cd_order` FROM `caldav_tasks-temp`"
            )
            connection.execSQL("DROP TABLE `caldav_tasks-temp`")
        }
    }

    private val MIGRATION_82_83 = object : Migration(82, 83) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `places` ADD COLUMN `radius` INTEGER NOT NULL DEFAULT 250")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_alarms` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `time` INTEGER NOT NULL, `type` INTEGER NOT NULL DEFAULT 0, `repeat` INTEGER NOT NULL DEFAULT 0, `interval` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL("INSERT INTO `_new_alarms` (`task`,`repeat`,`interval`,`_id`,`time`,`type`) SELECT `task`,`repeat`,`interval`,`alarms`.`_id`,`time`,`type` FROM `alarms` INNER JOIN `tasks` ON `tasks`.`_id` = `task`")
            connection.execSQL("DROP TABLE `alarms`")
            connection.execSQL("ALTER TABLE `_new_alarms` RENAME TO `alarms`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_alarms_task` ON `alarms` (`task`)")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_google_tasks` (`gt_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gt_task` INTEGER NOT NULL, `gt_remote_id` TEXT, `gt_list_id` TEXT, `gt_parent` INTEGER NOT NULL, `gt_remote_parent` TEXT, `gt_moved` INTEGER NOT NULL, `gt_order` INTEGER NOT NULL, `gt_remote_order` INTEGER NOT NULL, `gt_last_sync` INTEGER NOT NULL, `gt_deleted` INTEGER NOT NULL, FOREIGN KEY(`gt_task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL("INSERT INTO `_new_google_tasks` (`gt_parent`,`gt_task`,`gt_remote_parent`,`gt_order`,`gt_last_sync`,`gt_id`,`gt_remote_id`,`gt_list_id`,`gt_moved`,`gt_remote_order`,`gt_deleted`) SELECT `gt_parent`,`gt_task`,`gt_remote_parent`,`gt_order`,`gt_last_sync`,`gt_id`,`gt_remote_id`,`gt_list_id`,`gt_moved`,`gt_remote_order`,`gt_deleted` FROM `google_tasks` INNER JOIN `tasks` ON `tasks`.`_id` = `gt_task`")
            connection.execSQL("DROP TABLE `google_tasks`")
            connection.execSQL("ALTER TABLE `_new_google_tasks` RENAME TO `google_tasks`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `gt_list_parent` ON `google_tasks` (`gt_list_id`, `gt_parent`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_google_tasks_gt_task` ON `google_tasks` (`gt_task`)")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_tags` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `tag_uid` TEXT, `task_uid` TEXT, FOREIGN KEY(`task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL("INSERT INTO `_new_tags` (`task`,`task_uid`,`name`,`tag_uid`,`_id`) SELECT `task`,`task_uid`,`name`,`tag_uid`,`tags`.`_id` FROM `tags` INNER JOIN `tasks` ON `tasks`.`_id` = `task`")
            connection.execSQL("DROP TABLE `tags`")
            connection.execSQL("ALTER TABLE `_new_tags` RENAME TO `tags`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_task` ON `tags` (`task`)")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_notification` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `type` INTEGER NOT NULL, `location` INTEGER, FOREIGN KEY(`task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL("INSERT INTO `_new_notification` (`uid`,`task`,`location`,`type`,`timestamp`) SELECT `uid`,`task`,`location`,`type`,`timestamp` FROM `notification` INNER JOIN `tasks` ON `tasks`.`_id` = `task`")
            connection.execSQL("DROP TABLE `notification`")
            connection.execSQL("ALTER TABLE `_new_notification` RENAME TO `notification`")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notification_task` ON `notification` (`task`)")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_caldav_tasks` (`cd_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cd_task` INTEGER NOT NULL, `cd_calendar` TEXT, `cd_object` TEXT, `cd_remote_id` TEXT, `cd_etag` TEXT, `cd_last_sync` INTEGER NOT NULL, `cd_deleted` INTEGER NOT NULL, `cd_remote_parent` TEXT, `cd_order` INTEGER, FOREIGN KEY(`cd_task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL("INSERT INTO `_new_caldav_tasks` (`cd_object`,`cd_deleted`,`cd_order`,`cd_remote_parent`,`cd_etag`,`cd_id`,`cd_calendar`,`cd_remote_id`,`cd_last_sync`,`cd_task`) SELECT `cd_object`,`cd_deleted`,`cd_order`,`cd_remote_parent`,`cd_etag`,`cd_id`,`cd_calendar`,`cd_remote_id`,`cd_last_sync`,`cd_task` FROM `caldav_tasks` INNER JOIN `tasks` ON `tasks`.`_id` = `cd_task`")
            connection.execSQL("DROP TABLE `caldav_tasks`")
            connection.execSQL("ALTER TABLE `_new_caldav_tasks` RENAME TO `caldav_tasks`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_caldav_tasks_cd_task` ON `caldav_tasks` (`cd_task`)")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_geofences` (`geofence_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `place` TEXT, `arrival` INTEGER NOT NULL, `departure` INTEGER NOT NULL, FOREIGN KEY(`task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            connection.execSQL("INSERT INTO `_new_geofences` (`task`,`geofence_id`,`arrival`,`place`,`departure`) SELECT `task`,`geofence_id`,`arrival`,`place`,`departure` FROM `geofences` INNER JOIN `tasks` ON `tasks`.`_id` = `task`")
            connection.execSQL("DROP TABLE `geofences`")
            connection.execSQL("ALTER TABLE `_new_geofences` RENAME TO `geofences`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_geofences_task` ON `geofences` (`task`)")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_task_list_metadata` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `tag_uuid` TEXT, `filter` TEXT, `task_ids` TEXT)")
            connection.execSQL("INSERT INTO `_new_task_list_metadata` (`filter`,`tag_uuid`,`_id`,`task_ids`) SELECT `filter`,`tag_uuid`,`_id`,`task_ids` FROM `task_list_metadata`")
            connection.execSQL("DROP TABLE `task_list_metadata`")
            connection.execSQL("ALTER TABLE `_new_task_list_metadata` RENAME TO `task_list_metadata`")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `importance` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `hideUntil` INTEGER NOT NULL, `created` INTEGER NOT NULL, `modified` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `notes` TEXT, `estimatedSeconds` INTEGER NOT NULL, `elapsedSeconds` INTEGER NOT NULL, `timerStart` INTEGER NOT NULL, `notificationFlags` INTEGER NOT NULL, `lastNotified` INTEGER NOT NULL, `recurrence` TEXT, `repeatUntil` INTEGER NOT NULL, `calendarUri` TEXT, `remoteId` TEXT, `collapsed` INTEGER NOT NULL, `parent` INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO `_new_tasks` (`parent`,`notes`,`timerStart`,`estimatedSeconds`,`importance`,`created`,`collapsed`,`dueDate`,`completed`,`repeatUntil`,`title`,`hideUntil`,`remoteId`,`recurrence`,`deleted`,`notificationFlags`,`calendarUri`,`modified`,`_id`,`lastNotified`,`elapsedSeconds`) SELECT `parent`,`notes`,`timerStart`,`estimatedSeconds`,`importance`,`created`,`collapsed`,`dueDate`,`completed`,`repeatUntil`,`title`,`hideUntil`,`remoteId`,`recurrence`,`deleted`,`notificationFlags`,`calendarUri`,`modified`,`_id`,`lastNotified`,`elapsedSeconds` FROM `tasks`")
            connection.execSQL("DROP TABLE `tasks`")
            connection.execSQL("ALTER TABLE `_new_tasks` RENAME TO `tasks`")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `t_rid` ON `tasks` (`remoteId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `active_and_visible` ON `tasks` (`completed`, `deleted`, `hideUntil`)")
        }
    }

    private val MIGRATION_84_85 = object : Migration(84, 85) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tasks` ADD COLUMN `repeat_from` INTEGER NOT NULL DEFAULT ${Task.RepeatFrom.DUE_DATE}")
            connection
                .prepare("SELECT `_id`, `repeatUntil`, `recurrence` FROM `tasks` WHERE `recurrence` IS NOT NULL AND `recurrence` != ''")
                .use { cursor ->
                    while (cursor.step()) {
                        val id = cursor.getLong(0)
                        val recurrence = if (!cursor.isNull(2)) {
                            cursor.getText(2).takeIf { it.isNotBlank() }
                        } else {
                            null
                        } ?: continue
                        val recur = newRecur(recurrence.withoutFrom()!!)
                        if (!cursor.isNull(1)) {
                            cursor.getLong(1).takeIf { it > 0 }?.let { recur.until = DateTime(it).toDate() }
                        }
                        val repeatFrom = recurrence.repeatFrom()
                        connection.execSQL("UPDATE `tasks` SET `repeat_from` = $repeatFrom, `recurrence` = '$recur' WHERE `_id` = $id")
                    }
                }
            connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `importance` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `hideUntil` INTEGER NOT NULL, `created` INTEGER NOT NULL, `modified` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `notes` TEXT, `estimatedSeconds` INTEGER NOT NULL, `elapsedSeconds` INTEGER NOT NULL, `timerStart` INTEGER NOT NULL, `notificationFlags` INTEGER NOT NULL, `lastNotified` INTEGER NOT NULL, `recurrence` TEXT, `repeat_from` INTEGER NOT NULL DEFAULT 0, `calendarUri` TEXT, `remoteId` TEXT, `collapsed` INTEGER NOT NULL, `parent` INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO `_new_tasks` (`parent`,`notes`,`timerStart`,`estimatedSeconds`,`importance`,`created`,`collapsed`,`dueDate`,`completed`,`title`,`hideUntil`,`remoteId`,`recurrence`,`deleted`,`notificationFlags`,`calendarUri`,`modified`,`_id`,`lastNotified`,`elapsedSeconds`,`repeat_from`) SELECT `parent`,`notes`,`timerStart`,`estimatedSeconds`,`importance`,`created`,`collapsed`,`dueDate`,`completed`,`title`,`hideUntil`,`remoteId`,`recurrence`,`deleted`,`notificationFlags`,`calendarUri`,`modified`,`_id`,`lastNotified`,`elapsedSeconds`,`repeat_from` FROM `tasks`")
            connection.execSQL("DROP TABLE `tasks`")
            connection.execSQL("ALTER TABLE `_new_tasks` RENAME TO `tasks`")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `t_rid` ON `tasks` (`remoteId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `active_and_visible` ON `tasks` (`completed`, `deleted`, `hideUntil`)")
        }
    }

    private val MIGRATION_85_86 = object : Migration(85, 86) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS `attachment_file` (`file_id` INTEGER PRIMARY KEY AUTOINCREMENT, `file_uuid` TEXT NOT NULL, `filename` TEXT NOT NULL, `uri` TEXT NOT NULL)")
            connection.execSQL("INSERT INTO `attachment_file` (`file_id`, `uri`,`filename`,`file_id`,`file_uuid`) SELECT `_id`, `path`,`name`,`_id`,`remoteId` FROM `task_attachments`")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `attachment` (`attachment_id` INTEGER PRIMARY KEY AUTOINCREMENT, `task` INTEGER NOT NULL, `file` INTEGER NOT NULL, `file_uuid` TEXT NOT NULL, FOREIGN KEY(`task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`file`) REFERENCES `attachment_file`(`file_id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_attachment_task` ON `attachment` (`task`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_attachment_file` ON `attachment` (`file`)")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attachment_task_file` ON `attachment` (`task`, `file`)")

            connection.execSQL(
                "INSERT INTO `attachment` (`task`, `file`, `file_uuid`) SELECT `tasks`.`_id`, `task_attachments`.`_id`, `task_attachments`.`remoteId` FROM `task_attachments` JOIN `tasks` ON `tasks`.`remoteId` = `task_attachments`.`task_id`"
            )

            connection.execSQL("DROP TABLE `task_attachments`")
        }
    }

    private val MIGRATION_86_87 = object : Migration(86, 87) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tasks` ADD COLUMN `read_only` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("UPDATE `tasks` SET `read_only` = 1 WHERE `_id` IN (SELECT `cd_task` FROM `caldav_tasks` INNER JOIN `caldav_lists` ON `caldav_tasks`.`cd_calendar` = `caldav_lists`.`cdl_uuid` WHERE `cdl_access` = $ACCESS_READ_ONLY)")
        }
    }

    private fun migration_87_88(context: Context) = object : Migration(87, 88) {
        override fun migrate(connection: SQLiteConnection) {
            val prefs = Preferences(context)
            val defaultList = prefs.getStringValue(R.string.p_default_list)?.split(":")
            if (
                (defaultList?.size == 2) &&
                (defaultList[0].toIntOrNull()?.equals(DefaultFilterProvider.TYPE_GOOGLE_TASKS) == true)
            ) {
                connection.prepare("SELECT `gtl_remote_id` FROM `google_task_lists` WHERE `gtl_id` = '${defaultList[1]}'").use { cursor ->
                    if (cursor.step()) {
                        if (!cursor.isNull(0)) {
                            val uuid = cursor.getText(0)
                            prefs.setString(R.string.p_default_list, "${DefaultFilterProvider.TYPE_GOOGLE_TASKS}:$uuid")
                        }
                    }
                }
            }
            // migrate google task accounts and lists to caldav table
            connection.execSQL("ALTER TABLE `caldav_lists` ADD COLUMN `cdl_last_sync` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("INSERT INTO `caldav_accounts` (`cda_account_type`, `cda_server_type`, `cda_uuid`, `cda_name`, `cda_username`, `cda_collapsed`) SELECT $TYPE_GOOGLE_TASKS, $SERVER_UNKNOWN, `gta_account`, `gta_account`, `gta_account`, `gta_collapsed` FROM `google_task_accounts`")
            connection.execSQL("INSERT INTO `caldav_lists` (`cdl_account`, `cdl_uuid`, `cdl_name`, `cdl_color`, `cdl_icon`, `cdl_order`, `cdl_access`, `cdl_last_sync`) SELECT `gtl_account`, `gtl_remote_id`, `gtl_title`, `gtl_color`, `gtl_icon`, `gtl_remote_order`, $ACCESS_OWNER, `gtl_last_sync` FROM `google_task_lists`")
            connection.execSQL("DROP TABLE `google_task_accounts`")
            connection.execSQL("DROP TABLE `google_task_lists`")
            // move cd_order to task table
            connection.execSQL("ALTER TABLE `tasks` ADD COLUMN `order` INTEGER")
            connection.execSQL("ALTER TABLE `caldav_tasks` RENAME TO `caldav-temp`")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `caldav_tasks` (`cd_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cd_task` INTEGER NOT NULL, `cd_calendar` TEXT, `cd_object` TEXT, `cd_remote_id` TEXT, `cd_etag` TEXT, `cd_last_sync` INTEGER NOT NULL, `cd_deleted` INTEGER NOT NULL, `cd_remote_parent` TEXT, `gt_moved` INTEGER NOT NULL, `gt_remote_order` INTEGER NOT NULL, FOREIGN KEY(`cd_task`) REFERENCES `tasks`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            connection.execSQL("DROP INDEX `index_caldav_tasks_cd_task`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_caldav_tasks_cd_task` ON `caldav_tasks` (`cd_task`)")
            connection.execSQL("INSERT INTO `caldav_tasks` (`cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_remote_parent`, `gt_moved`, `gt_remote_order`) SELECT `cd_id`, `cd_task`, `cd_calendar`, `cd_object`, `cd_remote_id`, `cd_etag`, `cd_last_sync`, `cd_deleted`, `cd_remote_parent`, 0, 0 FROM `caldav-temp`")
            connection.execSQL("DROP TABLE `caldav-temp`")
            connection.execSQL("INSERT INTO `caldav_tasks` (`cd_task`, `cd_calendar`, `cd_remote_id`, `cd_last_sync`, `cd_deleted`, `cd_remote_parent`, `gt_moved`, `gt_remote_order`) SELECT `gt_task`, `gt_list_id`, `gt_remote_id`, `gt_last_sync`, `gt_deleted`, `gt_remote_parent`, 0, 0 FROM google_tasks")
            connection.execSQL("DROP TABLE `google_tasks`")
        }
    }

    fun migrations(
        context: Context,
        fileStorage: FileStorage
    ) = arrayOf(
            MIGRATION_35_36,
            MIGRATION_36_37,
            MIGRATION_37_38,
            MIGRATION_38_39,
            noop(39, 46),
            MIGRATION_46_47,
            MIGRATION_47_48,
            MIGRATION_48_49,
            MIGRATION_49_50,
            MIGRATION_50_51,
            MIGRATION_51_52,
            MIGRATION_52_53,
            MIGRATION_53_54,
            MIGRATION_54_58,
            MIGRATION_58_59,
            MIGRATION_59_60,
            MIGRATION_60_61,
            MIGRATION_61_62,
            MIGRATION_62_63,
            MIGRATION_63_64,
            MIGRATION_64_65,
            MIGRATION_65_66,
            MIGRATION_66_67,
            MIGRATION_67_68,
            MIGRATION_68_69,
            MIGRATION_69_70,
            MIGRATION_70_71,
            MIGRATION_71_72,
            MIGRATION_72_73,
            MIGRATION_73_74,
            MIGRATION_74_75,
            MIGRATION_75_76,
            MIGRATION_76_77,
            MIGRATION_77_78,
            MIGRATION_78_79,
            MIGRATION_79_80,
            MIGRATION_80_81,
            migration_81_82(fileStorage),
            MIGRATION_82_83,
            MIGRATION_84_85,
            MIGRATION_85_86,
            MIGRATION_86_87,
            migration_87_88(context),
    )

    private fun noop(from: Int, to: Int): Migration = object : Migration(from, to) {
        override fun migrate(connection: SQLiteConnection) {}
    }

    fun String?.repeatFrom() = if (this?.contains("FROM=COMPLETION") == true) {
        Task.RepeatFrom.COMPLETION_DATE
    } else {
        Task.RepeatFrom.DUE_DATE
    }

    fun String?.withoutFrom(): String? = this?.replace(";?FROM=[^;]*".toRegex(), "")
}