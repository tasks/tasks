/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

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
abstract public class AbstractDatabase {

    private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

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
     * @return true if upgrade was handled, false otherwise
     */
    protected abstract boolean onUpgrade(int oldVersion, int newVersion);

    // --- protected variables

    /**
     * SQLiteOpenHelper that takes care of database operations
     */
    private SQLiteOpenHelper helper = null;

    /**
     * Internal pointer to open database. Hides the fact that there is a
     * database and a wrapper by making a single monolithic interface
     */
    protected SQLiteDatabase database = null;

    // --- listeners

    /**
     * Interface for responding to database changes
     */
    public interface DatabaseUpdateListener {
        /**
         * Called when an INSERT, UPDATE, or DELETE occurs
         */
        public void onDatabaseUpdated();
    }

    private final ArrayList<DatabaseUpdateListener> listeners = new ArrayList<>();

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
        for(Table table : getTables()) {
            if(table.modelClass.equals(modelType)) {
                return table;
            }
        }
        throw new UnsupportedOperationException("Unknown model class " + modelType); //$NON-NLS-1$
    }

    private synchronized void initializeHelper() {
        if(helper == null) {
            if(ContextManager.getContext() == null) {
                throw new NullPointerException("Null context creating database helper");
            }
            helper = new DatabaseHelper(ContextManager.getContext(), getName(), getVersion());
        }
    }

    /**
     * Open the database for writing. Must be closed afterwards. If user is
     * out of disk space, database may be opened for reading instead
     */
    public synchronized final void openForWriting() {
        initializeHelper();

        if(database != null && !database.isReadOnly() && database.isOpen()) {
            return;
        }

        try {
            database = helper.getWritableDatabase();
        } catch (NullPointerException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        } catch (final RuntimeException original) {
            log.error(original.getMessage(), original);
            try {
                // provide read-only database
                openForReading();
            } catch (Exception readException) {
                log.error(readException.getMessage(), readException);
                // throw original write exception
                throw original;
            }
        }
    }

    /**
     * Open the database for reading. Must be closed afterwards
     */
    public synchronized final void openForReading() {
        initializeHelper();
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

    public synchronized Cursor rawQuery(String sql) {
        return getDatabase().rawQuery(sql, null);
    }

    /*
     * @see android.database.sqlite.SQLiteDatabase#insert(String  table, String  nullColumnHack, ContentValues  values)
     */
    public synchronized long insert(String table, String nullColumnHack, ContentValues values) {
        long result;
        try {
            result = getDatabase().insertOrThrow(table, nullColumnHack, values);
        } catch (SQLiteConstraintException e) { // Throw these exceptions
            throw e;
        } catch (Exception e) { // Suppress others
            log.error(e.getMessage(), e);
            result = -1;
        }
        onDatabaseUpdated();
        return result;
    }

    /*
     * @see android.database.sqlite.SQLiteDatabase#delete(String  table, String  whereClause, String[] whereArgs)
     */
    public synchronized int delete(String table, String whereClause, String[] whereArgs) {
        int result = getDatabase().delete(table, whereClause, whereArgs);
        onDatabaseUpdated();
        return result;
    }

    public synchronized int update(String  table, ContentValues  values, String whereClause) {
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
        public synchronized void onCreate(SQLiteDatabase db) {
            StringBuilder sql = new StringBuilder();
            SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();

            // create tables
            for(Table table : getTables()) {
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
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            log.info("Upgrading database from version {} to {}.", oldVersion, newVersion);

            database = db;
            try {
                if(!AbstractDatabase.this.onUpgrade(oldVersion, newVersion)) {
                    // We don't know how to handle this case because someone forgot to
                    // implement the upgrade. We can't drop tables, we can only
                    // throw a nasty exception at this time

                    throw new IllegalStateException("Missing database migration " +
                            "from " + oldVersion + " to " + newVersion);
                }
            } catch (Exception e) {
                log.error("database-upgrade-{}-{}-{}", getName(), oldVersion, newVersion, e);
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

        @Override
        public String visitInteger(Property<Integer> property, Void data) {
            return String.format("%s INTEGER", property.getColumnName());
        }

        @Override
        public String visitLong(Property<Long> property, Void data) {
            return String.format("%s INTEGER", property.getColumnName());
        }

        @Override
        public String visitString(Property<String> property, Void data) {
            return String.format("%s TEXT", property.getColumnName());
        }
    }
}

