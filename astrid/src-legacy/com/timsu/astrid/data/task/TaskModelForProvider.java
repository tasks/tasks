/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to see in the TaskView activity */
@SuppressWarnings("nls")
public class TaskModelForProvider extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
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
