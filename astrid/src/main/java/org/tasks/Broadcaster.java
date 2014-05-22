package org.tasks;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Broadcaster {

    @Inject
    public Broadcaster() {
    }

    public void sendOrderedBroadcast(Intent intent) {
        Context context = ContextManager.getContext();
        if (context != null) {
            sendOrderedBroadcast(context, intent);
        }
    }

    public void sendOrderedBroadcast(Context context, Intent intent) {
        context.sendOrderedBroadcast(intent, null);
    }
}
