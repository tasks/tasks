/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.util.ArrayList;
import java.util.Stack;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.sync.SyncMetadataService;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Service for working with GTasks metadata
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class GtasksMetadataService extends SyncMetadataService<GtasksTaskContainer> {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private GtasksListService gtasksListService;

    public GtasksMetadataService() {
        super(ContextManager.getContext());
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public GtasksTaskContainer createContainerFromLocalTask(Task task,
            ArrayList<Metadata> metadata) {
        return new GtasksTaskContainer(task, metadata);
    }

    @Override
    public Criterion getLocalMatchCriteria(GtasksTaskContainer remoteTask) {
        return GtasksMetadata.ID.eq(remoteTask.gtaskMetadata.getValue(GtasksMetadata.ID));
    }

    @Override
    public Criterion getMetadataCriteria() {
        return MetadataCriteria.withKey(getMetadataKey());
    }

    @Override
    public String getMetadataKey() {
        return GtasksMetadata.METADATA_KEY;
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return gtasksPreferenceService;
    }

    /**
     * Update order and parent fields for all tasks in the given list
     * @param listId
     */
    public void updateMetadataForList(String listId) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;
        Filter filter = GtasksFilterExposer.filterFromList(list);
        TodorooCursor<Task> cursor = PluginServices.getTaskService().fetchFiltered(filter.sqlQuery, null, Task.ID);
        try {
            int order = 0;
            int previousIndent = -1;
            Stack<Long> taskHierarchyStack = new Stack<Long>();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long taskId = cursor.getLong(0);
                Metadata metadata = getTaskMetadata(taskId);
                if(metadata == null)
                    continue;

                metadata.setValue(GtasksMetadata.ORDER, order++);
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                for(int i = indent; i <= previousIndent; i++)
                    taskHierarchyStack.pop();

                if(indent > 0) {
                    if(taskHierarchyStack.isEmpty())
                        metadata.setValue(GtasksMetadata.INDENT, 0);
                    else
                        metadata.setValue(GtasksMetadata.PARENT_TASK, taskHierarchyStack.peek());
                }

                PluginServices.getMetadataService().save(metadata);
                taskHierarchyStack.push(taskId);
            }
        } finally {
            cursor.close();
        }
    }

}
