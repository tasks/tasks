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
            database.execSQL("INSERT INTO `alarms` (`task`, `time`) SELECT `task`, `value` FROM `metadata` WHERE `key` = 'alarm'");
            database.execSQL("DELETE FROM `metadata` WHERE `key` = 'alarm'");
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
            NOOP(39, 40),
            NOOP(40, 41),
            NOOP(41, 42),
            NOOP(42, 43),
            NOOP(43, 44),
            NOOP(44, 45),
            NOOP(45, 46),
            MIGRATION_46_47
    };
}
