/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

/**
 * Exposes Task Detail for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AlarmDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(context, taskId);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, AlarmService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(Context context, long id) {
        TodorooCursor<Metadata> cursor = AlarmService.getInstance().getAlarms(id);
        long nextTime = -1;
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long time = cursor.get(AlarmFields.TIME);
                if(time > DateUtilities.now()) {
                    nextTime = time;
                    break;
                }
            }

            if(nextTime == -1)
                return null;
            int flags = DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME;
            Date today = new Date();
            Date alarm = new Date(nextTime);
            if(today.getYear() == alarm.getYear())
                flags |= DateUtils.FORMAT_NO_YEAR;
            if(alarm.getTime() - today.getTime() > DateUtilities.ONE_DAY)
                flags |= DateUtils.FORMAT_SHOW_DATE;
            CharSequence durationString = DateUtils.formatDateTime(context, nextTime,
                     flags);
            return "<img src='silk_clock'/> " + durationString; //$NON-NLS-1$
        } finally {
            cursor.close();
        }
    }

}
