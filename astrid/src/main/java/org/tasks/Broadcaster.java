package org.tasks;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.tasks.injection.TasksModule.ForApplication;

@Singleton
public class Broadcaster {

    private final Context context;

    @Inject
    public Broadcaster(@ForApplication Context context) {
        this.context = context;
    }

    public void sendOrderedBroadcast(Intent intent) {
        sendOrderedBroadcast(context, intent);
    }

    public void sendOrderedBroadcast(Context context, Intent intent) {
        context.sendOrderedBroadcast(intent, null);
    }
}
