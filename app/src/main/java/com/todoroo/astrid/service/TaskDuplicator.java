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

import org.tasks.Broadcaster;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class TaskDuplicator {

    private final GCalHelper gcalHelper;
    private final MetadataDao metadataDao;
    private final TaskDao taskDao;
    private final Broadcaster broadcaster;

    @Inject
    public TaskDuplicator(GCalHelper gcalHelper, MetadataDao metadataDao, TaskDao taskDao,
                          Broadcaster broadcaster) {
        this.gcalHelper = gcalHelper;
        this.metadataDao = metadataDao;
        this.taskDao = taskDao;
        this.broadcaster = broadcaster;
    }

    public List<Task> duplicate(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            result.add(clone(taskDao.fetch(task.getId(), Task.PROPERTIES), true));
        }
        broadcaster.refresh();
        return result;
    }

    private Task clone(Task original, boolean suppressRefresh) {
        Task clone = new Task(original);
        clone.setCreationDate(DateUtilities.now());
        clone.setCompletionDate(0L);
        clone.setDeletionDate(0L);
        clone.setCalendarUri("");
        clone.clearValue(Task.ID);
        clone.clearValue(Task.UUID);

        List<Metadata> metadataList = metadataDao.byTask(original.getId());
        if (!metadataList.isEmpty()) {
            clone.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
        }
        if (suppressRefresh) {
            clone.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
        }

        taskDao.save(clone);

        for (Metadata oldMetadata : metadataList) {
            if(!oldMetadata.containsNonNullValue(Metadata.KEY)) {
                continue;
            }
            Timber.d("Cloning %s", oldMetadata);
            if(GtasksMetadata.METADATA_KEY.equals(oldMetadata.getKey())) {
                Metadata gtaskMetadata = GtasksMetadata.createEmptyMetadataWithoutList(clone.getId());
                gtaskMetadata.setValue(GtasksMetadata.LIST_ID, oldMetadata.getValue(GtasksMetadata.LIST_ID));
                metadataDao.createNew(gtaskMetadata);
            } else if (TaskToTagMetadata.KEY.equals(oldMetadata.getKey())) {
                Metadata metadata = new Metadata(oldMetadata);
                metadata.setValue(TaskToTagMetadata.TASK_UUID, clone.getUuid());
                metadata.setTask(clone.getId());
                metadata.clearValue(Metadata.ID);
                metadataDao.createNew(metadata);
            }
        }

        gcalHelper.createTaskEventIfEnabled(clone);

        return clone;
    }
}
