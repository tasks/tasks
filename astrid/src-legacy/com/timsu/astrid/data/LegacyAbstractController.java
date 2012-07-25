/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;

/** Abstract controller class. Mostly contains some static fields */
@SuppressWarnings("nls")
abstract public class LegacyAbstractController {

    protected Context context;

    // special columns
    public static final String KEY_ROWID = "_id";

    // database and table names

    @Autowired
    protected String tasksTable;

    @Autowired
    protected String tagsTable;

    @Autowired
    protected String tagTaskTable;

    @Autowired
    protected String alertsTable;

    @Autowired
    protected String syncTable;

    // stuff

    public LegacyAbstractController(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
    }

    abstract public void open();
    abstract public void close();

    // cursor iterator

    public static class CursorIterator<TYPE extends LegacyAbstractModel> implements Iterator<TYPE> {
        Cursor cursor;
        Class<TYPE> cls;

        public CursorIterator(Cursor cursor, Class<TYPE> cls) {
            this.cursor = cursor;
            this.cls = cls;
        }

        public boolean hasNext() {
            return !cursor.isLast();
        }

        public TYPE next() {
            try {
                TYPE model = cls.getConstructor(Cursor.class).newInstance(cursor);
                cursor.moveToNext();
                return model;

            // ugh...
            } catch (IllegalArgumentException e) {
                Log.e("CursorIterator", e.toString());
            } catch (SecurityException e) {
                Log.e("CursorIterator", e.toString());
            } catch (InstantiationException e) {
                Log.e("CursorIterator", e.toString());
            } catch (IllegalAccessException e) {
                Log.e("CursorIterator", e.toString());
            } catch (InvocationTargetException e) {
                Log.e("CursorIterator", e.toString());
            } catch (NoSuchMethodException e) {
                Log.e("CursorIterator", e.toString());
            }

            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("Can't remove this way");
        }

    }

}
