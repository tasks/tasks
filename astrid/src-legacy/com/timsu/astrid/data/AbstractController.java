/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
abstract public class AbstractController {

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

    public AbstractController(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
    }

    abstract public void open();
    abstract public void close();

    // cursor iterator

    public static class CursorIterator<TYPE extends AbstractModel> implements Iterator<TYPE> {
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
