/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class GtasksStartupReceiver extends BroadcastReceiver {

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    /** Called when device is restarted */
    public void onReceive(final Context context, Intent intent) {
        ContextManager.setContext(context);
        new GtasksBackgroundService().scheduleService();
    }

}
