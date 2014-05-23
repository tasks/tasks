package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.Broadcaster;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class RefreshBroadcastReceiver extends InjectingBroadcastReceiver {

    @Inject Broadcaster broadcaster;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        broadcaster.sendOrderedBroadcast(context, new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_LIST_UPDATED));
        broadcaster.sendOrderedBroadcast(context, new Intent(AstridApiConstants.BROADCAST_EVENT_FILTER_LIST_UPDATED));
    }
}
