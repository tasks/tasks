package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.utility.Flags;

/**
 * Context Menu actions for changing the order of a task
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class GtasksOrderAction extends BroadcastReceiver {

    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    abstract int getDelta();

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;
        Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
        if(metadata == null)
            return;

        metadata = gtasksMetadataService.getTaskMetadata(taskId);

        String listId = metadata.getValue(GtasksMetadata.LIST_ID);
        gtasksTaskListUpdater.moveUp(listId, taskId);
        gtasksTaskListUpdater.debugPrint(listId);

        Flags.set(Flags.REFRESH);
    }

    public static class GtasksMoveUpAction extends GtasksOrderAction {
        @Override
        public int getDelta() {
            return -1;
        }
    }

    public static class GtasksMoveDownAction extends GtasksOrderAction {
        @Override
        public int getDelta() {
            return 1;
        }
    }

}
