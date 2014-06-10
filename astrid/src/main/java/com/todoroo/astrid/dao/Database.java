/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.UserActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Database wrapper
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class Database extends AbstractDatabase {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    // --- constants

    /**
     * Database version number. This variable must be updated when database
     * tables are updated, as it determines whether a database needs updating.
     */
    public static final int VERSION = 35;

    /**
     * Database name (must be unique)
     */
    private static final String NAME = "database";

    /**
     * List of table/ If you're adding a new table, add it to this list and
     * also make sure that our SQLite helper does the right thing.
     */
    public static final Table[] TABLES =  new Table[] {
        Task.TABLE,
        Metadata.TABLE,
        StoreObject.TABLE,
        TagData.TABLE,
        UserActivity.TABLE,
        TagMetadata.TABLE,
        TaskAttachment.TABLE,
        TaskListMetadata.TABLE,
    };

    // --- listeners

    @Inject
    public Database() {
    }

    // --- implementation

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected int getVersion() {
        return VERSION;
    }

    @Override
    public Table[] getTables() {
        return TABLES;
    }

    /**
     * Create indices
     */
    @Override
    protected synchronized void onCreateTables() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX IF NOT EXISTS md_tid ON ").
        append(Metadata.TABLE).append('(').
        append(Metadata.TASK.name).
        append(')');
        database.execSQL(sql.toString());
        sql.setLength(0);

        sql.append("CREATE INDEX IF NOT EXISTS md_tkid ON ").
        append(Metadata.TABLE).append('(').
        append(Metadata.TASK.name).append(',').
        append(Metadata.KEY.name).
        append(')');
        database.execSQL(sql.toString());
        sql.setLength(0);

        sql.append("CREATE INDEX IF NOT EXISTS so_id ON ").
        append(StoreObject.TABLE).append('(').
        append(StoreObject.TYPE.name).append(',').
        append(StoreObject.ITEM.name).
        append(')');
        database.execSQL(sql.toString());
        sql.setLength(0);

        sql.append("CREATE UNIQUE INDEX IF NOT EXISTS t_rid ON ").
        append(Task.TABLE).append('(').
        append(Task.UUID.name).
        append(')');
        database.execSQL(sql.toString());
        sql.setLength(0);
    }

    @Override
    protected synchronized boolean onUpgrade(int oldVersion, int newVersion) {
        SqlConstructorVisitor visitor = new SqlConstructorVisitor();
        switch(oldVersion) {
        case 1: {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.RECURRENCE.accept(visitor, null));
        }
        case 2: {
            for(Property<?> property : new Property<?>[] { Metadata.VALUE2,
                    Metadata.VALUE3, Metadata.VALUE4, Metadata.VALUE5 }) {
                database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                        property.accept(visitor, null));
            }
        }
        case 3: {
            database.execSQL(createTableSql(visitor, StoreObject.TABLE.name, StoreObject.PROPERTIES));

            onCreateTables();
        }
        case 4: {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.DETAILS.accept(visitor, null));
        }
        case 5: {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.REMINDER_SNOOZE.accept(visitor, null));
        }
        case 6: {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.DETAILS_DATE.accept(visitor, null));
        }
        case 7: {
            database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                    Metadata.CREATION_DATE.accept(visitor, null));
        }
        case 8: {
            // not needed anymore
        }
        case 9: try {
            Property<?>[] properties = new Property<?>[] { Task.UUID, Task.USER_ID };
            for(Property<?> property : properties) {
                database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                        property.accept(visitor, null) + " DEFAULT 0");
            }
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 10:
        case 11: try {
            database.execSQL(createTableSql(visitor, TagData.TABLE.name, TagData.PROPERTIES));
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 12:
        case 13: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.MEMBERS.accept(visitor, null));
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.MEMBER_COUNT.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 14: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.TASK_COUNT.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 15: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.LAST_SYNC.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 16: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.CREATOR_ID.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 17: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.TAG_DESCRIPTION.accept(visitor, null));
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 18: try {
            database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                    Metadata.VALUE6.accept(visitor, null));
            database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                    Metadata.VALUE7.accept(visitor, null));
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 19:
        case 20: try {
            String tasks = Task.TABLE.name;
            String id = Task.ID.name;
            String remoteId = Task.UUID.name;

            // Delete any items that have duplicate remote ids
            String deleteDuplicates = String.format("DELETE FROM %s WHERE %s IN (SELECT %s.%s FROM %s, %s AS t2 WHERE %s.%s < t2.%s AND %s.%s = t2.%s AND %s.%s > 0 GROUP BY %s.%s)",
                    tasks, id, tasks, id, tasks, tasks, tasks, id, id, tasks, remoteId, remoteId, tasks, remoteId, tasks, id);

            // Change all items with remote id = 0 to be remote id = NULL
            String changeZeroes = String.format("UPDATE %s SET %s = NULL WHERE %s = 0", tasks, remoteId, remoteId);

            database.execSQL(deleteDuplicates);
            database.execSQL(changeZeroes);

            onCreateTables();
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 21:
        case 22:
        case 23:
        case 24: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.REPEAT_UNTIL.accept(visitor, null));
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }

        case 25:
        case 26: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.TAG_ORDERING.accept(visitor, null));
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 27: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.SOCIAL_REMINDER.accept(visitor, null));
        } catch (SQLiteException e) {
            log.error("db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 28:
        case 29:
            tryExecSQL(createTableSql(visitor, TagMetadata.TABLE.name, TagMetadata.PROPERTIES));
            tryExecSQL(createTableSql(visitor, UserActivity.TABLE.name, UserActivity.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TaskAttachment.TABLE.name, TaskAttachment.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TaskListMetadata.TABLE.name, TaskListMetadata.PROPERTIES));

            tryExecSQL(addColumnSql(Task.TABLE, Task.PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.CLASSIFICATION, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.ATTACHMENTS_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.USER_ACTIVITIES_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.TASKS_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.METADATA_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.USER_ACTIVITIES_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(Metadata.TABLE, Metadata.DELETION_DATE, visitor, "0"));

        case 30:
        case 31:
        case 32:
        case 33:
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.LAST_AUTOSYNC, visitor, null));

        case 34:
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.IS_FOLDER, visitor, null));

        return true;
        }

        return false;
    }

    private void tryExecSQL(String sql) {
        try {
            database.execSQL(sql);
        } catch (SQLiteException e) {
            log.error("SQL Error: " + sql, e);
        }
    }

    private static String addColumnSql(Table table, Property<?> property, SqlConstructorVisitor visitor, String defaultValue) {
        StringBuilder builder = new StringBuilder();
        builder.append("ALTER TABLE ")
               .append(table.name)
               .append(" ADD ")
               .append(property.accept(visitor, null));
        if (!TextUtils.isEmpty(defaultValue)) {
            builder.append(" DEFAULT ").append(defaultValue);
        }
        return builder.toString();
    }

    public void tryAddColumn(Table table, Property<?> column, String defaultValue) {
        try {
            SqlConstructorVisitor visitor = new SqlConstructorVisitor();
            String sql = "ALTER TABLE " + table.name + " ADD " +  //$NON-NLS-1$//$NON-NLS-2$
                    column.accept(visitor, null);
            if (!TextUtils.isEmpty(defaultValue)) {
                sql += " DEFAULT " + defaultValue;
            }
            database.execSQL(sql);
        } catch (SQLiteException e) {
            // ignored, column already exists
        }
    }

    /**
     * Create table generation SQL
     */
    public String createTableSql(SqlConstructorVisitor visitor,
            String tableName, Property<?>[] properties) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append('(').
        append(AbstractModel.ID_PROPERTY).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
        for(Property<?> property : properties) {
            if(AbstractModel.ID_PROPERTY.name.equals(property.name)) {
                continue;
            }
            sql.append(',').append(property.accept(visitor, null));
        }
        sql.append(')');
        return sql.toString();
    }

}

