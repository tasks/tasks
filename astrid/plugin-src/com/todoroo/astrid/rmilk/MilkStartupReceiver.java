/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MilkStartupReceiver extends BroadcastReceiver {

    @Override
    /** Called when device is restarted */
    public void onReceive(final Context context, Intent intent) {
        MilkBackgroundService.scheduleService();
    }

}
