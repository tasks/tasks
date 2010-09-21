package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;

abstract public class GtasksIndentAction extends BroadcastReceiver {

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
            metadata = GtasksMetadata.createEmptyMetadata();

        int newIndent = Math.max(0, metadata.getValue(GtasksMetadata.INDENTATION) + getDelta());
        metadata.setValue(GtasksMetadata.INDENTATION, newIndent);
        PluginServices.getMetadataService().save(metadata);

        Toast.makeText(context, context.getString(R.string.gtasks_indent_toast, newIndent),
                Toast.LENGTH_SHORT).show();
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
