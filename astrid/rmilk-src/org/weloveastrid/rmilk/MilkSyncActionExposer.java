/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.SyncAction;

/**
 * Exposes sync action
 *
 */
public class MilkSyncActionExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        // if we aren't logged in, don't expose sync action
        if(!MilkUtilities.INSTANCE.isLoggedIn())
            return;

        Intent syncIntent = new Intent(MilkBackgroundService.SYNC_ACTION, null,
                context, MilkBackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SyncAction syncAction = new SyncAction(context.getString(R.string.rmilk_MPr_header),
                pendingIntent);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, MilkUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, syncAction);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
