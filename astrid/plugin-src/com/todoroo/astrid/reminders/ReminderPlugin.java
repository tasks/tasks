package com.todoroo.astrid.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Plugin;

public class ReminderPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        Plugin plugin = new Plugin(IDENTIFIER, "Reminders", "Todoroo",
                "Provides notification reminders for tasks");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
