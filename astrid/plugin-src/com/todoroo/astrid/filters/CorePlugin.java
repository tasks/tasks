package com.todoroo.astrid.filters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Plugin;

@SuppressWarnings("nls")
public class CorePlugin extends BroadcastReceiver {

    static final String pluginIdentifier = "core";

    @Override
    public void onReceive(Context context, Intent intent) {
        Plugin plugin = new Plugin(pluginIdentifier, "Core Filters", "Todoroo",
                "Provides 'Inbox' and 'All Tasks' Filters");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
