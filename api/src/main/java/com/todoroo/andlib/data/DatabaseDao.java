/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DAO for reading data from an instance of {@link AbstractDatabase}. If you
 * are writing an add-on for Astrid, you probably want to be using a subclass
 * of ContentResolverDao instead.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseDao<TYPE extends AbstractModel> {

    private final Class<TYPE> modelClass;

    private Table table;

    private AbstractDatabase database;

    @Autowired
    protected Boolean debug;

    public DatabaseDao(Class<TYPE> modelClass) {
        DependencyInjectionService.getInstance().inject(this);
        this.modelClass = modelClass;
        if(debug == null) {
            debug = false;
        }
    }

    /** Gets table associated with this DAO */
    public Table getTable() {
        return table;
    }

    /**
     * Sets database accessed by this DAO. Used for dependency-injected
     * initialization by child classes and unit tests
     */
    public void setDatabase(AbstractDatabase database) {
        if(database == this.database) {
            return;
        }
        this.database = database;
        table = database.getTable(modelClass);
    }

    // --- listeners

    public interface ModelUpdateListener<MTYPE> {
        public void onModelUpdated(MTYPE model);
    }

    private final ArrayList<ModelUpdateListener<TYPE>> listeners =
        new ArrayList<ModelUpdateListener<TYPE>>();

    public void addListener(ModelUpdateListener<TYPE> listener) {
        listeners.add(listener);
    }

    protected void onModelUpdated(TYPE model) {
        TYPE modelCopy = (TYPE) model.clone();
        for(ModelUpdateListener<TYPE> listener : listeners) {
            listener.onModelUpdated(modelCopy);
        }
    }

    // --- dao methods

    /**
     * Construct a query with SQL DSL objects
     */
    public TodorooCursor<TYPE> query(Query query) {
        query.from(table);
        if(debug) {
            Log.i("SQL-" + modelClass.getSimpleName(), query.toString()); //$NON-NLS-1$
        }
        Cursor cursor = database.rawQuery(query.toString());
        return new TodorooCursor<TYPE>(cursor, query.getFields());
    }

    /**
     * Construct a query with raw SQL
     */
    public TodorooCursor<TYPE> rawQuery(String selection, String[] selectionArgs, Property<?>... properties) {
        String[] fields = new String[properties.length];
        for(int i = 0; i < properties.length; i++) {
            fields[i] = properties[i].name;
        }
        return new TodorooCursor<TYPE>(database.getDatabase().query(table.name,
                fields, selection, selectionArgs, null, null, null),
                properties);
    }

    /**
     * Returns object corresponding to the given identifier
     * @param properties
     *            properties to read
     * @param id
     *            id of item
     * @return null if no item found
     */
    public TYPE fetch(long id, Property<?>... properties) {
        TodorooCursor<TYPE> cursor = fetchItem(id, properties);
        return returnFetchResult(cursor);
    }

    protected TYPE returnFetchResult(TodorooCursor<TYPE> cursor) {
        try {
            if (cursor.getCount() == 0) {
                return null;
            }
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
     * @return true if delete was successful
     */
    public boolean delete(long id) {
        return database.delete(table.name,
                AbstractModel.ID_PROPERTY.eq(id).toString(), null) > 0;
    }

    /**
     * Delete all matching a clause
     * @param where predicate for deletion
     * @return # of deleted items
     */
    public int deleteWhere(Criterion where) {
        return database.delete(table.name,
                where.toString(), null);
    }

    /**
     * Update all matching a clause to have the values set on template object.
     * <p>
     * Example (updates "joe" => "bob" in metadata value1):
     * {code}
     * Metadata item = new Metadata();
     * item.setValue(Metadata.VALUE1, "bob");
     * update(item, Metadata.VALUE1.eq("joe"));
     * {code}
     * @param where sql criteria
     * @param template set fields on this object in order to set them in the db.
     * @return # of updated items
     */
    public int update(Criterion where, TYPE template) {
        return database.update(table.name, template.getSetValues(),
                where.toString());
    }

    /**
     * Save the given object to the database. Creates a new object if
     * model id property has not been set
     *
     * @return true on success.
     */
    public boolean persist(TYPE item) {
        if (item.getId() == AbstractModel.NO_ID) {
            return createNew(item);
        } else {
            ContentValues values = item.getSetValues();

            if (values.size() == 0) // nothing changed
            {
                return true;
            }

            return saveExisting(item);
        }
    }

    private interface DatabaseChangeOp {
        public boolean makeChange();
    }

    private boolean insertOrUpdateAndRecordChanges(TYPE item, DatabaseChangeOp op) {
        final AtomicBoolean result = new AtomicBoolean(false);
        synchronized(database) {
            result.set(op.makeChange());
            if (result.get()) {
                onModelUpdated(item);
                item.markSaved();
            }
        }
        return result.get();
    }

    /**
     * Creates the given item.
     * @param item
     *            item model
     * @return returns true on success.
     */
    public boolean createNew(final TYPE item) {
        item.clearValue(AbstractModel.ID_PROPERTY);

        DatabaseChangeOp insert = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                long newRow = database.insert(table.name,
                        AbstractModel.ID_PROPERTY.name, item.getMergedValues());
                boolean result = newRow >= 0;
                if (result) {
                    item.setId(newRow);
                }
                return result;
            }
        };
        return insertOrUpdateAndRecordChanges(item, insert);
    }

    /**
     * Saves the given item. Will not create a new item!
     * @param item
     *            item model
     * @return returns true on success.
     */
    public boolean saveExisting(final TYPE item) {
        final ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0) // nothing changed
        {
            return true;
        }
        DatabaseChangeOp update = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                return database.update(table.name, values,
                        AbstractModel.ID_PROPERTY.eq(item.getId()).toString()) > 0;
            }
        };
        return insertOrUpdateAndRecordChanges(item, update);
    }

    // --- helper methods

    /**
     * Returns cursor to object corresponding to the given identifier
     * @param properties
     *            properties to read
     * @param id
     *            id of item
     */
    protected TodorooCursor<TYPE> fetchItem(long id, Property<?>... properties) {
        TodorooCursor<TYPE> cursor = query(
                Query.select(properties).where(AbstractModel.ID_PROPERTY.eq(id)));
        cursor.moveToFirst();
        return new TodorooCursor<TYPE>(cursor, properties);
    }

    public int count(Query query) {
        TodorooCursor<TYPE> cursor = query(query);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }
}
