package com.todoroo.astrid.timers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;

public class TimerPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "timer"; //$NON-NLS-1$

    @Override
    @SuppressWarnings("nls")
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Timer", "Todoroo",
                "Lets you time how long it takes to complete tasks.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
