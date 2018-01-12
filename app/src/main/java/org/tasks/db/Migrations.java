package org.tasks.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

public class Migrations {
    private static final Migration MIGRATION_35_36 = new Migration(35, 36) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `tagdata` ADD COLUMN `color` INTEGER DEFAULT -1");
        }
    };

    private static final Migration MIGRATION_36_37 = new Migration(36, 37) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `store` ADD COLUMN `deleted` INTEGER DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_37_38 = new Migration(37, 38) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `store` ADD COLUMN `value4` TEXT DEFAULT -1");
        }
    };

    private static final Migration MIGRATION_38_39 = new Migration(38, 39) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `notification` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `type` INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX `index_notification_task` ON `notification` (`task`)");
        }
    };

    private static final Migration MIGRATION_46_47 = new Migration(46, 47) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `alarms` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `task` INTEGER, `time` INTEGER)");
            database.execSQL("INSERT INTO `alarms` (`task`, `time`) SELECT `task`, `value` FROM `metadata` WHERE `key` = 'alarm' AND `deleted` = 0");
            database.execSQL("DELETE FROM `metadata` WHERE `key` = 'alarm'");
        }
    };

    private static final Migration MIGRATION_47_48 = new Migration(47, 48) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `radius` INTEGER NOT NULL)");
            database.execSQL("INSERT INTO `locations` (`task`, `name`, `latitude`, `longitude`, `radius`) " +
                    "SELECT `task`, `value`, `value2`, `value3`, `value4` FROM `metadata` WHERE `key` = 'geofence' AND `deleted` = 0");
            database.execSQL("DELETE FROM `metadata` WHERE `key` = 'geofence'");
        }
    };

    private static final Migration MIGRATION_48_49 = new Migration(48, 49) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `name` TEXT, `tag_uid` TEXT, `task_uid` TEXT)");
            database.execSQL("INSERT INTO `tags` (`task`, `name`, `tag_uid`, `task_uid`) " +
                    "SELECT `task`, `value`, `value2`, `value3` FROM `metadata` WHERE `key` = 'tags-tag' AND `deleted` = 0");
            database.execSQL("DELETE FROM `metadata` WHERE `key` = 'tags-tag'");
        }
    };

    private static final Migration MIGRATION_49_50 = new Migration(49, 50) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `google_tasks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task` INTEGER NOT NULL, `remote_id` TEXT, `list_id` TEXT, `parent` INTEGER NOT NULL, `indent` INTEGER NOT NULL, `order` INTEGER NOT NULL, `remote_order` INTEGER NOT NULL, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL)");
            database.execSQL("INSERT INTO `google_tasks` (`task`, `remote_id`, `list_id`, `parent`, `indent`, `order`, `remote_order`, `last_sync`, `deleted`) " +
                    "SELECT `task`, `value`, `value2`, `value3`, `value4`, `value5`, `value6`, `value7`, `deleted` FROM `metadata` WHERE `key` = 'gtasks'");
            database.execSQL("DROP TABLE IF EXISTS `metadata`");
        }
    };

    private static final Migration MIGRATION_50_51 = new Migration(50, 51) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `filters` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `sql` TEXT, `values` TEXT, `criterion` TEXT)");
            database.execSQL("INSERT INTO `filters` (`title`, `sql`, `values`, `criterion`) " +
                    "SELECT `item`, `value`, `value2`, `value3` FROM `store` WHERE `type` = 'filter' AND `deleted` = 0");
            database.execSQL("DELETE FROM `store` WHERE `type` = 'filter'");
        }
    };

    private static final Migration MIGRATION_51_52 = new Migration(51, 52) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `google_task_lists` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remote_id` TEXT, `title` TEXT, `remote_order` INTEGER NOT NULL, `last_sync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `color` INTEGER)");
            database.execSQL("INSERT INTO `google_task_lists` (`remote_id`, `title`, `remote_order`, `last_sync`, `color`, `deleted`) " +
                    "SELECT `item`, `value`, `value2`, `value3`, `value4`, `deleted` FROM `store` WHERE `type` = 'gtasks-list'");
            database.execSQL("DROP TABLE IF EXISTS `store`");
        }
    };

    private static Migration NOOP(int from, int to) {
        return new Migration(from, to) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase database) {

            }
        };
    }

    public static Migration[] MIGRATIONS = new Migration[] {
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
            MIGRATION_51_52
    };
}
