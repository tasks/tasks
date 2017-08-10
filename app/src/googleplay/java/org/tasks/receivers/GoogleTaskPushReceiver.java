package org.tasks.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;

import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class GoogleTaskPushReceiver extends InjectingBroadcastReceiver {

    public static void broadcast(Context context, Task task, ContentValues values) {
        Intent intent = new Intent(context, GoogleTaskPushReceiver.class);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK, task);
        intent.putExtra(AstridApiConstants.EXTRAS_VALUES, values);
        context.sendBroadcast(intent);
    }

    private static final Property<?>[] TASK_PROPERTIES = { Task.ID, Task.TITLE,
            Task.NOTES, Task.DUE_DATE, Task.COMPLETION_DATE, Task.DELETION_DATE };

    @Inject SyncAdapterHelper syncAdapterHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(!syncAdapterHelper.isEnabled()) {
            return;
        }

        Task model = intent.getParcelableExtra(AstridApiConstants.EXTRAS_TASK);
        ContentValues setValues = intent.getParcelableExtra(AstridApiConstants.EXTRAS_VALUES);
        if (model == null) {
            return;
        }
        if(model.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
            return;
        }
        if (checkValuesForProperties(setValues, TASK_PROPERTIES) || model.checkTransitory(SyncFlags.FORCE_SYNC)) {
            syncAdapterHelper.requestSynchronization();
        }
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }

    /**
     * Checks to see if any of the values changed are among the properties we sync
     * @return false if none of the properties we sync were changed, true otherwise
     */
    private boolean checkValuesForProperties(ContentValues values, Property<?>[] properties) {
        if (values == null) {
            return false;
        }
        for (Property<?> property : properties) {
            if (property != Task.ID && values.containsKey(property.name)) {
                return true;
            }
        }
        return false;
    }
}
