package com.todoroo.astrid.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Plugin;

public class ExtendedPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "extended";

    @Override
    public void onReceive(Context context, Intent intent) {
        Plugin plugin = new Plugin(IDENTIFIER, "Extended Filters", "Todoroo",
                "Provides extended filters for viewing subsets of your tasks");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
