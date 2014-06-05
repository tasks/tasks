/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;

import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class BackupStartupReceiver extends InjectingBroadcastReceiver {

    @Inject Preferences preferences;

    @Override
    /** Called when device is restarted */
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        ContextManager.setContext(context);
        BackupService.scheduleService(preferences, context);
    }
}
