package org.tasks.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

public class PushReceiver extends InjectingBroadcastReceiver {

    public static void broadcast(Context context, Task task, ContentValues values) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
