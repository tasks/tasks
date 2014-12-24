/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

import org.tasks.injection.InjectingBroadcastReceiver;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

public class AlarmTaskRepeatListener extends InjectingBroadcastReceiver {

    @Inject AlarmService alarmService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        long oldDueDateExtra = intent.getLongExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
        final long oldDueDate = oldDueDateExtra == 0 ? DateUtilities.now() : oldDueDateExtra;
        final long newDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, -1);

        if(newDueDate <= 0 || newDueDate <= oldDueDate) {
            return;
        }

        final Set<Long> alarms = new LinkedHashSet<>();
        alarmService.getAlarms(taskId, new Callback<Metadata>() {
            @Override
            public void apply(Metadata metadata) {
                alarms.add(metadata.getValue(AlarmFields.TIME) + (newDueDate - oldDueDate));
            }
        });
        if (!alarms.isEmpty()) {
            alarmService.synchronizeAlarms(taskId, alarms);
        }
    }
}
