package com.todoroo.astrid.notes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Plugin;

public class NotesPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "notes";

    @Override
    public void onReceive(Context context, Intent intent) {
        Plugin plugin = new Plugin(IDENTIFIER, "Notes", "Todoroo",
                "Lets you add and view notes for a task.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_PLUGINS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_PLUGIN, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
