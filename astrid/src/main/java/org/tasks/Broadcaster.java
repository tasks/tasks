package org.tasks;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;

public class Broadcaster {

    public void sendOrderedBroadcast(Intent intent) {
        Context context = ContextManager.getContext();
        if(context != null) {
            context.sendOrderedBroadcast(intent, null);
        }
    }
}
