/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteTransactionListener;
import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.SyncFlags;



/**
 * DAO for reading data from an instance of {@link AbstractDatabase}. If you
 * are writing an add-on for Astrid, you probably want to be using a subclass
 * of {@link ContentResolverDao} instead.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseDao<TYPE extends AbstractModel> {

    private final Class<TYPE> modelClass;

    private Table table;

    private Table outstandingTable;

    private AbstractDatabase database;

    @Autowired
    protected Boolean debug;

    public DatabaseDao(Class<TYPE> modelClass) {
        DependencyInjectionService.getInstance().inject(this);
        this.modelClass = modelClass;
        if(debug == null)
            debug = false;
    }

    public DatabaseDao(Class<TYPE> modelClass, AbstractDatabase database) {
        this(modelClass);
        setDatabase(database);
    }

    /** Gets table associated with this DAO */
    public Table getTable() {
        return table;
    }

    public Class<TYPE> getModelClass() {
        return modelClass;
    }

    /**
     * Sets database accessed by this DAO. Used for dependency-injected
     * initialization by child classes and unit tests
     *
     * @param database
     */
    public void setDatabase(AbstractDatabase database) {
        if(database == this.database)
            return;
        this.database = database;
        table = database.getTable(modelClass);
        outstandingTable = database.getOutstandingTable(modelClass);
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
     *
     * @param query
     * @return
     */
    public TodorooCursor<TYPE> query(Query query) {
        query.from(table);
        if(debug)
            Log.i("SQL-" + modelClass.getSimpleName(), query.toString()); //$NON-NLS-1$
        Cursor cursor = database.rawQuery(query.toString(), null);
        return new TodorooCursor<TYPE>(cursor, query.getFields());
    }

    /**
     * Construct a query with raw SQL
     *
     * @param properties
     * @param selection
     * @param selectionArgs
     * @return
     */
    public TodorooCursor<TYPE> rawQuery(String selection, String[] selectionArgs, Property<?>... properties) {
        String[] fields = new String[properties.length];
        for(int i = 0; i < properties.length; i++)
            fields[i] = properties[i].name;
        return new TodorooCursor<TYPE>(database.getDatabase().query(table.name,
                fields, selection, selectionArgs, null, null, null),
                properties);
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
     * @return null if no item found
     */
    public TYPE fetch(long id, Property<?>... properties) {
        TodorooCursor<TYPE> cursor = fetchItem(id, properties);
        return returnFetchResult(cursor);
    }

    protected TYPE returnFetchResult(TodorooCursor<TYPE> cursor) {
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
                where.toString(), null);
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
                return true;

            return saveExisting(item);
        }
    }

    private interface DatabaseChangeOp {
        public boolean makeChange();
    }

    private boolean insertOrUpdateAndRecordChanges(TYPE item, ContentValues values, DatabaseChangeOp op) {
        boolean recordOutstanding = (outstandingTable != null) &&
                !item.checkAndClearTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES);
        final AtomicBoolean result = new AtomicBoolean(false);

        if (recordOutstanding) { // begin transaction
            database.getDatabase().beginTransactionWithListener(new SQLiteTransactionListener() {
                @Override
                public void onRollback() {
                    result.set(false);
                }
                @Override
                public void onCommit() {/**/}
                @Override
                public void onBegin() {/**/}
            });
        }
        try {
            result.set(op.makeChange());
            if(result.get()) {
                if (recordOutstanding)
                    createOutstandingEntries(item.getId(), values); // Create entries for setValues in outstanding table
                onModelUpdated(item);
                item.markSaved();
            }
        } finally {
            if (recordOutstanding) // commit transaction
                database.getDatabase().endTransaction();
        }
        return result.get();
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
    public boolean createNew(final TYPE item) {
        item.clearValue(AbstractModel.ID_PROPERTY);

        DatabaseChangeOp insert = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                long newRow = database.insert(table.name,
                        AbstractModel.ID_PROPERTY.name, item.getMergedValues());
                boolean result = newRow >= 0;
                if (result)
                    item.setId(newRow);
                return result;
            }
        };
        return insertOrUpdateAndRecordChanges(item, item.getMergedValues(), insert);
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
    public boolean saveExisting(final TYPE item) {
        final ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0) // nothing changed
            return true;
        DatabaseChangeOp update = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                return database.update(table.name, values,
                        AbstractModel.ID_PROPERTY.eq(item.getId()).toString(), null) > 0;
            }
        };
        return insertOrUpdateAndRecordChanges(item, values, update);
    }

    private boolean createOutstandingEntries(long modelId, ContentValues modelSetValues) {
        Set<Entry<String, Object>> entries = modelSetValues.valueSet();
        long now = DateUtilities.now();
        for (Entry<String, Object> entry : entries) {
            if (entry.getValue() != null && shouldRecordOutstandingEntry(entry.getKey())) {
                AbstractModel m;
                try {
                    m = outstandingTable.modelClass.newInstance();
                } catch (IllegalAccessException e) {
                    return false;
                } catch (InstantiationException e2) {
                    return false;
                }
                m.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, modelId);
                m.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, entry.getKey());
                m.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, entry.getValue().toString());
                m.setValue(OutstandingEntry.CREATED_AT_PROPERTY, now);
                database.insert(outstandingTable.name, null, m.getSetValues());
            }
        }
        database.getDatabase().setTransactionSuccessful();
        return true;
    }

    /**
     * Returns true if an entry in the outstanding table should be recorded for this
     * column. Subclasses can override to return false for insignificant columns
     * (e.g. Task.DETAILS, last modified, etc.)
     * @param columnName
     * @return
     */
    protected boolean shouldRecordOutstandingEntry(String columnName) {
        return true;
    }

    /**
     * Updates multiple rows of the database based on model set values
     *
     * @param item
     *            item model
     * @param criterion
     * @return returns true on success.
     */
    public int updateMultiple(ContentValues values, Criterion criterion) {
        if(values.size() == 0) // nothing changed
            return 0;
        return database.update(table.name, values, criterion.toString(), null);
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
