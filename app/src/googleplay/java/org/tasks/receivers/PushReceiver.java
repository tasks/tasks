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
        Intent intent = new Intent(context, PushReceiver.class);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK, task);
        intent.putExtra(AstridApiConstants.EXTRAS_VALUES, values);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        GoogleTaskPushReceiver.broadcast(
                context,
                intent.getParcelableExtra(AstridApiConstants.EXTRAS_TASK),
                intent.getParcelableExtra(AstridApiConstants.EXTRAS_VALUES));
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
