/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Francois DESLANDES
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
package com.timsu.astrid.data.task;

import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to see in the TaskView activity */
public class TaskModelForProvider extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        AbstractController.KEY_ROWID,
        NAME,
        IMPORTANCE,
        PREFERRED_DUE_DATE,
        DEFINITE_DUE_DATE,
        "COALESCE(" + PREFERRED_DUE_DATE + ", 0) as pdd",
        "COALESCE(" + DEFINITE_DUE_DATE + ", 0) as ddd"
    };

    // --- constructors

    public TaskModelForProvider(Cursor cursor) {
        super(cursor);

        prefetchData(FIELD_LIST);
    }

    // --- getters

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public Importance getImportance() {
        return super.getImportance();
    }

    @Override
    public Date getPreferredDueDate() {
        return super.getPreferredDueDate();
    }

    @Override
    public Date getDefiniteDueDate() {
        return super.getDefiniteDueDate();
    }

    public void update(ContentValues newValues) {
        setValues.putAll(newValues);
    }
}
