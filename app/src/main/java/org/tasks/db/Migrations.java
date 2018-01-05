package org.tasks.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.SqlConstructorVisitor;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

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

    private static final Migration MIGRATION_39_40 = new Migration(39, 40) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

        }
    };

    private static final Migration MIGRATION_40_41 = new Migration(40, 41) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

        }
    };

    public static Migration[] MIGRATIONS = new Migration[] {
            MIGRATION_35_36,
            MIGRATION_36_37,
            MIGRATION_37_38,
            MIGRATION_38_39,
            MIGRATION_39_40,
            MIGRATION_40_41
    };

    public static RoomDatabase.Callback ON_CREATE = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            StringBuilder sql = new StringBuilder();
            SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();

            // create tables
            for(Table table : Database.TABLES) {
                sql.append("CREATE TABLE IF NOT EXISTS ").append(table.name).append('(').
                        append(AbstractModel.ID_PROPERTY).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
                for(Property<?> property : table.getProperties()) {
                    if(AbstractModel.ID_PROPERTY.name.equals(property.name)) {
                        continue;
                    }
                    sql.append(',').append(property.accept(sqlVisitor, null));
                }
                sql.append(')');
                db.execSQL(sql.toString());
                sql.setLength(0);
            }

            sql.setLength(0);
            sql.append("CREATE INDEX IF NOT EXISTS md_tid ON ").
                    append(Metadata.TABLE).append('(').
                    append(Metadata.TASK.name).
                    append(')');
            db.execSQL(sql.toString());
            sql.setLength(0);

            sql.append("CREATE INDEX IF NOT EXISTS md_tkid ON ").
                    append(Metadata.TABLE).append('(').
                    append(Metadata.TASK.name).append(',').
                    append(Metadata.KEY.name).
                    append(')');
            db.execSQL(sql.toString());
            sql.setLength(0);

            sql.append("CREATE INDEX IF NOT EXISTS so_id ON ").
                    append(StoreObject.TABLE).append('(').
                    append(StoreObject.TYPE.name).append(',').
                    append(StoreObject.ITEM.name).
                    append(')');
            db.execSQL(sql.toString());
            sql.setLength(0);

            sql.append("CREATE UNIQUE INDEX IF NOT EXISTS t_rid ON ").
                    append(Task.TABLE).append('(').
                    append(Task.UUID.name).
                    append(')');
            db.execSQL(sql.toString());
            sql.setLength(0);
        }
    };
}
