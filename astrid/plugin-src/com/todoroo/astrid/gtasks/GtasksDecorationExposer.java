/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for GTasks indentation
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksDecorationExposer implements TaskDecorationExposer {

    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    public GtasksDecorationExposer() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public TaskDecoration expose(Filter activeFilter, Task task) {
        if(!gtasksPreferenceService.isLoggedIn())
            return null;

        if(!activeFilter.sqlQuery.contains(GtasksMetadata.METADATA_KEY))
            return null;

        return createDecoration(task);
    }

    private TaskDecoration createDecoration(Task task) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(task.getId());
        if(metadata == null)
            return null;

        int indentation = metadata.getValue(GtasksMetadata.INDENTATION);
        if(indentation <= 0)
            return null;

        RemoteViews decoration = new RemoteViews(ContextManager.getContext().getPackageName(),
                R.layout.gtasks_decoration);
        decoration.setInt(R.id.indent, "setWidth", indentation * 20); //$NON-NLS-1$
        return new TaskDecoration(decoration, TaskDecoration.POSITION_LEFT, 0);
    }

}
