/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class MilkStartupReceiver extends BroadcastReceiver {

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    /** Called when device is restarted */
    public void onReceive(final Context context, Intent intent) {
        ContextManager.setContext(context);
        MilkBackgroundService.scheduleService();
    }

}
