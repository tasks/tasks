/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.andlib.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.service.ContextManager;

/**
 * AbstractDatabase is a database abstraction which wraps a SQLite database.
 * <p>
 * Users of this class are in charge of the database's lifecycle - ensuring that
 * the database is open when needed and closed when usage is finished. Within an
 * activity, this is typically accomplished through the onResume and onPause
 * methods, though if the database is not needed for the activity's entire
 * lifecycle, it can be closed earlier.
 * <p>
 * Direct querying is not recommended for type safety reasons. Instead, use one
 * of the service classes to issue the request and return a {@link TodorooCursor}.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
abstract public class AbstractDatabase {

	// --- abstract methods

    /**
     * @return database name
     */
    protected abstract String getName();

    /**
     * @return all tables in this database
     */
    protected abstract Table[] getTables();

    /**
     * @return database version
     */
    protected abstract int getVersion();

    /**
     * Called after database and tables are created. Use this method to
     * create indices and perform other database maintenance
     */
    protected abstract void onCreateTables();

    /**
     * Upgrades an open database from one version to the next
     * @param oldVersion
     * @param newVersion
     * @return true if upgrade was handled, false otherwise
     */
    protected abstract boolean onUpgrade(int oldVersion, int newVersion);

    // --- protected variables

    /**
     * SQLiteOpenHelper that takes care of database operations
     */
    protected SQLiteOpenHelper helper = null;

    /**
     * Internal pointer to open database. Hides the fact that there is a
     * database and a wrapper by making a single monolithic interface
     */
    protected SQLiteDatabase database = null;

	// --- internal implementation

    /**
     * Return the name of the table containing these models
     * @param modelType
     * @return
     */
    public final Table getTable(Class<? extends AbstractModel> modelType) {
        for(Table table : getTables()) {
            if(table.modelClass.equals(modelType))
                return table;
        }
        throw new UnsupportedOperationException("Unknown model class " + modelType); //$NON-NLS-1$
    }

    protected final void initializeHelper() {
        if(helper == null)
            helper = new DatabaseHelper(ContextManager.getContext(),
                    getName(), null, getVersion());
    }

    /**
     * Open the database for writing. Must be closed afterwards. If user is
     * out of disk space, database may be opened for reading instead
     */
    public synchronized final void openForWriting() {
        initializeHelper();

        try {
            database = helper.getWritableDatabase();
        } catch (SQLiteException writeException) {
            Log.e("database-" + getName(), "Error opening db",
                    writeException);
            try {
                // provide read-only database
                openForReading();
            } catch (SQLiteException readException) {
                // throw original write exception
                throw writeException;
            }
        }
    }

    /**
     * Open the database for reading. Must be closed afterwards
     */
    public synchronized final void openForReading() {
        initializeHelper();
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
     * Clear all data in database. Warning: this does what it says. Any open
     * database resources will be abruptly closed.
     */
    public synchronized final void clear() {
        close();
        ContextManager.getContext().deleteDatabase(getName());
    }

    /**
     * @return sql database. opens database if not yet open
     */
    public final SQLiteDatabase getDatabase() {
        // open database if requested
        if(database == null)
            openForWriting();
        return database;
    }

    /**
     * @return human-readable database name for debugging
     */
    @Override
    public String toString() {
        return "DB:" + getName();
    }

    // --- helper classes

    /**
     * Default implementation of Astrid database helper
     */
    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        /**
         * Called to create the database tables
         */
        @Override
        public synchronized void onCreate(SQLiteDatabase db) {
            StringBuilder sql = new StringBuilder();
            SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();

            // create tables
            for(Table table : getTables()) {
                sql.append("CREATE TABLE IF NOT EXISTS ").append(table.name).append('(').
                append(AbstractModel.ID_PROPERTY).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
                for(Property<?> property : table.getProperties()) {
                    if(AbstractModel.ID_PROPERTY.name.equals(property.name))
                        continue;
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
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("database-" + getName(), String.format("Upgrading database from version %d to %d.",
                    oldVersion, newVersion));

            database = db;
            if(!AbstractDatabase.this.onUpgrade(oldVersion, newVersion)) {
                // We don't know how to handle this case because someone forgot to
                // implement the upgrade. We can't drop tables, we can only
                // throw a nasty exception at this time

                throw new IllegalStateException("Missing database migration " +
                        "from " + oldVersion + " to " + newVersion);
            }
        }
    }

    /**
     * Visitor that returns SQL constructor for this property
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class SqlConstructorVisitor implements PropertyVisitor<String, Void> {

        public String visitDouble(Property<Double> property, Void data) {
            return String.format("%s REAL", property.name);
        }

        public String visitInteger(Property<Integer> property, Void data) {
            return String.format("%s INTEGER", property.name);
        }

        public String visitLong(Property<Long> property, Void data) {
            return String.format("%s INTEGER", property.name);
        }

        public String visitString(Property<String> property, Void data) {
            return String.format("%s TEXT", property.name);
        }
    }
}

