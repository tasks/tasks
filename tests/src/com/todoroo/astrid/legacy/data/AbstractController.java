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
package com.todoroo.astrid.legacy.data;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.timsu.astrid.data.LegacyAbstractModel;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.upgrade.Astrid2To3UpgradeTests;

/** Abstract controller class. Mostly contains some static fields */
abstract public class AbstractController {

    protected Context context;

    // special columns
    public static final String KEY_ROWID = "_id";

    // database and table names
    protected String tasksTable = Astrid2To3UpgradeTests.TASKS_TEST;
    protected String tagsTable = Astrid2To3UpgradeTests.TAGS_TEST;
    protected String tagTaskTable = Astrid2To3UpgradeTests.TAG_TASK_TEST;
    protected String alertsTable = Astrid2To3UpgradeTests.ALERTS_TEST;
    protected String syncTable = Astrid2To3UpgradeTests.SYNC_TEST;

    // stuff

    public AbstractController(Context context) {
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
