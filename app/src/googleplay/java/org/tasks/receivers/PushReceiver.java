package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

import java.util.ArrayList;

import javax.inject.Inject;

public class PushReceiver extends InjectingBroadcastReceiver {

    public static void broadcast(Context context, Task task, Task original) {
        Intent intent = new Intent(context, PushReceiver.class);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK, task);
        intent.putExtra(AstridApiConstants.EXTRAS_ORIGINAL, original);
        context.sendBroadcast(intent);
    }

    @Inject GoogleTaskPusher googleTaskPusher;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        googleTaskPusher.push(
                intent.getParcelableExtra(AstridApiConstants.EXTRAS_TASK),
                intent.getParcelableExtra(AstridApiConstants.EXTRAS_ORIGINAL));
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
