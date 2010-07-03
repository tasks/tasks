/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.andlib.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import android.content.ContentValues;
import android.database.Cursor;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;



/**
 * Abstract data access object
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GenericDao<TYPE extends AbstractModel> {

    private final Class<TYPE> modelClass;

    private Table table;

    private AbstractDatabase database;

    public GenericDao(Class<TYPE> modelClass) {
        this.modelClass = modelClass;
    }

    public GenericDao(Class<TYPE> modelClass, AbstractDatabase database) {
        this.modelClass = modelClass;
        setDatabase(database);
    }

    /**
     * Sets up a database
     * @param database
     */
    protected void setDatabase(AbstractDatabase database) {
        this.database = database;
        table = database.getTable(modelClass);
    }

    // --- dao methods

    /**
     * Construct a query with SQL DSL objects
     * @param database
     * @param properties
     * @param builder
     * @param where
     * @param groupBy
     * @param sortOrder
     * @return
     */
    public TodorooCursor<TYPE> query(Query query) {
        query.from(table);
        Cursor cursor = database.getDatabase().rawQuery(query.toString(), null);
        return new TodorooCursor<TYPE>(cursor, query.getFields());
    }

    /**
     * Returns object corresponding to the given identifier
     *
     * @param database
     * @param table
     *            name of table
     * @param properties
     *            properties to read
     * @param id
     *            id of item
     * @return
     */
    public TYPE fetch(long id, Property<?>... properties) {
        TodorooCursor<TYPE> cursor = fetchItem(id, properties);
        try {
            if (cursor.getCount() == 0)
                return null;
            Constructor<TYPE> constructor = modelClass.getConstructor(TodorooCursor.class);
            return constructor.newInstance(cursor);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete the given id
     *
     * @param database
     * @param id
     * @return true if delete was successful
     */
    public boolean delete(long id) {
        return database.getDatabase().delete(table.name,
                AbstractModel.ID_PROPERTY.eq(id).toString(), null) > 0;
    }

    /**
     * Delete all matching a clause
     * @param database
     * @param where
     * @return # of deleted items
     */
    public int deleteWhere(Criterion where) {
        return database.getDatabase().delete(table.name,
                where.toString(), null);
    }

    /**
     * Save the given object to the database. Creates a new object if
     * model id property has not been set
     *
     * @return true on success.
     */
    public boolean persist(AbstractModel item) {
        if (item.getId() == AbstractModel.NO_ID) {
            return createNew(item);
        } else {
            ContentValues values = item.getSetValues();

            if (values.size() == 0) // nothing changed
                return true;

            return saveExisting(item);
        }
    }

    /**
     * Creates the given item.
     *
     * @param database
     * @param table
     *            table name
     * @param item
     *            item model
     * @return returns true on success.
     */
    public boolean createNew(AbstractModel item) {
        long newRow = database.getDatabase().insert(table.name,
                AbstractModel.ID_PROPERTY.name, item.getMergedValues());
        item.setId(newRow);
        return newRow >= 0;
    }

    /**
     * Saves the given item. Will not create a new item!
     *
     * @param database
     * @param table
     *            table name
     * @param item
     *            item model
     * @return returns true on success.
     */
    public boolean saveExisting(AbstractModel item) {
        ContentValues values = item.getSetValues();
        if(values.size() == 0) // nothing changed
            return true;
        return database.getDatabase().update(table.name, values,
                AbstractModel.ID_PROPERTY.eq(item.getId()).toString(), null) > 0;
    }

    // --- helper methods


    /**
     * Returns cursor to object corresponding to the given identifier
     *
     * @param database
     * @param table
     *            name of table
     * @param properties
     *            properties to read
     * @param id
     *            id of item
     * @return
     */
    protected TodorooCursor<TYPE> fetchItem(long id, Property<?>... properties) {
        TodorooCursor<TYPE> cursor = query(
                Query.select(properties).where(AbstractModel.ID_PROPERTY.eq(id)));
        cursor.moveToFirst();
        return new TodorooCursor<TYPE>(cursor, properties);
    }
}
