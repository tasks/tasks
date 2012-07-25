/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.legacy.data.task;

import java.util.Date;

import android.database.Cursor;

import com.todoroo.astrid.legacy.data.AbstractController;
import com.todoroo.astrid.legacy.data.enums.Importance;



/** Fields that you would want to see in the TaskView activity */
public class TaskModelForWidget extends AbstractTaskModel {

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

    public TaskModelForWidget(Cursor cursor) {
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
}
