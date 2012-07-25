/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;


/**
 * DAO for reading and writing values from an Android ContentResolver
 *
 * @author Tim Su <tim@todoroo.com>
 *
 * @param <TYPE> model type
 */
public class ContentResolverDao<TYPE extends AbstractModel> {

    /** class of model */
    private final Class<TYPE> modelClass;

    /** base content uri */
    private final Uri baseUri;

    /** content resolver */
    private final ContentResolver cr;

    @Autowired
    protected Boolean debug;

    public ContentResolverDao(Class<TYPE> modelClass, Context context, Uri baseUri) {
        DependencyInjectionService.getInstance().inject(this);
        this.modelClass = modelClass;
        if(debug == null)
            debug = false;
        this.baseUri = baseUri;

        cr = context.getContentResolver();
    }

    /**
     * Returns a URI for a single id
     * @param id
     * @return
     */
    private Uri uriWithId(long id) {
        return Uri.withAppendedPath(baseUri, Long.toString(id));
    }

    /**
     * Delete specific item from the given table
     * @param id
     * @return number of rows affected
     */
    public int delete(long id) {
        return cr.delete(uriWithId(id), null, null);
    }

    /**
     * Delete by criteria
     * @param where
     * @return number of rows affected
     */
    public int deleteWhere(Criterion where) {
        return cr.delete(baseUri, where.toString(), null);
    }

    /**
     * Query content provider
     * @param query
     * @return
     */
    public TodorooCursor<TYPE> query(Query query) {
        if(debug)
            Log.i("SQL-" + modelClass.getSimpleName(), query.toString()); //$NON-NLS-1$
        Cursor cursor = query.queryContentResolver(cr, baseUri);
        return new TodorooCursor<TYPE>(cursor, query.getFields());
    }

    /**
     * Create new or save existing model
     * @param model
     * @return true if data was written to the db, false otherwise
     */
    public boolean save(TYPE model) {
        writeTransitoriesToModelContentValues(model);
        if(model.isSaved()) {
            if(model.getSetValues() == null)
                return false;
            if(cr.update(uriWithId(model.getId()), model.getSetValues(), null, null) != 0)
                return true;
        }
        Uri uri = cr.insert(baseUri, model.getMergedValues());
        long id = Long.parseLong(uri.getLastPathSegment());
        model.setId(id);
        model.markSaved();
        return true;
    }

    private void writeTransitoriesToModelContentValues(AbstractModel model) {
        Set<String> keys = model.getAllTransitoryKeys();
        if (keys != null) {
            ContentValues transitories = new ContentValues();
            for (String key : keys) {
                String newKey = AbstractModel.RETAIN_TRANSITORY_PREFIX + key;
                Object value = model.getTransitory(key);
                AndroidUtilities.putInto(transitories, newKey, value, false);
            }
            model.mergeWith(transitories);
        }
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
        TodorooCursor<TYPE> cursor = query(
                Query.select(properties).where(AbstractModel.ID_PROPERTY.eq(id)));
        try {
            if (cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
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
            try {
                cursor.close();
            } catch (NullPointerException e) {
                // cursor was not open
            }
        }
    }
}
