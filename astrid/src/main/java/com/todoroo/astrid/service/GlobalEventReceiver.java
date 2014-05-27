/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.utility.Flags;

import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

/**
 * BroadcastReceiver for receiving Astrid events not associated with a
 * specific activity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class GlobalEventReceiver extends InjectingBroadcastReceiver {

    @Inject TaskService taskService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(intent == null) {
            return;
        }

        if(AstridApiConstants.BROADCAST_EVENT_FLUSH_DETAILS.equals(intent.getAction())) {
            taskService.clearDetails(Criterion.all);
            Flags.set(Flags.REFRESH);
        }
    }
}
