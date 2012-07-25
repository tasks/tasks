/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "reminders"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        /*Plugin plugin = new Plugin(IDENTIFIER, "Reminders", "Todoroo",
                "Provides notification reminders for tasks");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);*/
    }

}
