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
    public static final int VERSION = 36;

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

        case 35:
            tryExecSQL(addColumnSql(Task.TABLE, Task.REMAINING_SECONDS, visitor, "0"));
            TaskDao.migrateLoggedTime(database);

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
            log.error(e.getMessage(), e);
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

