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
 * Context Menu actions for changing indent level of a task
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class GtasksIndentAction extends BroadcastReceiver {

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
        if(metadata == null) {
            metadata = GtasksMetadata.createEmptyMetadata(taskId);
        }

        if(metadata.getValue(GtasksMetadata.INDENT) + getDelta() < 0)
            return;

        String listId = metadata.getValue(GtasksMetadata.LIST_ID);
        gtasksTaskListUpdater.indent(listId, taskId, getDelta());
        gtasksTaskListUpdater.correctMetadataForList(listId);

        Flags.set(Flags.REFRESH);
    }

    public static class GtasksIncreaseIndentAction extends GtasksIndentAction {
        @Override
        public int getDelta() {
            return 1;
        }
    }

    public static class GtasksDecreaseIndentAction extends GtasksIndentAction {
        @Override
        public int getDelta() {
            return -1;
        }
    }

}
