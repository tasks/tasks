/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;

import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class GtasksStartupReceiver extends InjectingBroadcastReceiver {

    @Inject GtasksScheduler scheduler;

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        ContextManager.setContext(context);
        scheduler.scheduleService();
    }
}
