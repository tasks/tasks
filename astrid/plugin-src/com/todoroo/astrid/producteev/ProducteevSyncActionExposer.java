/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

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
public class ProducteevSyncActionExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        // if we aren't logged in, don't expose sync action
        if(!ProducteevUtilities.INSTANCE.isLoggedIn())
            return;

        Intent syncIntent = new Intent(ProducteevBackgroundService.SYNC_ACTION, null,
                context, ProducteevBackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SyncAction syncAction = new SyncAction(context.getString(R.string.producteev_PPr_header),
                pendingIntent);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ProducteevUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, syncAction);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
