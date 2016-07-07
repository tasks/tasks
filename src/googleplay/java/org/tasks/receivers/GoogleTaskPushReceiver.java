package org.tasks.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.service.TaskService;

import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

import java.io.IOException;

import javax.inject.Inject;

public class GoogleTaskPushReceiver extends InjectingBroadcastReceiver {

    private static final Property<?>[] TASK_PROPERTIES = { Task.ID, Task.TITLE,
            Task.NOTES, Task.DUE_DATE, Task.COMPLETION_DATE, Task.DELETION_DATE };

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksSyncService gtasksSyncService;
    @Inject TaskDao taskDao;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(!gtasksPreferenceService.isLoggedIn()) {
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
        if (gtasksPreferenceService.isOngoing() && !model.checkTransitory(TaskService.TRANS_REPEAT_COMPLETE)) { //Don't try and sync changes that occur during a normal sync
            return;
        }
        if (checkValuesForProperties(setValues, TASK_PROPERTIES) || model.checkTransitory(SyncFlags.FORCE_SYNC)) {
            Task toPush = taskDao.fetch(model.getId(), TASK_PROPERTIES);
            gtasksSyncService.enqueue(new TaskPushOp(toPush));
        }
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }

    private class TaskPushOp implements GtasksSyncService.SyncOnSaveOperation {
        protected Task model;
        protected long creationDate = DateUtilities.now();

        public TaskPushOp(Task model) {
            this.model = model;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            if(DateUtilities.now() - creationDate < 1000) {
                AndroidUtilities.sleepDeep(1000 - (DateUtilities.now() - creationDate));
            }
            gtasksSyncService.pushTaskOnSave(model, model.getMergedValues(), invoker);
        }
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
