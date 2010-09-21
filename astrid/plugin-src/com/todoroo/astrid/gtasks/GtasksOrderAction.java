package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.utility.Flags;

/**
 * Context Menu actions for changing the order of a task
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class GtasksOrderAction extends BroadcastReceiver {

    @Autowired private GtasksMetadataService gtasksMetadataService;

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

        gtasksMetadataService.updateMetadataForList(metadata.getValue(GtasksMetadata.LIST_ID));

        metadata = gtasksMetadataService.getTaskMetadata(taskId);
        int oldOrder = metadata.getValue(GtasksMetadata.ORDER);
        int newOrder = Math.max(0, oldOrder + getDelta());
        if(newOrder == oldOrder)
            return;

        metadata.setValue(GtasksMetadata.ORDER, newOrder);
        TodorooCursor<Metadata> cursor = PluginServices.getMetadataService().query(Query.select(Metadata.ID, GtasksMetadata.INDENT).where(
                Criterion.and(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                        GtasksMetadata.LIST_ID.eq(metadata.getValue(GtasksMetadata.LIST_ID)),
                        GtasksMetadata.ORDER.eq(newOrder))));
        try {
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                Metadata otherTask = new Metadata(cursor);

                int myIndent = metadata.getValue(GtasksMetadata.INDENT);
                int otherIndent = otherTask.getValue(GtasksMetadata.INDENT);

                if(myIndent < otherIndent) {
                    // swap indents. TODO: what does google do
                    otherTask.setValue(GtasksMetadata.INDENT, myIndent);
                    metadata.setValue(GtasksMetadata.INDENT, otherIndent);
                }

                otherTask.setValue(GtasksMetadata.ORDER, oldOrder);
                PluginServices.getMetadataService().save(otherTask);
            }
        } finally {
            cursor.close();
        }

        PluginServices.getMetadataService().save(metadata);
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
