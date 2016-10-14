package com.todoroo.astrid.service;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class TaskDuplicator {

    private final GCalHelper gcalHelper;
    private final MetadataDao metadataDao;
    private final TaskDao taskDao;

    @Inject
    public TaskDuplicator(GCalHelper gcalHelper, MetadataDao metadataDao, TaskDao taskDao) {
        this.gcalHelper = gcalHelper;
        this.metadataDao = metadataDao;
        this.taskDao = taskDao;
    }

    /**
     * Create an uncompleted copy of this task and edit it
     * @return cloned item id
     */
    public long duplicateTask(long itemId) {
        Task original = taskDao.fetch(itemId, Task.PROPERTIES);
        Timber.d("Cloning %s", original);
        Task clone = new Task(original);
        clone.setCreationDate(DateUtilities.now());
        clone.setCompletionDate(0L);
        clone.setDeletionDate(0L);
        clone.setCalendarUri("");
        clone.clearValue(Task.ID);
        clone.clearValue(Task.UUID);

        List<Metadata> metadataList = metadataDao.byTask(itemId);
        if (!metadataList.isEmpty()) {
            clone.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
        }

        taskDao.save(clone);

        for (Metadata oldMetadata : metadataList) {
            if(!oldMetadata.containsNonNullValue(Metadata.KEY)) {
                continue;
            }
            Timber.d("Cloning %s", oldMetadata);
            Metadata metadata = new Metadata(oldMetadata);
            if(GtasksMetadata.METADATA_KEY.equals(metadata.getKey())) {
                metadata.setValue(GtasksMetadata.ID, ""); //$NON-NLS-1$
            } else if (TaskToTagMetadata.KEY.equals(metadata.getKey())) {
                metadata.setValue(TaskToTagMetadata.TASK_UUID, clone.getUuid());
            }
            metadata.setTask(clone.getId());
            metadata.clearValue(Metadata.ID);
            metadataDao.createNew(metadata);
        }

        gcalHelper.createTaskEventIfEnabled(clone);

        return clone.getId();
    }
}
