/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;

public class CorePlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "core"; //$NON-NLS-1$

    @Override
    @SuppressWarnings("nls")
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Core Filters", "Todoroo",
                "Provides 'Inbox', 'Search', and 'More...' Filters");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
