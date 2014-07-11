/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

import org.tasks.injection.InjectingBroadcastReceiver;

import java.util.LinkedHashSet;

import javax.inject.Inject;

public class AlarmTaskRepeatListener extends InjectingBroadcastReceiver {

    @Inject AlarmService alarmService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        long oldDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
        if(oldDueDate == 0) {
            oldDueDate = DateUtilities.now();
        }
        long newDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, -1);
        if(newDueDate <= 0 || newDueDate <= oldDueDate) {
            return;
        }

        TodorooCursor<Metadata> cursor = alarmService.getAlarms(taskId);
        try {
            if(cursor.getCount() == 0) {
                return;
            }

            LinkedHashSet<Long> alarms = new LinkedHashSet<>(cursor.getCount());
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Metadata metadata = new Metadata(cursor);
                alarms.add(metadata.getValue(AlarmFields.TIME) + (newDueDate - oldDueDate));
            }
            alarmService.synchronizeAlarms(taskId, alarms);

        } finally {
            cursor.close();
        }
    }

}
