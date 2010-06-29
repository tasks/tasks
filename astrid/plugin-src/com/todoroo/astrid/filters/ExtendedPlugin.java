package com.todoroo.astrid.filters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Plugin;

@SuppressWarnings("nls")
public class ExtendedPlugin extends BroadcastReceiver {

    static final String pluginIdentifier = "extended";

    @Override
    public void onReceive(Context context, Intent intent) {
        Plugin plugin = new Plugin(pluginIdentifier, "Extended Filters", "Todoroo",
                "Provides extended filters for viewing subsets of your tasks");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
