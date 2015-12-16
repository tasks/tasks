/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.SqlConstructorVisitor;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.UserActivity;

import org.tasks.injection.ForApplication;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Database wrapper
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class Database {

    private static final int VERSION = 35;
    private static final String NAME = "database";
    private static final Table[] TABLES =  new Table[] {
            Task.TABLE,
            Metadata.TABLE,
            StoreObject.TABLE,
            TagData.TABLE,
            UserActivity.TABLE,
            TaskAttachment.TABLE,
            TaskListMetadata.TABLE,
    };

    private final ArrayList<DatabaseUpdateListener> listeners = new ArrayList<>();
    private final SQLiteOpenHelper helper;
    private SQLiteDatabase database;

    // --- listeners

    @Inject
    public Database(@ForApplication Context context) {
        helper = new DatabaseHelper(context, getName(), VERSION);
    }

    // --- implementation

    public String getName() {
        return NAME;
    }

    /**
     * Create indices
     */
    private void onCreateTables() {
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

    private boolean onUpgrade(int oldVersion, int newVersion) {
        SqlConstructorVisitor visitor = new SqlConstructorVisitor();
        switch(oldVersion) {
        }

        return false;
    }

    private void tryExecSQL(String sql) {
        try {
            database.execSQL(sql);
        } catch (SQLiteException e) {
            Timber.e(e, "SQL Error: " + sql);
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

    private void tryAddColumn(Table table, Property<?> column, String defaultValue) {
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
            Timber.e(e, e.getMessage());
        }
    }

    private String createTableSql(SqlConstructorVisitor visitor,
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

    public void addListener(DatabaseUpdateListener listener) {
        listeners.add(listener);
    }

    private void onDatabaseUpdated() {
        for(DatabaseUpdateListener listener : listeners) {
            listener.onDatabaseUpdated();
        }
    }

    /**
     * Return the name of the table containing these models
     */
    public final Table getTable(Class<? extends AbstractModel> modelType) {
        for(Table table : TABLES) {
            if(table.modelClass.equals(modelType)) {
                return table;
            }
        }
        throw new UnsupportedOperationException("Unknown model class " + modelType); //$NON-NLS-1$
    }

    /**
     * Open the database for writing. Must be closed afterwards. If user is
     * out of disk space, database may be opened for reading instead
     */
    public synchronized final void openForWriting() {
        if(database != null && !database.isReadOnly() && database.isOpen()) {
            return;
        }

        try {
            database = helper.getWritableDatabase();
        } catch (NullPointerException e) {
            Timber.e(e, e.getMessage());
            throw new IllegalStateException(e);
        } catch (final RuntimeException original) {
            Timber.e(original, original.getMessage());
            try {
                // provide read-only database
                openForReading();
            } catch (Exception readException) {
                Timber.e(readException, readException.getMessage());
                // throw original write exception
                throw original;
            }
        }
    }

    /**
     * Open the database for reading. Must be closed afterwards
     */
    public synchronized final void openForReading() {
        if(database != null && database.isOpen()) {
            return;
        }
        database = helper.getReadableDatabase();
    }

    /**
     * Close the database if it has been opened previously
     */
    public synchronized final void close() {
        if(database != null) {
            database.close();
        }
        database = null;
    }

    /**
     * @return sql database. opens database if not yet open
     */
    public synchronized final SQLiteDatabase getDatabase() {
        if(database == null) {
            AndroidUtilities.sleepDeep(300L);
            openForWriting();
        }
        return database;
    }

    /**
     * @return human-readable database name for debugging
     */
    @Override
    public String toString() {
        return "DB:" + getName();
    }

    // --- database wrapper

    public Cursor rawQuery(String sql) {
        return getDatabase().rawQuery(sql, null);
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        long result;
        try {
            result = getDatabase().insertOrThrow(table, nullColumnHack, values);
        } catch (SQLiteConstraintException e) { // Throw these exceptions
            throw e;
        } catch (Exception e) { // Suppress others
            Timber.e(e, e.getMessage());
            result = -1;
        }
        onDatabaseUpdated();
        return result;
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        int result = getDatabase().delete(table, whereClause, whereArgs);
        onDatabaseUpdated();
        return result;
    }

    public int update(String  table, ContentValues  values, String whereClause) {
        int result = getDatabase().update(table, values, whereClause, null);
        onDatabaseUpdated();
        return result;
    }

    // --- helper classes

    /**
     * Default implementation of Astrid database helper
     */
    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name, int version) {
            super(context, name, null, version);
        }

        /**
         * Called to create the database tables
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder sql = new StringBuilder();
            SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();

            // create tables
            for(Table table : TABLES) {
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

            // post-table-creation
            database = db;
            onCreateTables();
        }

        /**
         * Called to upgrade the database to a new version
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Timber.i("Upgrading database from version %s to %s", oldVersion, newVersion);

            database = db;
            try {
                if(!Database.this.onUpgrade(oldVersion, newVersion)) {
                    // We don't know how to handle this case because someone forgot to
                    // implement the upgrade. We can't drop tables, we can only
                    // throw a nasty exception at this time

                    throw new IllegalStateException("Missing database migration " +
                            "from " + oldVersion + " to " + newVersion);
                }
            } catch (Exception e) {
                Timber.e(e, "database-upgrade-%s-%s-%s", getName(), oldVersion, newVersion);
            }
        }
    }
}

