/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import java.util.LinkedHashSet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

public class AlarmTaskRepeatListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        long oldDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
        if(oldDueDate == 0)
            oldDueDate = DateUtilities.now();
        long newDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, -1);
        if(newDueDate <= 0 || newDueDate <= oldDueDate)
            return;

        TodorooCursor<Metadata> cursor = AlarmService.getInstance().getAlarms(taskId);
        try {
            if(cursor.getCount() == 0)
                return;

            Metadata metadata = new Metadata();
            LinkedHashSet<Long> alarms = new LinkedHashSet<Long>(cursor.getCount());
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(cursor);
                alarms.add(metadata.getValue(AlarmFields.TIME) + (newDueDate - oldDueDate));
            }
            AlarmService.getInstance().synchronizeAlarms(taskId, alarms);

        } finally {
            cursor.close();
        }
    }

}
