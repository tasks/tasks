/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Database wrapper
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Database extends AbstractDatabase {

    // --- constants

    /**
     * Database version number. This variable must be updated when database
     * tables are updated, as it determines whether a database needs updating.
     */
    public static final int VERSION = 1;

    /**
     * Database name
     */
    private static final String NAME = "database"; //$NON-NLS-1$

    /**
     * List of table/ If you're adding a new table, add it to this list and
     * also make sure that BenteSQLiteOpenHelper does the right thing.
     */
    public static final Table[] TABLES =  new Table[] {
        Task.TABLE,
        Metadata.TABLE,
    };

    // --- implementation

    /**
     * Creates a database wrapper
     */
    public Database() {
        //
    }

    @Override
    protected String getName() {
        return NAME;
    }

    protected int getVersion() {
        return VERSION;
    }

    @Override
    public Table[] getTables() {
        return TABLES;
    }

    /**
     * Default implementation of Astrid database helper
     */
    @SuppressWarnings("nls")
    private static class AstridSQLiteOpenHelper extends SQLiteOpenHelper {

        public AstridSQLiteOpenHelper(Context context, String name,
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
            for(Table table : TABLES) {
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

            // create indices
            sql.append("CREATE INDEX IF NOT EXISTS md_tid ON ").
                    append(Metadata.TABLE).append('(').
                append(Metadata.TASK.name).
            append(')');
            db.execSQL(sql.toString());
        }

        /**
         * Called to upgrade the database to a new version
         */
        @Override
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("database", String.format("Upgrading database from version %d to %d.",
                    oldVersion, newVersion));

            switch(oldVersion) {
            default:
                // We don't know how to handle this case because someone forgot to
                // implement the upgrade. We can't drop tables, we can only
                // throw a nasty exception at this time

                Log.e("database", "Unsupported migration, tables dropped!");
                throw new IllegalStateException("Missing database migration " +
                		"from " + oldVersion + " to " + newVersion);
            }
        }
    }

    /**
     * Gets the underlying database
     *
     * @return database object
     * @throws IllegalStateException if database hasn't been opened
     */
    @Override
    public SQLiteDatabase getDatabase() {
        if(database == null)
            throw new IllegalStateException("Tried to access an unopened database"); //$NON-NLS-1$
        return database;
    }

    /**
     * Get SQLite Helper
     */
    @Override
    protected SQLiteOpenHelper getHelper() {
        if(helper == null) {
            helper = new AstridSQLiteOpenHelper(ContextManager.getContext(),
                    NAME, null, VERSION);
        }
        return helper;
    }

    /**
     * Close the database. This should be used with caution, as there is
     * usually only one global connection to the database
     */
    @Override
    public synchronized void close() {
        super.close();
        helper = null;
    }
}

