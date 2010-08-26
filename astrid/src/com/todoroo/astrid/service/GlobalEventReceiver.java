package com.todoroo.astrid.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.utility.Flags;

/**
 * BroadcastReceiver for receiving Astrid events not associated with a
 * specific activity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class GlobalEventReceiver extends BroadcastReceiver {

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    private TaskService taskService;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null)
            return;

        DependencyInjectionService.getInstance().inject(this);

        if(AstridApiConstants.BROADCAST_EVENT_FLUSH_DETAILS.equals(intent.getAction())) {
            taskService.clearDetails();
            Flags.set(Flags.REFRESH);
        }
    }

}
