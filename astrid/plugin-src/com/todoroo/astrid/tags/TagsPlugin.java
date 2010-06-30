package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Plugin;

public class TagsPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "tags";

    @Override
    public void onReceive(Context context, Intent intent) {
        Plugin plugin = new Plugin(IDENTIFIER, "Tags", "Todoroo",
                "Provides tagging support for tasks.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
