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

    public static void broadcast(Context context, Task task, ArrayList<String> values) {
        Intent intent = new Intent(context, PushReceiver.class);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK, task);
        intent.putStringArrayListExtra(AstridApiConstants.EXTRAS_VALUES, values);
        context.sendBroadcast(intent);
    }

    @Inject GoogleTaskPusher googleTaskPusher;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        googleTaskPusher.push(
                intent.getParcelableExtra(AstridApiConstants.EXTRAS_TASK),
                intent.getStringArrayListExtra(AstridApiConstants.EXTRAS_VALUES));
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
