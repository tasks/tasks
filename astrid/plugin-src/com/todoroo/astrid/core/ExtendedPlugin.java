package com.todoroo.astrid.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;

public class ExtendedPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "extended";

    @Override
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Extended Filters", "Todoroo",
                "Provides extended filters for viewing subsets of your tasks");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
