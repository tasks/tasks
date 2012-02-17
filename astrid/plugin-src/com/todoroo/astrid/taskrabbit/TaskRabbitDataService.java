/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.taskrabbit;

import java.util.ArrayList;

import android.content.Context;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;

public final class TaskRabbitDataService {


    // --- singleton

    private static TaskRabbitDataService instance = null;

    public static synchronized TaskRabbitDataService getInstance() {
        if(instance == null)
            instance = new TaskRabbitDataService(ContextManager.getContext());
        return instance;
    }

    // --- instance variables

    protected final Context context;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataService metadataService;

    private TaskRabbitDataService(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- task and metadata methods

    /**
     * Saves a task and its metadata
     * @param task
     */
    public void saveTaskAndMetadata(TaskRabbitTaskContainer task) {
        taskDao.save(task.task);
        task.metadata.add(task.trTask);
        // note we don't include note metadata, since we only receive deltas
        metadataService.synchronizeMetadata(task.task.getId(), task.metadata,
                MetadataCriteria.withKey(TaskRabbitMetadata.METADATA_KEY));
    }

    /**
     * Reads a task and its metadata
     * @param task
     * @return
     */
    public TaskRabbitTaskContainer getContainerForTask(Task task) {
        // read tags, notes, etc
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        TodorooCursor<Metadata> metadataCursor = metadataService.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.and(MetadataCriteria.byTask(task.getId()),
                                MetadataCriteria.withKey(TaskRabbitMetadata.METADATA_KEY))));
        try {
            for(metadataCursor.moveToFirst(); !metadataCursor.isAfterLast(); metadataCursor.moveToNext()) {
                metadata.add(new Metadata(metadataCursor));
            }
        } finally {
            metadataCursor.close();
        }

        if (metadata.size() == 0) return new TaskRabbitTaskContainer(task);
        return new TaskRabbitTaskContainer(task, metadata.get(0));
    }

}
