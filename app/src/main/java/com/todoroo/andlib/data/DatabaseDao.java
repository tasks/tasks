/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import android.content.ContentValues;
import android.database.Cursor;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;

import org.tasks.BuildConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * DAO for reading data from an instance of {@link Database}. If you
 * are writing an add-on for Astrid, you probably want to be using a subclass
 * of ContentResolverDao instead.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseDao {

    private final Table table = Task.TABLE;
    private final Database database;

    public DatabaseDao(Database database) {
        this.database = database;
    }

    public List<Task> toList(Query query) {
        return query(query).toList();
    }

    private Task getFirst(Query query) {
        return query(query).first();
    }

    /**
     * Construct a query with SQL DSL objects
     */
    public TodorooCursor query(Query query) {
        query.from(table);
        String queryString = query.toString();
        if (BuildConfig.DEBUG) {
            Timber.v(queryString);
        }
        Cursor cursor = database.rawQuery(queryString);
        return new TodorooCursor(cursor, query.getFields());
    }

    /**
     * Returns object corresponding to the given identifier
     * @param properties
     *            properties to read
     * @param id
     *            id of item
     * @return null if no item found
     */
    public Task fetch(long id, Property<?>... properties) {
        return getFirst(Query.select(properties).where(AbstractModel.ID_PROPERTY.eq(id)));
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
        Timber.v("deleteWhere(%s)", where);
        return database.delete(table.name,
                where.toString(), null);
    }

    /**
     * Update all matching a clause to have the values set on template object.
     * <p>
     * Example (updates "joe" => "bob" in metadata value1):
     * {code}
     * Metadata item = new Metadata();
     * item.setVALUE1("bob");
     * update(item, Metadata.VALUE1.eq("joe"));
     * {code}
     * @param where sql criteria
     * @param template set fields on this object in order to set them in the db.
     * @return # of updated items
     */
    public int update(Criterion where, Task template) {
        return database.update(table.name, template.getSetValues(),
                where.toString());
    }

    private interface DatabaseChangeOp {
        boolean makeChange();
    }

    private boolean insertOrUpdateAndRecordChanges(Task item, DatabaseChangeOp op) {
        final AtomicBoolean result = new AtomicBoolean(false);
        synchronized(database) {
            result.set(op.makeChange());
            if (result.get()) {
                item.markSaved();
                if (BuildConfig.DEBUG) {
                    Timber.v("%s %s", op, item.toString());
                }
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
    public boolean createNew(final Task item) {
        if (!item.containsValue(Task.UUID) || Task.isUuidEmpty(item.getUuidProperty())) {
            item.setUuidProperty(UUIDHelper.newUUID());
        }

        item.clearValue(AbstractModel.ID_PROPERTY);

        DatabaseChangeOp insert = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                long newRow = database.insert(table.name, item.getMergedValues());
                boolean result = newRow >= 0;
                if (result) {
                    item.setId(newRow);
                }
                return result;
            }

            @Override
            public String toString() {
                return "INSERT";
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
    public boolean saveExisting(final Task item) {
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

            @Override
            public String toString() {
                return "UPDATE";
            }
        };
        return insertOrUpdateAndRecordChanges(item, update);
    }

    // --- helper methods

    public int count(Query query) {
        return query(query).count();
    }
}
