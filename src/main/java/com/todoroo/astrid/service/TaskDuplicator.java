package com.todoroo.astrid.service;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.gtasks.GtasksMetadata;

import java.util.List;

import javax.inject.Inject;

public class TaskDuplicator {

    private final TaskService taskService;
    private final GCalHelper gcalHelper;
    private final MetadataDao metadataDao;

    @Inject
    public TaskDuplicator(TaskService taskService, GCalHelper gcalHelper, MetadataDao metadataDao) {
        this.taskService = taskService;
        this.gcalHelper = gcalHelper;
        this.metadataDao = metadataDao;
    }

    /**
     * Create an uncompleted copy of this task and edit it
     * @return cloned item id
     */
    public long duplicateTask(long itemId) {
        Task original = new Task();
        original.setId(itemId);
        Task clone = clone(original);
        clone.setCreationDate(DateUtilities.now());
        clone.setCompletionDate(0L);
        clone.setDeletionDate(0L);
        clone.setCalendarUri(""); //$NON-NLS-1$
        gcalHelper.createTaskEventIfEnabled(clone);

        taskService.save(clone);
        return clone.getId();
    }

    private Task clone(Task task) {
        Task newTask = taskService.fetchById(task.getId(), Task.PROPERTIES);
        if(newTask == null) {
            return new Task();
        }
        newTask.clearValue(Task.ID);
        newTask.clearValue(Task.UUID);

        List<Metadata> metadataList = metadataDao.toList(Query.select(Metadata.PROPERTIES).where(MetadataDao.MetadataCriteria.byTask(task.getId())));

        if (!metadataList.isEmpty()) {
            newTask.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskService.save(newTask);
            long newId = newTask.getId();
            for (Metadata metadata : metadataList) {
                if(!metadata.containsNonNullValue(Metadata.KEY)) {
                    continue;
                }

                if(GtasksMetadata.METADATA_KEY.equals(metadata.getKey())) {
                    metadata.setValue(GtasksMetadata.ID, ""); //$NON-NLS-1$
                }

                metadata.setTask(newId);
                metadata.clearValue(Metadata.ID);
                metadataDao.createNew(metadata);
            }
        }
        return newTask;
    }
}
