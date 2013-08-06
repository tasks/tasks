/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;

import java.util.Date;


/**
 * Fields that you would want to synchronize in the TaskModel
 */
public class TaskModelForSync extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[]{
            LegacyAbstractController.KEY_ROWID,
            NAME,
            IMPORTANCE,
            ESTIMATED_SECONDS,
            ELAPSED_SECONDS,
            DEFINITE_DUE_DATE,
            PREFERRED_DUE_DATE,
            HIDDEN_UNTIL,
            BLOCKING_ON,
            PROGRESS_PERCENTAGE,
            CREATION_DATE,
            COMPLETION_DATE,
            NOTES,
            REPEAT,
            LAST_NOTIFIED,
            NOTIFICATIONS,
            NOTIFICATION_FLAGS,
            FLAGS,
    };

    // --- constructors

    public TaskModelForSync() {
        setCreationDate(new Date());
    }

    public TaskModelForSync(Cursor cursor) {
        super(cursor);
        prefetchData(FIELD_LIST);
    }
}

