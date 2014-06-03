package org.tasks;

import android.content.Context;
import android.content.Intent;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Broadcaster {

    private final Context context;

    @Inject
    public Broadcaster(@ForApplication Context context) {
        this.context = context;
    }

    public void sendOrderedBroadcast(Intent intent) {
        sendOrderedBroadcast(intent, null);
    }

    public void sendOrderedBroadcast(Intent intent, String permissions) {
        context.sendOrderedBroadcast(intent, permissions);
    }
}
