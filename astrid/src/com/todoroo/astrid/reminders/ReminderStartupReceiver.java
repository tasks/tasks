/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Service which handles jobs that need to be run when phone boots
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderStartupReceiver extends BroadcastReceiver {

    static {
        AstridDependencyInjector.initialize();
    }

    // --- system startup

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        startReminderSchedulingService(context);
    }

    public static void startReminderSchedulingService(Context context) {
        context.startService(new Intent(context, ReminderSchedulingService.class));
    }
}
