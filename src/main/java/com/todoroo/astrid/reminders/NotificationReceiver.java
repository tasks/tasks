package com.todoroo.astrid.reminders;

import android.content.Context;
import android.content.Intent;

import org.tasks.Notifier;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class NotificationReceiver extends InjectingBroadcastReceiver {

    public static final String ID_KEY = "id"; //$NON-NLS-1$
    public static final String EXTRAS_TYPE = "type"; //$NON-NLS-1$

    @Inject Notifier notifier;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        notifier.triggerTaskNotification(
                intent.getLongExtra(ID_KEY, 0),
                intent.getIntExtra(EXTRAS_TYPE, (byte) 0));
    }
}
