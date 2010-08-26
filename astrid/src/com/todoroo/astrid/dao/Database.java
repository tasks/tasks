/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

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
    public static final int VERSION = 5;

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
    };

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

        sql.append("CREATE INDEX IF NOT EXISTS so_id ON ").
            append(StoreObject.TABLE).append('(').
                append(StoreObject.TYPE.name).append(',').
                append(StoreObject.ITEM.name).
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
            StringBuilder sql = new StringBuilder();
            sql.append("CREATE TABLE IF NOT EXISTS ").append(StoreObject.TABLE.name).append('(').
            append(AbstractModel.ID_PROPERTY).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
            for(Property<?> property : StoreObject.PROPERTIES) {
                if(AbstractModel.ID_PROPERTY.name.equals(property.name))
                    continue;
                sql.append(',').append(property.accept(visitor, null));
            }
            sql.append(')');
            database.execSQL(sql.toString());
            sql.setLength(0);

            sql.append("CREATE INDEX IF NOT EXISTS so_id ON ").
                append(StoreObject.TABLE).append('(').
                    append(StoreObject.TYPE.name).append(',').
                    append(StoreObject.ITEM.name).
                append(')');
            database.execSQL(sql.toString());
        }
        case 4: {
            database.execSQL("ALTER TABLE " + Task.TABLE.name + " ADD " +
                Task.DETAILS.accept(visitor, null));
        }

        return true;
        }

        return false;
    }

}

