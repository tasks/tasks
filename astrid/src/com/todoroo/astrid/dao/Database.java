/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.crittercism.app.Crittercism;
import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.data.ABTestEvent;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskAttachmentOutstanding;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskListMetadataOutstanding;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.data.UserActivityOutstanding;
import com.todoroo.astrid.data.WaitingOnMe;
import com.todoroo.astrid.data.WaitingOnMeOutstanding;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Database wrapper
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Database extends AbstractDatabase {

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
        Update.TABLE,
        User.TABLE,
        UserActivity.TABLE,
        ABTestEvent.TABLE,
        TagMetadata.TABLE,
        History.TABLE,
        TaskAttachment.TABLE,
        TaskListMetadata.TABLE,
        WaitingOnMe.TABLE,

        TaskOutstanding.TABLE,
        TagOutstanding.TABLE,
        UserActivityOutstanding.TABLE,
        TaskAttachmentOutstanding.TABLE,
        TaskListMetadataOutstanding.TABLE,
        WaitingOnMeOutstanding.TABLE
    };

    // --- listeners

    public Database() {
        super();
        addListener(new DatabaseUpdateListener() {
            @Override
            public void onDatabaseUpdated() {
                Astrid2TaskProvider.notifyDatabaseModification();
                Astrid3ContentProvider.notifyDatabaseModification();
                TasksWidget.updateWidgets(ContextManager.getContext());
            }
        });
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

        sql.append("CREATE INDEX IF NOT EXISTS hist_tag_id ON ").
        append(History.TABLE).append('(').
        append(History.TAG_ID.name).
        append(')');
        database.execSQL(sql.toString());
        sql.setLength(0);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="SF_SWITCH_FALLTHROUGH")
    protected synchronized boolean onUpgrade(int oldVersion, int newVersion) {
        SqlConstructorVisitor visitor = new SqlConstructorVisitor();
        switch(oldVersion) {
        case 1: {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.RECURRENCE.accept(visitor, null));
        }
        case 2: {
            for(Property<?> property : new Property<?>[] { Metadata.VALUE2,
                    Metadata.VALUE3, Metadata.VALUE4, Metadata.VALUE5 })
                database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                        property.accept(visitor, null));
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
            database.execSQL(createTableSql(visitor, Update.TABLE.name, Update.PROPERTIES));
            onCreateTables();

            Property<?>[] properties = new Property<?>[] { Task.UUID,
                    Task.USER_ID };

            for(Property<?> property : properties) {
                database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                        property.accept(visitor, null) + " DEFAULT 0");
            }

            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.USER.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 10: try {
            //
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 11: try {
            database.execSQL(createTableSql(visitor, TagData.TABLE.name, TagData.PROPERTIES));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 12: try {
            database.execSQL("ALTER TABLE " + Update.TABLE.name + " ADD " +
                    Update.TAGS.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 13: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.MEMBERS.accept(visitor, null));
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.MEMBER_COUNT.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 14: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.TASK_COUNT.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 15: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.LAST_SYNC.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 16: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.CREATOR_ID.accept(visitor, null) + " DEFAULT 0");
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 17: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.TAG_DESCRIPTION.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 18: try {
            database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                    Metadata.VALUE6.accept(visitor, null));
            database.execSQL("ALTER TABLE " + Metadata.TABLE.name + " ADD " +
                    Metadata.VALUE7.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 19: try {
            for(Property<?> property : new Property<?>[] { Update.TASK_LOCAL, Update.TAGS_LOCAL })
                database.execSQL("ALTER TABLE " + Update.TABLE.name + " ADD " +
                        property.accept(visitor, null));
            database.execSQL("CREATE INDEX IF NOT EXISTS up_tid ON " +
                    Update.TABLE + "(" + Update.TASK_LOCAL.name + ")");
            database.execSQL("CREATE INDEX IF NOT EXISTS up_tid ON " +
                    Update.TABLE + "(" + Update.TAGS_LOCAL.name + ")");

        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
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
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 21: try {
            for(Property<?> property : new Property<?>[] { Update.OTHER_USER_ID, Update.OTHER_USER })
                database.execSQL("ALTER TABLE " + Update.TABLE.name + " ADD " +
                        property.accept(visitor, null));

        }
        catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 22: try {
            database.execSQL(createTableSql(visitor, User.TABLE.name, User.PROPERTIES));
            onCreateTables();
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }

        case 23: try {
            database.execSQL(createTableSql(visitor, ABTestEvent.TABLE.name, ABTestEvent.PROPERTIES));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }

        case 24: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.REPEAT_UNTIL.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }

        case 25: try {
            database.execSQL("ALTER TABLE " + User.TABLE.name + " ADD " +
                    User.STATUS.accept(visitor, null));

            database.execSQL("ALTER TABLE " + User.TABLE.name + " ADD " +
                    User.PENDING_STATUS.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 26: try {
            database.execSQL("ALTER TABLE " + TagData.TABLE.name + " ADD " +
                    TagData.TAG_ORDERING.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 27: try {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                    Task.SOCIAL_REMINDER.accept(visitor, null));
        } catch (SQLiteException e) {
            Log.e("astrid", "db-upgrade-" + oldVersion + "-" + newVersion, e);
        }
        case 28:
        case 29:
            tryExecSQL(createTableSql(visitor, TaskOutstanding.TABLE.name, TaskOutstanding.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TagOutstanding.TABLE.name, TagOutstanding.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TaskAttachmentOutstanding.TABLE.name, TagOutstanding.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TagMetadata.TABLE.name, TagMetadata.PROPERTIES));
            tryExecSQL(createTableSql(visitor, UserActivity.TABLE.name, UserActivity.PROPERTIES));
            tryExecSQL(createTableSql(visitor, UserActivityOutstanding.TABLE.name, UserActivityOutstanding.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TaskAttachment.TABLE.name, TaskAttachment.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TaskListMetadata.TABLE.name, TaskListMetadata.PROPERTIES));
            tryExecSQL(createTableSql(visitor, TaskListMetadataOutstanding.TABLE.name, TaskListMetadataOutstanding.PROPERTIES));

            tryExecSQL(addColumnSql(Task.TABLE, Task.PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.IS_PUBLIC, visitor, "0"));
            tryExecSQL(addColumnSql(Task.TABLE, Task.IS_READONLY, visitor, "0"));
            tryExecSQL(addColumnSql(Task.TABLE, Task.CLASSIFICATION, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.HISTORY_FETCH_DATE, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.ATTACHMENTS_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(Task.TABLE, Task.USER_ACTIVITIES_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.HISTORY_FETCH_DATE, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.TASKS_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.METADATA_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.USER_ACTIVITIES_PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(Metadata.TABLE, Metadata.DELETION_DATE, visitor, "0"));
            tryExecSQL(addColumnSql(User.TABLE, User.PUSHED_AT, visitor, null));
            tryExecSQL(addColumnSql(User.TABLE, User.FIRST_NAME, visitor, null));
            tryExecSQL(addColumnSql(User.TABLE, User.LAST_NAME, visitor, null));

        case 30:
            tryExecSQL(createTableSql(visitor, WaitingOnMe.TABLE.name, WaitingOnMe.PROPERTIES));
            tryExecSQL(createTableSql(visitor, WaitingOnMeOutstanding.TABLE.name, WaitingOnMeOutstanding.PROPERTIES));

        case 31:
            tryExecSQL(addColumnSql(Task.TABLE, Task.HISTORY_HAS_MORE, visitor, null));
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.HISTORY_HAS_MORE, visitor, null));
        case 32:
            tryExecSQL("DROP TABLE " + History.TABLE.name);
            tryExecSQL(createTableSql(visitor, History.TABLE.name, History.PROPERTIES));
            tryExecSQL(addColumnSql(User.TABLE, User.TASKS_PUSHED_AT, visitor, null));
        case 33:
            tryExecSQL(addColumnSql(TagData.TABLE, TagData.LAST_AUTOSYNC, visitor, null));
            tryExecSQL(addColumnSql(User.TABLE, User.LAST_AUTOSYNC, visitor, null));

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
            Log.e("astrid", "SQL Error: " + sql, e);
            Crittercism.logHandledException(e);
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
            if (!TextUtils.isEmpty(defaultValue))
                sql += " DEFAULT " + defaultValue;
            database.execSQL(sql);
        } catch (SQLiteException e) {
            // ignored, column already exists
        }
    }

    /**
     * Create table generation SQL
     * @param sql
     * @param tableName
     * @param properties
     * @return
     */
    public String createTableSql(SqlConstructorVisitor visitor,
            String tableName, Property<?>[] properties) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append('(').
        append(AbstractModel.ID_PROPERTY).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
        for(Property<?> property : properties) {
            if(AbstractModel.ID_PROPERTY.name.equals(property.name))
                continue;
            sql.append(',').append(property.accept(visitor, null));
        }
        sql.append(')');
        return sql.toString();
    }

}

