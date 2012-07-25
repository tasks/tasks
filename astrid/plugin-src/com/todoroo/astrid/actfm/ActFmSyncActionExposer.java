/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.SyncAction;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Exposes sync action
 *
 */
public class ActFmSyncActionExposer extends BroadcastReceiver {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);

        // if we aren't logged in, don't expose sync action
        if(!actFmPreferenceService.isLoggedIn())
            return;

        SyncAction syncAction = new SyncAction(context.getString(R.string.actfm_APr_header),
                null);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ActFmPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, syncAction);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
